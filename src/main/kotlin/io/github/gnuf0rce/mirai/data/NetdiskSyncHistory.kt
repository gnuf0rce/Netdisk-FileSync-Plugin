package io.github.gnuf0rce.mirai.data

import net.mamoe.mirai.console.data.*

internal object NetdiskSyncHistory : AutoSavePluginData("history") {
    @ValueName("records")
    val records: MutableList<String> by value()
}