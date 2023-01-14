package io.github.gnuf0rce.mirai.netdisk

import io.github.gnuf0rce.mirai.netdisk.data.*
import io.ktor.utils.io.errors.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.utils.*
import xyz.cssxsh.baidu.disk.*
import xyz.cssxsh.baidu.oauth.*
import xyz.cssxsh.baidu.oauth.exception.*
import java.util.*
import kotlin.properties.*
import kotlin.reflect.*

@PublishedApi
internal object BaiduNetDiskPool : ReadOnlyProperty<Contact?, BaiduNetDiskClient> {
    val cache: MutableMap<Long, BaiduNetDiskClient> = WeakHashMap()

    private val logger: MiraiLogger by lazy {
        try {
            NetDiskFileSyncPlugin.logger
        } catch (_: Exception) {
            MiraiLogger.Factory.create(this::class, "netdisk")
        }
    }

    override fun getValue(thisRef: Contact?, property: KProperty<*>): BaiduNetDiskClient {
        if (thisRef == null) return NetDisk
        val user = cache[thisRef.id]
        if (user != null) return user
        if (thisRef is Member) {
            val group = cache[thisRef.group.id]
            if (group != null) return group
        }
        return NetDisk
    }

    private var KClass<out Throwable>.count: Int by object : ReadWriteProperty<KClass<*>, Int> {
        private val history: MutableMap<KClass<*>, Int> = WeakHashMap()

        override fun getValue(thisRef: KClass<*>, property: KProperty<*>): Int {
            return history[thisRef] ?: 0
        }

        override fun setValue(thisRef: KClass<*>, property: KProperty<*>, value: Int) {
            history[thisRef] = value
        }
    }

    val defaultApiIgnore: suspend (Throwable) -> Boolean = { throwable ->
        when (throwable) {
            is java.net.UnknownHostException,
            is java.net.NoRouteToHostException -> false
            is IOException -> {
                val count = ++throwable::class.count
                if (count > 10) {
                    throwable::class.count = 0
                    false
                } else {
                    logger.warning { "NetDiskClient Ignore: $throwable" }
                    true
                }
            }
            else -> false
        }
    }

    fun create(id: Long): BaiduNetDiskClient {
        return object : BaiduNetDiskClient(config = NetdiskOauthConfig) {
            override val status: BaiduAuthStatus = NetdiskAuthPool.status(id)
            override val apiIgnore: suspend (Throwable) -> Boolean = defaultApiIgnore
            override suspend fun refreshToken(): String {
                return try {
                    super.refreshToken()
                } catch (cause: NotTokenException) {
                    NetDisk.logger.warning { "缺少 RefreshToken, 请使用 '/baidu bind' 绑定百度账号" }
                    throw cause
                }
            }
        }
    }
}