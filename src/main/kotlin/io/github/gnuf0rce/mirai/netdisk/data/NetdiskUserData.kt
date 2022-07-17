package io.github.gnuf0rce.mirai.netdisk.data

import kotlinx.serialization.modules.*
import net.mamoe.mirai.console.data.*
import xyz.cssxsh.baidu.api.*
import xyz.cssxsh.baidu.oauth.*
import java.time.*

internal object NetdiskUserData : AutoSavePluginData("user"), BaiduAuthStatus {

    override val serializersModule: SerializersModule = SerializersModule {
        contextual(OffsetDateTimeSerializer)
    }

    @ValueName("expires")
    override var expires: OffsetDateTime by value()

    @ValueName("access_token")
    override var accessTokenValue: String by value("")

    @ValueName("refresh_token")
    override var refreshTokenValue: String by value("")

    @ValueName("scope")
    override var scope: List<String> by value()
}