package io.github.gnuf0rce.mirai.data

import net.mamoe.mirai.console.data.*

object NetdiskUploadConfig : ReadOnlyPluginConfig("upload") {
    @ValueName("https")
    val https by value(false)

    @ValueName("reply")
    val reply by value(true)
}