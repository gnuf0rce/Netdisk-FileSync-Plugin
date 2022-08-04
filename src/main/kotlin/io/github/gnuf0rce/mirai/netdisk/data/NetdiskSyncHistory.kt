package io.github.gnuf0rce.mirai.netdisk.data

import io.github.gnuf0rce.mirai.netdisk.entry.*
import net.mamoe.mirai.console.data.*

internal object NetdiskSyncHistory : AutoSavePluginData("history") {
    @ValueName("sync_upload_records")
    val records: MutableList<SyncUploadRecord> by value()
}