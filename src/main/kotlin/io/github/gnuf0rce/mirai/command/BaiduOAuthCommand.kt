package io.github.gnuf0rce.mirai.command

import io.github.gnuf0rce.mirai.*
import net.mamoe.mirai.console.*
import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.console.util.*
import net.mamoe.mirai.utils.*
import xyz.cssxsh.baidu.disk.*
import xyz.cssxsh.baidu.oauth.*

object BaiduOAuthCommand : SimpleCommand(
    owner = NetdiskFileSyncPlugin,
    "baidu-oauth",
    description = "文件同步 百度账号认证指令"
) {
    private val logger by NetdiskFileSyncPlugin::logger

    @Handler
    suspend fun ConsoleCommandSender.handle() {
        val url = NetDiskClient.getWebAuthorizeUrl(type = AuthorizeType.AUTHORIZATION)
        sendMessage("请打开连接，然后在十分钟内输入获得的认证码, $url")
        NetDiskClient.runCatching {
            val code = MiraiConsole.requestInput("")
            getAuthorizeToken(code = code).also { saveToken(token = it) } to getUserInfo()
        }.onSuccess { (token, user) ->
            logger.info { "百度云用户认证成功, ${user.baiduName} by $token" }
            sendMessage("百度云用户认证成功, ${user.baiduName} by $token")
        }.onFailure {
            logger.warning({ "认证失败" }, it)
            sendMessage("百度云用户认证失败, ${it.message}")
        }
    }
}