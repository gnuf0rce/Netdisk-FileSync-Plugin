package io.github.gnuf0rce.mirai.netdisk.data

import net.mamoe.mirai.console.data.*

internal object NetdiskUploadConfig : ReadOnlyPluginConfig("upload") {
    @ValueName("https")
    val https by value(false)

    @ValueName("reply")
    val reply by value(true)

    @ValueName("log")
    val log by value(false)
}