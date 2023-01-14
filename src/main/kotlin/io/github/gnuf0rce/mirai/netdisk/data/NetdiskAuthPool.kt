package io.github.gnuf0rce.mirai.netdisk.data

import io.github.gnuf0rce.mirai.netdisk.*
import kotlinx.serialization.modules.*
import net.mamoe.mirai.console.data.*
import net.mamoe.mirai.console.util.*
import xyz.cssxsh.baidu.api.*
import xyz.cssxsh.baidu.oauth.*
import java.time.*

@PublishedApi
internal object NetdiskAuthPool : AutoSavePluginData("pool") {

    override val serializersModule: SerializersModule = SerializersModule {
        contextual(OffsetDateTimeSerializer)
    }

    @ValueName("expires")
    val expires: MutableMap<Long, OffsetDateTime> by value()

    @ValueName("access_token")
    val accessTokenValue: MutableMap<Long, String> by value()

    @ValueName("refresh_token")
    val refreshTokenValue: MutableMap<Long, String> by value()

    @ValueName("scope")
    val scope: MutableMap<Long, List<String>> by value()

    fun status(id: Long) : BaiduAuthStatus {
        return object : BaiduAuthStatus {
            override var accessTokenValue: String
                get() = this@NetdiskAuthPool.accessTokenValue[id].orEmpty()
                set(value) = this@NetdiskAuthPool.accessTokenValue.set(id, value)

            override var expires: OffsetDateTime
                get() = this@NetdiskAuthPool.expires[id] ?: OffsetDateTime.MIN
                set(value) = this@NetdiskAuthPool.expires.set(id, value)

            override var refreshTokenValue: String
                get() = this@NetdiskAuthPool.refreshTokenValue[id].orEmpty()
                set(value) = this@NetdiskAuthPool.refreshTokenValue.set(id, value)

            override var scope: List<String>
                get() = this@NetdiskAuthPool.scope[id].orEmpty()
                set(value) = this@NetdiskAuthPool.scope.set(id, value)
        }
    }

    @ConsoleExperimentalApi
    override fun onInit(owner: PluginDataHolder, storage: PluginDataStorage) {
        super.onInit(owner, storage)
        val now = OffsetDateTime.now()
        for ((id, time) in expires) {
            if (time > now) {
                val client = BaiduNetDiskPool.create(id)
                BaiduNetDiskPool.cache[id] = client
            }
        }
    }
}