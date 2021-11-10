package io.github.gnuf0rce.mirai.data

import net.mamoe.mirai.console.data.*

object NetdiskUserData : AutoSavePluginData("user") {
    @ValueName("expires")
    var expires: String by value("")

    @ValueName("access_token")
    var accessToken: String by value("")

    @ValueName("refresh_token")
    var refreshToken: String by value("")
}