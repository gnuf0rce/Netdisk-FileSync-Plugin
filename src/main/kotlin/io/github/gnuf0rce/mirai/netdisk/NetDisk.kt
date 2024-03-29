package io.github.gnuf0rce.mirai.netdisk

import io.github.gnuf0rce.mirai.netdisk.data.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import net.mamoe.mirai.console.permission.*
import net.mamoe.mirai.console.permission.PermissionService.Companion.testPermission
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.contact.file.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.utils.*
import xyz.cssxsh.baidu.disk.*
import xyz.cssxsh.baidu.disk.data.*
import xyz.cssxsh.baidu.oauth.*
import xyz.cssxsh.baidu.oauth.exception.*
import java.time.*
import kotlin.coroutines.*

public object NetDisk : BaiduNetDiskClient(config = NetdiskOauthConfig), ListenerHost, CoroutineScope {

    override val coroutineContext: CoroutineContext =
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, cause ->
            logger.warning({ "Exception in NetDisk" }, cause)
        }

    public val permission: Permission by lazy {
        with(PermissionService.INSTANCE) {
            try {
                val id = NetDiskFileSyncPlugin.permissionId("sync")
                val parent = NetDiskFileSyncPlugin.parentPermission
                register(id, "百度云文件同步", parent)
            } catch (_: Exception) {
                rootPermission
            }
        }
    }

    @JvmStatic
    public val SHORT_URL_REGEX: Regex = """(?:surl=|s/1)([A-z0-9=_-]+)\s*([A-z0-9]{4})?""".toRegex()

    @JvmStatic
    public val STAND_CODE_REGEX: Regex = """[A-z0-9]{32}#([A-z0-9]{32}#)?\d+#\S+""".toRegex()

    @JvmStatic
    public val BD_LINK_REGEX: Regex = """bdlink=(\S+)""".toRegex()

    @PublishedApi
    internal val logger: MiraiLogger by lazy {
        try {
            NetDiskFileSyncPlugin.logger
        } catch (_: Exception) {
            MiraiLogger.Factory.create(this::class, "netdisk")
        }
    }

    private val downloader: HttpClient = HttpClient(OkHttp) {
        ContentEncoding()
        BrowserUserAgent()
        // FIXME: MalformedInputException
        expectSuccess = NetdiskUploadConfig.https.not()
        HttpResponseValidator {
            validateResponse { response ->
                if (response.headers[HttpHeaders.ContentType] == "text/octet") {
                    val bytes = response.body<ByteArray>()
                    throw ClientRequestException(response, bytes.joinToString("") { "\\x%02x".format(it) })
                }
            }
        }
    }

    override val apiIgnore: suspend (Throwable) -> Boolean = BaiduNetDiskPool.defaultApiIgnore

    override val status: BaiduAuthStatus get() = NetdiskAuthStatus

    override suspend fun refreshToken(): String {
        return try {
            super.refreshToken()
        } catch (cause: NotTokenException) {
            logger.warning { "缺少 RefreshToken, 请使用 '/baidu oauth' 绑定百度账号" }
            throw cause
        }
    }

    @EventHandler
    public fun MessageEvent.upload() {
        val contact = subject as? Group ?: return
        val content = message.findIsInstance<FileMessage>() ?: return
        if (permission.testPermission(contact.permitteeId).not() &&
            permission.testPermission(sender.permitteeId).not()
        ) return
        val netdisk = contact.netdisk

        launch {
            logger.info { "发现文件消息 $content 开始上传" }
            val file = withTimeout(10_000) {
                content.toAbsoluteFile(contact) ?: throw NoSuchElementException("${content.name} in $contact")
            }

            val rapid = try {
                netdisk.uploadAbsoluteFile(file)
            } catch (cause: Exception) {
                logger.warning({ "上传失败 $file" }, cause)
                if (NetdiskUploadConfig.reply) {
                    subject.sendMessage(message.quote() + "文件 ${file.name} 上传失败, ${cause.message}")
                }
                return@launch
            }
            val code = rapid.format()
            logger.info { "上传成功 $file" }
            launch {
                NetDiskFileSyncRecorder.record(file = file, rapid = rapid)
            }
            if (NetdiskUploadConfig.reply) {
                subject.sendMessage(message.quote() + "文件 ${file.name} 上传成功, 秒传码:\n$code")
            }
        }
    }

    @EventHandler
    public fun MessageEvent.save() {
        val plain = message.findIsInstance<PlainText>() ?: return
        if (permission.testPermission(sender.permitteeId).not()) return
        val netdisk = sender.netdisk

        launch {
            val paths = mutableListOf<String>()
            SHORT_URL_REGEX.findAll(plain.content).forEach { match ->
                logger.info { "发现分享链接 ${match.value} 开始转存" }
                val (surl, password) = match.destructured
                val path = netdisk.saveShareLink(surl = surl, password = password)
                paths.add(path)

                launch {
                    NetDiskFileSyncRecorder.record(source = source, surl = surl, password = password)
                }
            }
            if (paths.isNotEmpty() && NetdiskUploadConfig.reply) {
                val info = netdisk.user()
                val name = info.netdiskName.ifEmpty { info.baiduName }
                subject.sendMessage(buildMessageChain {
                    append(message.quote())
                    appendLine("分享转存到 $name 成功")
                    paths.forEach { path ->
                        appendLine(path)
                    }
                })
            }
        }
        launch {
            val paths = mutableListOf<String>()
            STAND_CODE_REGEX.findAll(plain.content).forEach { match ->
                logger.info { "发现秒传码 ${match.value} 开始保存" }
                val upload = RapidUploadInfo.parse(code = match.value)
                val path = netdisk.saveRapidUpload(upload = upload)
                paths.add(path)

                launch {
                    NetDiskFileSyncRecorder.record(source = source, rapid = upload)
                }
            }
            if (paths.isNotEmpty() && NetdiskUploadConfig.reply) {
                val info = netdisk.user()
                val name = info.netdiskName.ifEmpty { info.baiduName }
                subject.sendMessage(buildMessageChain {
                    append(message.quote())
                    appendLine("秒传码保存到 $name 成功")
                    paths.forEach { path ->
                        appendLine(path)
                    }
                })
            }
        }
        launch {
            val paths = mutableListOf<String>()
            BD_LINK_REGEX.findAll(plain.content).forEach { match ->
                logger.info { "发现秒传链接 ${match.value} 开始保存" }
                val (base64) = match.destructured
                STAND_CODE_REGEX.findAll(base64.decodeBase64String()).forEach { m ->
                    val upload = RapidUploadInfo.parse(code = m.value)
                    val path = netdisk.saveRapidUpload(upload = upload)
                    paths.add(path)

                    launch {
                        NetDiskFileSyncRecorder.record(source = source, rapid = upload)
                    }
                }
            }
            if (paths.isNotEmpty() && NetdiskUploadConfig.reply) {
                val info = netdisk.user()
                val name = info.netdiskName.ifEmpty { info.baiduName }
                subject.sendMessage(buildMessageChain {
                    append(message.quote())
                    appendLine("秒传链接保存到 $name 成功")
                    paths.forEach { path ->
                        appendLine(path)
                    }
                })
            }
        }
    }

    @PublishedApi
    internal suspend fun BaiduNetDiskClient.uploadAbsoluteFile(file: AbsoluteFile): RapidUploadInfo {
        val url = requireNotNull(file.getUrl()) { "远程文件 URL 获取失败" }
        @Suppress("INVISIBLE_MEMBER")
        val rapid = with(file) {
            val content = file.md5.toHexString()
            val slice = if (size <= SLICE_SIZE) {
                content
            } else {
                val slice = download(urlString = url, range = 0L until SLICE_SIZE.toLong()).body<ByteArray>()
                slice.md5().toHexString()
            }
            RapidUploadInfo(
                content = content,
                slice = slice,
                length = size,
                path = "${LocalDate.now()}/${contact.id}${absolutePath}"
            )
        }

        // 用群号做根目录
        mkdir(path = rapid.path.substringBeforeLast('/'))
        logger.info { "upload ${rapid.format()}" }

        // 尝试秒传
        try {
            rapid(upload = rapid)
            return rapid
        } catch (cause: ClientRequestException) {
            logger.info { "文件 ${file.name} 秒传失败, 进入文件上传, ${cause.response.bodyAsText()}" }
        } catch (exception: Exception) {
            logger.warning({ "文件 ${file.name} 秒传失败, 进入文件上传" }, exception)
        }

        val user = user()
        check(file.size <= user.vip.updateLimit) { "${file.contact}-${file.name} 超过了文件上传极限" }
        val limit = user.vip.superLimit.toLong()

        if (file.size < limit) {
            try {
                val bytes = download(urlString = url).body<ByteArray>()
                pcs.upload(path = rapid.path, ondup = OnDupType.NEW_COPY, size = bytes.size.toLong()) {
                    writeFully(bytes)
                }
                return rapid
            } catch (cause: ClientRequestException) {
                logger.info { "文件 ${file.name} 单文件上传失败, 进入文件上传, ${cause.message}" }
            } catch (exception: Exception) {
                logger.warning({ "文件 ${file.name} 单文件上传失败, 进入文件上传" }, exception)
            }
        }


        val prepare = rest.prepare(upload = rapid, blocks = LAZY_BLOCKS, ondup = OnDupType.NEW_COPY)
        if (prepare.type == PrepareReturnType.EXIST) return rapid
        val uploadId = requireNotNull(prepare.uploadId) { prepare }

        val blocks = download(urlString = url).execute { response ->
            val channel = response.bodyAsChannel()
            val capacity = file.size.minus(1).div(limit).plus(1).toInt()
            List(capacity) { index ->
                val packet = channel.readRemaining(limit)
                supervisorScope {
                    async {
                        val size = packet.remaining.toInt()
                        val temp = packet.use {
                            pcs.temp(path = rapid.path, id = uploadId, index = index, size = size) {
                                writePacket(packet)
                            }
                        }

                        temp.md5
                    }
                }
            }
        }.awaitAll()

        val merge = MergeFileInfo(
            blocks = blocks,
            uploadId = uploadId,
            size = file.size,
            path = rapid.path
        )

        rest.create(merge = merge, ondup = OnDupType.NEW_COPY)

        return rapid
    }

    @PublishedApi
    internal val Contact.netdisk: BaiduNetDiskClient by BaiduNetDiskPool

    @PublishedApi
    internal suspend fun BaiduNetDiskClient.saveShareLink(surl: String, password: String): String {
        val key = if (password.isNotEmpty()) {
            val verify = rest.verify(surl = surl, password = password)
            require(verify.errorNo == 0) { verify.errorMessage.ifEmpty { "$surl - $password" } }
            verify.key
        } else {
            ""
        }

        val root = rest.view(surl = surl, key = key)
        val dir = rest.mkdir(path = "${LocalDate.now()}/${root.uk}_${root.shareId}", ondup = OnDupType.NEW_COPY)

        val info = TransferFileInfo(
            shareId = root.shareId,
            from = root.uk,
            key = key,
            files = emptyList()
        )

        val limit = user().vip.transferLimit

        for (list in root.list.chunked(limit)) {
            val part = info.copy(files = list.map { it.id })

            rest.transfer(info = part, path = dir.path, ondup = OnDupType.NEW_COPY)

            delay(list.size.toLong())
        }

        return dir.path
    }

    @PublishedApi
    internal suspend fun BaiduNetDiskClient.saveRapidUpload(upload: RapidUploadInfo): String {
        val path = "${LocalDate.now()}/${upload.path}"

        val dir = mkdir(path = path.substringBeforeLast('/'))
        rapid(upload = upload.copy(path = path), ondup = OnDupType.NEW_COPY)

        return dir.path.ifEmpty { path }
    }

    private suspend fun download(urlString: String, range: LongRange? = null): HttpStatement {
        val fragment = range?.run { "bytes=${start}-${endInclusive}" }
        logger.verbose { "download $urlString#$fragment" }
        return downloader.prepareGet(urlString) {
            url {
                if (NetdiskUploadConfig.https) {
                    protocol = URLProtocol.HTTPS
                    host = "gzc-download.ftn.qq.com"
                }
            }
            header(HttpHeaders.Range, fragment)
        }
    }
}