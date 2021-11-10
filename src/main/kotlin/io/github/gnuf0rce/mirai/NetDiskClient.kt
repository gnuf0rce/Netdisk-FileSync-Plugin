package io.github.gnuf0rce.mirai

import io.github.gnuf0rce.mirai.data.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import net.mamoe.mirai.console.command.CommandSender.Companion.toCommandSender
import net.mamoe.mirai.console.permission.*
import net.mamoe.mirai.console.permission.PermissionService.Companion.testPermission
import net.mamoe.mirai.console.util.CoroutineScopeUtils.childScope
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.contact.file.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.utils.*
import okio.ByteString.Companion.toByteString
import xyz.cssxsh.baidu.*
import xyz.cssxsh.baidu.disk.*
import java.time.*

object NetDiskClient : BaiduNetDiskClient(config = NetdiskOauthConfig),
    CoroutineScope by NetdiskFileSyncPlugin.childScope("NetDiskClient") {

    val permission: Permission by lazy {
        val id = NetdiskFileSyncPlugin.permissionId("sync")
        val parent = NetdiskFileSyncPlugin.parentPermission
        PermissionService.INSTANCE.register(id, "百度云文件同步", parent)
    }

    private val logger get() = NetdiskFileSyncPlugin.logger

    override val accessToken: String
        get() {
            return try {
                super.accessToken
            } catch (e: Throwable) {
                logger.warning({ "认证失效, 请访问以下网址，使用/baidu-oauth 指令进行认证" }, e)
                throw e
            }
        }

    fun reload() = synchronized(this) {
        expires = try {
            OffsetDateTime.parse(NetdiskUserData.expires)
        } catch (e: Throwable) {
            OffsetDateTime.now()
        }
        accessTokenValue = NetdiskUserData.accessToken
        refreshTokenValue = NetdiskUserData.refreshToken
    }

    fun save() {
        NetdiskUserData.expires = expires.toString()
        NetdiskUserData.accessToken = accessTokenValue.orEmpty()
        NetdiskUserData.refreshToken = refreshTokenValue.orEmpty()
    }

    fun subscribe() {
        globalEventChannel().subscribeMessages {
            always {
                val contact = subject as? FileSupported ?: return@always
                val content = message.firstIsInstanceOrNull<FileMessage>() ?: return@always
                if (permission.testPermission(toCommandSender()).not()) return@always

                logger.info { "发现文件消息 ${content}，开始上传" }
                lateinit var file: AbsoluteFile
                runCatching {
                    file = withTimeout(10_000) {
                        requireNotNull(content.toAbsoluteFile(contact)) { "文件获取失败" }
                    }

                    uploadAbsoluteFile(file)
                }.onSuccess { rapid ->
                    logger.info { "上传成功$file" }
                    subject.sendMessage(message.quote() + "文件${file.name}上传成功, 秒传码${rapid.format()}")
                }.onFailure {
                    logger.warning { "上传失败$file, $it" }
                    subject.sendMessage(message.quote() + "文件${file.name}上传失败, $it")
                }
            }
        }
    }

    private suspend fun uploadAbsoluteFile(file: AbsoluteFile): RapidUploadInfo {

        val path = "${file.contact.id}${file.absolutePath}"
        val rapid = file.rapid()

        logger.info { "upload $file to $path" }

        runCatching {
            rapidUploadFile(rapid.copy(path = path))
        }.onSuccess {
            return@uploadAbsoluteFile rapid
        }

        val user = getUserInfo()
        check(file.size <= user.vip.updateLimit) {
            "${file.contact}-${file.name} 超过了文件上传极限"
        }
        val block = user.vip.superLimit.toLong()

        val pre = preCreate(
            path = path,
            size = file.size,
            isDir = false,
            blocks = emptyList(),
        )

        if (pre.type == CreateReturnType.EXIST) {
            return rapid
        }

        val url = requireNotNull(file.getUrl()) { "文件不存在" }
        val blocks = (0..file.size step block).asFlow().map { first ->
            val last = minOf(first + block, file.size) - 1
            download(url, first..last)
        }.withIndex().buffer().map { (index, bytes) ->
            superFile(
                path = path,
                uploadId = pre.uploadId,
                index = index,
                data = bytes,
                size = bytes.size
            )
            bytes.toByteString().md5().hex()
        }.toList()

        createFile(
            path = path,
            size = file.size,
            isDir = false,
            blocks = blocks,
            uploadId = pre.uploadId
        )

        return rapid
    }

    private suspend fun download(urlString: String, range: LongRange? = null): ByteArray {
        val url = Url(urlString).copy(protocol = URLProtocol.HTTPS, host = "gzc-download.ftn.qq.com")
        logger.info { "$url" }
        return useHttpClient { client ->
            client.config {
                BrowserUserAgent()
            }.get(url) {
                header(HttpHeaders.Range, range?.run { "${start}-${endInclusive}" })
            }
        }
    }

    private suspend fun AbsoluteFile.rapid(): RapidUploadInfo {
        return RapidUploadInfo(
            content = md5.toByteString().hex(),
            slice = slice().toByteString().hex(),
            length = size,
            path = absolutePath
        )
    }

    private suspend fun AbsoluteFile.slice(): ByteArray {
        val url = requireNotNull(getUrl()) { "文件不存在" }
        logger.info { url }
        return download(
            urlString = url,
            range = 0..SLICE_SIZE.toLong().coerceAtMost(size)
        )
    }
}