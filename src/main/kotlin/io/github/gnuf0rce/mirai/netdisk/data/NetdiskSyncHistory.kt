package io.github.gnuf0rce.mirai.netdisk.data

import io.github.gnuf0rce.mirai.netdisk.entry.*
import net.mamoe.mirai.console.data.*

@PublishedApi
internal object NetdiskSyncHistory : AutoSavePluginData("history") {
    @ValueName("sync_upload_records")
    val sync: MutableList<SyncUploadRecord> by value()

    @ValueName("code_save_records")
    val code: MutableList<CodeSaveRecord> by value()

    @ValueName("share_save_records")
    val share: MutableList<ShareSaveRecord> by value()

    @ValueName("log_upload_records")
    val log: MutableList<LogUploadRecord> by value()
}