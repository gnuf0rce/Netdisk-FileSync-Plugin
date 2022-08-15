package io.github.gnuf0rce.mirai.netdisk

import io.github.gnuf0rce.mirai.netdisk.data.*
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.errors.*
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
import java.util.*
import kotlin.collections.*
import kotlin.coroutines.*
import kotlin.properties.*
import kotlin.reflect.*

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
            } catch (_: Throwable) {
                rootPermission
            }
        }
    }

    private val logger: MiraiLogger by lazy {
        try {
            NetDiskFileSyncPlugin.logger
        } catch (_: Throwable) {
            MiraiLogger.Factory.create(this::class, "netdisk")
        }
    }

    private var KClass<out Throwable>.count: Int by object : ReadWriteProperty<KClass<*>, Int> {
        private val history: MutableMap<KClass<*>, Int> = WeakHashMap()

        override fun getValue(thisRef: KClass<*>, property: KProperty<*>): Int {
            return history[thisRef] ?: 0
        }

        override fun setValue(thisRef: KClass<*>, property: KProperty<*>, value: Int) {
            history[thisRef] = value
        }
    }

    override val apiIgnore: suspend (Throwable) -> Boolean = { throwable ->
        when (throwable) {
            is MalformedInputException -> false
            is IOException -> {
                val count = ++throwable::class.count
                if (count > 10) {
                    throwable::class.count = 0
                    false
                } else {
                    logger.warning { "NetDiskClient Ignore: $throwable" }
                    true
                }
            }

            else -> false
        }
    }

    override val status: BaiduAuthStatus get() = NetdiskUserData

    override suspend fun refreshToken(): String {
        return try {
            super.refreshToken()
        } catch (cause: NotTokenException) {
            logger.warning { "缺少 RefreshToken, 请使用 /baidu-oauth 绑定百度账号" }
            throw cause
        }
    }

    @EventHandler
    public fun MessageEvent.handle() {
        val contact = subject as? Group ?: return
        val content = message.findIsInstance<FileMessage>() ?: return
        if (permission.testPermission(contact.permitteeId).not() &&
            permission.testPermission(sender.permitteeId).not()
        ) return

        launch {
            logger.info { "发现文件消息 ${content}，开始上传" }
            val file = withTimeout(10_000) {
                content.toAbsoluteFile(contact) ?: throw NoSuchElementException("${content.name} in $contact")
            }

            val rapid = try {
                uploadAbsoluteFile(file)
            } catch (cause: Throwable) {
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

    private suspend fun uploadAbsoluteFile(file: AbsoluteFile): RapidUploadInfo {
        val url = requireNotNull(file.getUrl()) { "远程文件 URL 获取失败" }
        val rapid = with(file) {
            val content = file.md5.toUHexString("").lowercase()
            val slice = if (size <= SLICE_SIZE) {
                content
            } else {
                val slice = download(urlString = url, range = 0L until SLICE_SIZE.toLong()).body<ByteArray>()
                slice.md5().toUHexString("").lowercase()
            }
            RapidUploadInfo(
                content = content,
                slice = slice,
                length = size,
                path = "${contact.id}${absolutePath}"
            )
        }

        // 用群号做根目录
        mkdir(path = "${file.contact.id}")
        logger.info { "upload ${rapid.format()}" }

        // 尝试秒传
        try {
            rapid(upload = rapid)
            return rapid
        } catch (throwable: IllegalArgumentException) {
            logger.info { "文件 ${file.name} 秒传失败, 进入文件上传, ${throwable.message}" }
        } catch (exception: Throwable) {
            logger.warning({ "文件 ${file.name} 秒传失败, 进入文件上传" }, exception)
        }

        val user = rest.user()
        check(file.size <= user.vip.updateLimit) { "${file.contact}-${file.name} 超过了文件上传极限" }
        val limit = user.vip.superLimit.toLong()

        if (file.size < limit) {
            try {
                val bytes = download(urlString = url).body<ByteArray>()
                pcs.upload(path = rapid.path, ondup = OnDupType.NEW_COPY, size = bytes.size.toLong()) {
                    writeFully(bytes)
                }
                return rapid
            } catch (throwable: ClientRequestException) {
                logger.info { "文件 ${file.name} 单文件上传失败, 进入文件上传, ${throwable.message}" }
            } catch (exception: Throwable) {
                logger.info({ "文件 ${file.name} 快速存入失败, 进入文件上传" }, exception)
            }
        }


        val prepare = rest.prepare(upload = rapid, blocks = LAZY_BLOCKS, ondup = OnDupType.NEW_COPY)
        if (prepare.type == PrepareReturnType.EXIST) {
            return rapid
        }
        val uploadId = requireNotNull(prepare.uploadId) { prepare }

        val blocks = download(urlString = url).execute { response ->
            val channel = response.bodyAsChannel()
            val capacity = (file.size / limit + 1).toInt()
            List(capacity) { index ->
                val packet = channel.readRemaining(limit)
                supervisorScope {
                    async {
                        val size = packet.remaining.toInt()
                        val temp = pcs.temp(path = rapid.path, id = uploadId, index = index, size = size) {
                            writePacket(packet)
                        }
                        packet.close()

                        temp.md5
                    }
                }
            }
        }.awaitAll()

        val merge = MergeFileInfo(
            blocks = blocks.toMutableList(),
            uploadId = uploadId,
            size = file.size,
            path = rapid.path
        )

        rest.create(merge = merge, ondup = OnDupType.NEW_COPY)

        return rapid
    }

    private val downloader: HttpClient = HttpClient(OkHttp) {
        ContentEncoding()
        BrowserUserAgent()
        // FIXME: MalformedInputException
        expectSuccess = NetdiskUploadConfig.https.not()
        HttpResponseValidator {
            validateResponse { response ->
                if (response.headers[HttpHeaders.ContentType] == "text/octet") {
                    val bytes = response.readBytes()
                    throw ClientRequestException(response, bytes.joinToString("") { "\\x%02x".format(it) })
                }
            }
        }
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