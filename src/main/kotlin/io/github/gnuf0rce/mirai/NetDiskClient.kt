package io.github.gnuf0rce.mirai

import io.github.gnuf0rce.mirai.data.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.console.util.CoroutineScopeUtils.childScope
import net.mamoe.mirai.contact.FileSupported
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.data.FileMessage
import net.mamoe.mirai.utils.*
import okio.ByteString.Companion.toByteString
import xyz.cssxsh.baidu.*
import xyz.cssxsh.baidu.disk.*
import java.time.OffsetDateTime

@ConsoleExperimentalApi
object NetDiskClient : BaiduNetDiskClient(config = NetdiskOauthConfig),
    CoroutineScope by NetdiskFileSyncPlugin.childScope("NetDiskClient") {

    private val logger by NetdiskFileSyncPlugin::logger

    override val accessToken: String get() {
        return runCatching {
            super.accessToken
        }.onFailure {
            logger.warning {
                "认证失效, 请访问以下网址，使用/baidu-oauth 指令进行认证"
            }
        }.getOrThrow()
    }

    fun reload() = synchronized(this) {
        expires = runCatching { OffsetDateTime.parse(NetdiskUserData.expires) }.getOrElse { OffsetDateTime.now() }
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
            val excludes by FileSyncConfig::excludes
            has<FileMessage> {
                if (subject.id in excludes) return@has

                val file = it.toRemoteFile(subject as FileSupported) ?: return@has
                runCatching {
                    uploadRemoteFile(file)
                }.onSuccess {
                    subject.sendMessage("文件${file.name}上传成功, 秒传码${file.getRapidUploadInfo().format()}")
                }
            }
        }
    }

    private suspend fun uploadRemoteFile(file: RemoteFile): NetDiskFileInfo {
        val info = requireNotNull(file.getDownloadInfo())
        val contact by file::contact

        val path = "${contact}/${file.path}/${file.name}"
        val rapid = file.getRapidUploadInfo()

        runCatching {
            rapidUploadFile(rapid.copy(path = path))
        }.onSuccess {
            return@uploadRemoteFile it
        }

        val user = getUserInfo()
        check(file.length() <= user.vip.updateLimit) {
            "${file.contact}-${file.name} 超过了文件上传极限"
        }
        val block = user.vip.superLimit.toLong()

        val pre = preCreate(
            path = path,
            size = file.length(),
            isDir = false,
            blocks = emptyList(),
        )

        if (pre.type == CreateReturnType.EXIST) {
            return requireNotNull(pre.info) { pre.toString() }
        }

        val blocks = (0 .. file.length() step block).asFlow().map { first ->
            val last = minOf(first + block, file.length()) - 1
            download(info.url, first..last)
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

        return createFile(
            path = path,
            size = file.length(),
            isDir = false,
            blocks = blocks,
            uploadId = pre.uploadId
        )
    }

    private suspend fun download(url: String, range: LongRange? = null): ByteArray {
        return useHttpClient {
            it.get(url) {
                header(HttpHeaders.Range, range?.run { "${start}-${endInclusive}" })
            }
        }
    }

    private suspend fun RemoteFile.slice(): ByteArray {
        val info = requireNotNull(getDownloadInfo())
        return download(info.url, 0..SLICE_SIZE.toLong().coerceAtMost(length()))
    }

    private suspend fun RemoteFile.getRapidUploadInfo(): RapidUploadInfo {
        val info = requireNotNull(getDownloadInfo())

        return RapidUploadInfo(
            content = info.md5.toByteString().hex(),
            slice = slice().toByteString().hex(),
            length = length(),
            path = info.filename
        )
    }
}