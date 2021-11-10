package io.github.gnuf0rce.mirai.data

import net.mamoe.mirai.console.data.*

object NetdiskSyncHistory : AutoSavePluginData("history") {
    @ValueName("records")
    val records by value(mutableListOf<String>())
}