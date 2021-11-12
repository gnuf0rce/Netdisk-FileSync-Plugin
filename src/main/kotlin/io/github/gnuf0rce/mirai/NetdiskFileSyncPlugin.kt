package io.github.gnuf0rce.mirai

import io.github.gnuf0rce.mirai.command.*
import io.github.gnuf0rce.mirai.data.*
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.utils.*

object NetdiskFileSyncPlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "io.github.gnuf0rce.file-sync",
        name = "file-sync",
        version = "1.0.2",
    ) {
        author("cssxsh")
    }
) {

    override fun onEnable() {
        NetdiskOauthConfig.reload()
        NetdiskUploadConfig.reload()
        NetdiskUserData.reload()
        NetdiskSyncHistory.reload()

        check(NetdiskOauthConfig.appKey.isNotBlank()) {
            "插件需要百度网盘API支持，请到 https://pan.baidu.com/union/main/application/personal 申请应用，并填入oauth.yml"
        }

        logger.info { "请将文件同步权限授予群 /perm g* ${NetDiskClient.permission.id}" }

        NetDiskClient.reload()

        NetDiskClient.subscribe()

        BaiduOAuthCommand.register()
    }

    override fun onDisable() {
        NetDiskClient.save()

        BaiduOAuthCommand.unregister()
    }
}