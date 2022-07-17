package io.github.gnuf0rce.mirai.netdisk

import io.github.gnuf0rce.mirai.netdisk.command.*
import io.github.gnuf0rce.mirai.netdisk.data.*
import net.mamoe.mirai.console.MiraiConsole
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.plugin.*
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.console.util.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.utils.*

public object NetDiskFileSyncPlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "io.github.gnuf0rce.file-sync",
        name = "file-sync",
        version = "1.3.5",
    ) {
        author("cssxsh")
    }
) {

    override fun onEnable() {
        // XXX: mirai console version check
        check(SemVersion.parseRangeRequirement(">= 2.12.0-RC").test(MiraiConsole.version)) {
            "$name $version 需要 Mirai-Console 版本 >= 2.12.0，目前版本是 ${MiraiConsole.version}"
        }

        NetdiskOauthConfig.reload()
        NetdiskUploadConfig.reload()
        NetdiskUserData.reload()
        NetdiskSyncHistory.reload()

        check(NetdiskOauthConfig.appKey.isNotBlank()) {
            "插件需要百度网盘API支持，请到 https://pan.baidu.com/union/main/application/personal 申请应用，并填入oauth.yml"
        }

        logger.info { "请将文件同步权限授予群 /perm add g* ${NetDisk.permission.id}" }

        NetDisk.registerTo(globalEventChannel())

        BaiduOAuthCommand.register()
    }

    override fun onDisable() {
        BaiduOAuthCommand.unregister()

        NetDisk.cancel()
    }
}