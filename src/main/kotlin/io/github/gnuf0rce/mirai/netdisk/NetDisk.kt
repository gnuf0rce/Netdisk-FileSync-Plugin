package io.github.gnuf0rce.mirai.netdisk

import io.github.gnuf0rce.mirai.netdisk.data.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.errors.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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
import kotlin.coroutines.*
import kotlin.properties.*
import kotlin.reflect.*

public object NetDisk : BaiduNetDiskClient(config = NetdiskOauthConfig), ListenerHost, CoroutineScope {

    override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.IO

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
    public suspend fun MessageEvent.handle() {
        val contact = subject as? Group ?: return
        val content = message.firstIsInstanceOrNull<FileMessage>() ?: return
        if (permission.testPermission(contact.permitteeId).not()) return

        logger.info { "发现文件消息 ${content}，开始上传" }
        lateinit var file: AbsoluteFile
        runCatching {
            file = withTimeout(10_000) {
                requireNotNull(content.toAbsoluteFile(contact)) { "文件获取失败" }
            }

            uploadAbsoluteFile(file)
        }.onSuccess { rapid ->
            val code = rapid.format()
            logger.info { "上传成功 $file" }
            NetdiskSyncHistory.records.add(code)
            if (NetdiskUploadConfig.reply) {
                subject.sendMessage(message.quote() + "文件 ${file.name} 上传成功, 秒传码:\n$code")
            }
        }.onFailure {
            logger.warning({ "上传失败 $file" }, it)
            if (NetdiskUploadConfig.reply) {
                subject.sendMessage(message.quote() + "文件 ${file.name} 上传失败, $it")
            }
        }
    }

    public fun cancel() {
        coroutineContext.cancelChildren()
    }

    public suspend fun uploadAbsoluteFile(file: AbsoluteFile): RapidUploadInfo {

        val path = "${file.contact.id}${file.absolutePath}"
        val rapid = file.rapid().copy(path = path)

        val mkdir = coroutineScope {
            async {
                createDir(path = "${file.contact.id}")
            }
        }

        logger.info { "upload ${rapid.format()} to $path" }

        try {
            rapidUploadFile(info = rapid)
            return rapid
        } catch (throwable: IllegalArgumentException) {
            logger.info { "文件 ${file.name} 快速存入失败, 进入文件上传, $throwable" }
        } catch (exception: Throwable) {
            logger.info({ "文件 ${file.name} 快速存入失败, 进入文件上传" }, exception)
        }

        val user = getUserInfo()
        check(file.size <= user.vip.updateLimit) { "${file.contact}-${file.name} 超过了文件上传极限" }
        val limit = user.vip.superLimit.toLong()

        val url = requireNotNull(file.getUrl()) { "文件不存在" }

        if (file.size < limit) {
            try {
                uploadSingleFile(path = path, bytes = download(urlString = url), size = file.size.toInt())
                return rapid
            } catch (throwable: ClientRequestException) {
                logger.info { "文件 ${file.name} 单文件上传失败, 进入文件上传, $throwable" }
            } catch (exception: Throwable) {
                logger.info({ "文件 ${file.name} 快速存入失败, 进入文件上传" }, exception)
            }
        }

        val pre = preCreate(
            path = path,
            size = file.size,
            isDir = false,
            blocks = emptyList(),
            rename = RenameType.PATH
        )

        if (pre.type == CreateReturnType.EXIST) {
            return rapid
        } else {
            check(pre.uploadId.isNotEmpty()) { pre }
        }

        val blocks = (0 until file.size step limit).asFlow().map { offset ->
            download(urlString = url, range = offset until minOf(offset + limit, file.size))
        }.withIndex().buffer().onStart {
            mkdir.await()
        }.map { (index, bytes) ->
            superFile(path = path, uploadId = pre.uploadId, index = index, data = bytes, size = bytes.size)
            bytes.md5().toUHexString("").lowercase()
        }.toList()

        createFile(
            path = path,
            size = file.size,
            isDir = false,
            blocks = blocks,
            uploadId = pre.uploadId,
            rename = RenameType.PATH
        )

        return rapid
    }

    private suspend fun download(urlString: String, range: LongRange? = null): ByteArray {
        val fragment = range?.run { "bytes=${start}-${endInclusive}" }
        logger.info { "download $urlString#$fragment" }
        return useHttpClient { client ->
            client.config {
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
            }.get {
                url {
                    takeFrom(urlString)
                    if (NetdiskUploadConfig.https) {
                        protocol = URLProtocol.HTTPS
                        host = "gzc-download.ftn.qq.com"
                    }
                }
                header(HttpHeaders.Range, fragment)
            }.body()
        }
    }

    private suspend fun AbsoluteFile.rapid(): RapidUploadInfo {
        return RapidUploadInfo(
            content = md5(),
            slice = slice(),
            length = size,
            path = absolutePath
        )
    }

    private fun AbsoluteFile.md5(): String {
        return md5.toUHexString("")
    }

    private suspend fun AbsoluteFile.slice(): String {
        return if (size <= SLICE_SIZE) {
            md5()
        } else {
            val url = requireNotNull(getUrl()) { "文件不存在" }
            val slice = download(urlString = url, range = 0L until SLICE_SIZE.toLong())
            slice.md5().toUHexString("")
        }
    }
}