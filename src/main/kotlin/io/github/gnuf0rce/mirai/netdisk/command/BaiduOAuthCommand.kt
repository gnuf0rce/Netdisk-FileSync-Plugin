package io.github.gnuf0rce.mirai.netdisk.command

import io.github.gnuf0rce.mirai.netdisk.*
import io.github.gnuf0rce.mirai.netdisk.data.*
import net.mamoe.mirai.console.*
import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.console.util.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.*
import net.mamoe.mirai.utils.*

@PublishedApi
internal object BaiduOAuthCommand : CompositeCommand(
    owner = NetDiskFileSyncPlugin,
    primaryName = "baidu",
    description = "文件同步 百度账号认证指令"
) {
    private val logger by NetDiskFileSyncPlugin::logger

    private suspend fun CommandSender.requestInput(hint: String): String {
        return when (this) {
            is ConsoleCommandSender -> MiraiConsole.requestInput(hint)
            is CommandSenderOnMessage<*> -> {
                sendMessage(hint)
                fromEvent.nextMessage().content
            }
            else -> throw IllegalStateException("未知环境 $this")
        }
    }

    @SubCommand
    suspend fun CommandSender.oauth() {
        NetDisk.runCatching {
            authorize { url ->
                requestInput("请打开连接，然后在十分钟内输入获得的认证码, $url")
            } to user()
        }.onSuccess { (token, user) ->
            logger.info { "百度云用户认证成功, ${user.baiduName} by $token" }
            sendMessage("百度云用户认证成功, ${user.baiduName} by $token")
        }.onFailure { cause ->
            logger.warning({ "认证失败" }, cause)
            sendMessage("百度云用户认证失败, ${cause.message}")
        }
    }

    @SubCommand
    suspend fun CommandSender.refresh(token: String) {
        NetdiskAuthStatus.refreshTokenValue = token
        NetDisk.runCatching {
            refresh() to user()
        }.onSuccess { (token, user) ->
            logger.info { "百度云用户认证成功, ${user.baiduName} by $token" }
            sendMessage("百度云用户认证成功, ${user.baiduName} by $token")
        }.onFailure { cause ->
            logger.warning({ "认证失败" }, cause)
            sendMessage("百度云用户认证失败, ${cause.message}")
        }
    }

    @SubCommand
    suspend fun CommandSender.host() {
        NetDisk.runCatching {
            host()
        }.onSuccess { host ->
            logger.info { "host 刷新成功 $host" }
            sendMessage("host 刷新成功，共 ${host.server.size} 个服务器")
        }.onFailure { cause ->
            logger.warning({ "刷新失败" }, cause)
            sendMessage("host 刷新失败, ${cause.message}")
        }
    }

    @SubCommand
    suspend fun CommandSender.user() {
        NetDisk.runCatching {
            user()
        }.onSuccess { user ->
            logger.info { "百度云用户刷新成功, ${user.baiduName} - ${user.vip}" }
            sendMessage("百度云用户刷新成功, ${user.baiduName} - ${user.vip}")
        }.onFailure { cause ->
            logger.warning({ "刷新失败" }, cause)
            sendMessage("百度云用户刷新失败, ${cause.message}")
        }
    }
}