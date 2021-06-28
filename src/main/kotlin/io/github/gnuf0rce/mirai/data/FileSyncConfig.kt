package io.github.gnuf0rce.mirai.data

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object FileSyncConfig: AutoSavePluginConfig("sync") {
    @ValueDescription("例外的联系人")
    val excludes by value(mutableListOf<Long>())
}