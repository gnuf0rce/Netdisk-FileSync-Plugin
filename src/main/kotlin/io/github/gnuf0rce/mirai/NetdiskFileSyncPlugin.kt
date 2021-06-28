package io.github.gnuf0rce.mirai

import io.github.gnuf0rce.mirai.command.BaiduOAuthCommand
import io.github.gnuf0rce.mirai.data.FileSyncConfig
import io.github.gnuf0rce.mirai.data.NetdiskOauthConfig
import io.github.gnuf0rce.mirai.data.NetdiskUserData
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.console.util.ConsoleExperimentalApi


object NetdiskFileSyncPlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "io.github.gnuf0rce.netdisk-filesync-plugin",
        name = "netdisk-filesync-plugin",
        version = "1.0.0-dev-1",
    ) {
        author("cssxsh")
    }
) {

    @ConsoleExperimentalApi
    override fun onEnable() {
        NetdiskOauthConfig.reload()
        NetdiskUserData.reload()
        FileSyncConfig.reload()

        if(NetdiskOauthConfig.appKey.isBlank()) {
            logger.warning("插件需要百度网盘API支持，请到 https://pan.baidu.com/union/main/application/personal 申请应用，并填入oauth.yml")
            return
        }

        NetDiskClient.reload()

        NetDiskClient.subscribe()

        BaiduOAuthCommand.register()
    }

    @ConsoleExperimentalApi
    override fun onDisable() {
        NetDiskClient.save()

        BaiduOAuthCommand.unregister()
    }
}