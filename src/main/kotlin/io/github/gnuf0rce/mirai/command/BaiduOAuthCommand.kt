package io.github.gnuf0rce.mirai.command

import io.github.gnuf0rce.mirai.*
import net.mamoe.mirai.console.*
import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.console.util.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.*
import net.mamoe.mirai.utils.*
import xyz.cssxsh.baidu.disk.*

internal object BaiduOAuthCommand : SimpleCommand(
    owner = NetDiskFileSyncPlugin,
    "baidu-oauth",
    description = "文件同步 百度账号认证指令"
) {
    private val logger by NetDiskFileSyncPlugin::logger

    private suspend fun CommandSender.read(): String {
        return when (this) {
            is ConsoleCommandSender -> MiraiConsole.requestInput("")
            is CommandSenderOnMessage<*> -> fromEvent.nextMessage().content
            else -> throw IllegalStateException("未知环境 $this")
        }
    }

    @Handler
    suspend fun CommandSender.handle() {
        NetDisk.runCatching {
            authorize { url ->
                sendMessage("请打开连接，然后在十分钟内输入获得的认证码, $url")
                read()
            } to getUserInfo()
        }.onSuccess { (token, user) ->
            logger.info { "百度云用户认证成功, ${user.baiduName} by $token" }
            sendMessage("百度云用户认证成功, ${user.baiduName} by $token")
        }.onFailure {
            logger.warning({ "认证失败" }, it)
            sendMessage("百度云用户认证失败, ${it.message}")
        }
    }
}