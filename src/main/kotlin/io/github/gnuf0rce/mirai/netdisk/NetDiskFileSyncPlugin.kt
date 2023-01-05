package io.github.gnuf0rce.mirai.netdisk

import io.github.gnuf0rce.mirai.netdisk.command.*
import io.github.gnuf0rce.mirai.netdisk.data.*
import kotlinx.coroutines.*
import net.mamoe.mirai.console.*
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.plugin.*
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.console.util.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.utils.*
import xyz.cssxsh.baidu.disk.data.*
import java.io.File

public object NetDiskFileSyncPlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "io.github.gnuf0rce.file-sync",
        name = "file-sync",
        version = "1.3.7",
    ) {
        author("cssxsh")

        dependsOn("xyz.cssxsh.mirai.plugin.mirai-hibernate-plugin", true)
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

        check(NetdiskOauthConfig.appKey.isNotBlank()) {
            "插件需要百度网盘API支持，请到 https://pan.baidu.com/union/main/application/personal 申请应用，并填入oauth.yml"
        }
        try {
            NetDiskFileSyncRecorder.enable()
            logger.info { "审核记录将记录到数据库 ${NetDiskFileSyncRecorder.database()}" }
        } catch (_: NoClassDefFoundError) {
            logger.info { "审核记录将记录到 history.yml" }
            NetdiskSyncHistory.reload()
        }

        logger.info { "请将文件同步权限授予群 /perm add g* ${NetDisk.permission.id}" }

        NetDisk.registerTo(globalEventChannel())

        BaiduOAuthCommand.register()

        if (NetdiskUploadConfig.log) {
            NetDisk.launch {
                val log = File("./logs")
                    .listFiles()
                    .orEmpty()
                    .maxBy { it.lastModified() }

                val file = NetDisk.upload(file = log, path = "mirai-logs", ondup = OnDupType.NEW_COPY)
                NetDiskFileSyncRecorder.record(file = file)
            }
        } else {
            logger.info { "如果需要上传日志文件，请修改 upload.yml 中的 log 配置项" }
        }
    }

    override fun onDisable() {
        BaiduOAuthCommand.unregister()

        NetDisk.cancel()
    }
}