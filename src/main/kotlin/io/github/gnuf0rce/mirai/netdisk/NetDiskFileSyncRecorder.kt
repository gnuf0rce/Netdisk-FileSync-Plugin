package io.github.gnuf0rce.mirai.netdisk

import io.github.gnuf0rce.mirai.netdisk.data.*
import io.github.gnuf0rce.mirai.netdisk.entry.*
import net.mamoe.mirai.contact.file.*
import net.mamoe.mirai.message.data.*
import org.hibernate.*
import xyz.cssxsh.baidu.disk.*
import xyz.cssxsh.baidu.disk.data.*
import xyz.cssxsh.hibernate.*
import xyz.cssxsh.mirai.hibernate.*

public object NetDiskFileSyncRecorder {

    private var factory: java.io.Closeable? = null

    public fun enable() {
        jakarta.persistence.Entity::class.java
        factory = MiraiHibernateConfiguration(plugin = NetDiskFileSyncPlugin)
            .buildSessionFactory()
    }

    public fun disable() {
        factory?.close()
    }

    public fun database(): String? {
        val factory = factory ?: return null

        factory as SessionFactory

        return factory.fromSession { session -> session.getDatabaseMetaData() }.url
    }

    public fun record(file: AbsoluteFile, rapid: RapidUploadInfo) {

        val record = SyncUploadRecord(
            contactId = file.contact.id,
            uploaderId = file.uploaderId,
            uploadTime = file.uploadTime,
            ids = file.id,
            code = rapid.format()
        )

        val factory = factory

        if (factory != null) {
            factory as SessionFactory

            factory.fromTransaction { session -> session.persist(record) }
        } else {
            NetdiskSyncHistory.sync.add(record)
        }
    }

    public fun record(source: MessageSource, rapid: RapidUploadInfo) {

        val record = CodeSaveRecord(
            targetId = source.targetId,
            fromId = source.fromId,
            time = source.time,
            ids = source.ids.joinToString(","),
            code = rapid.format()
        )

        val factory = factory

        if (factory != null) {
            factory as SessionFactory

            factory.fromTransaction { session -> session.persist(record) }
        } else {
            NetdiskSyncHistory.code.add(record)
        }
    }

    public fun record(source: MessageSource, surl: String, password: String) {

        val record = ShareSaveRecord(
            targetId = source.targetId,
            fromId = source.fromId,
            time = source.time,
            ids = source.ids.joinToString(","),
            surl = surl,
            password = password
        )

        val factory = factory

        if (factory != null) {
            factory as SessionFactory

            factory.fromTransaction { session -> session.persist(record) }
        } else {
            NetdiskSyncHistory.share.add(record)
        }
    }

    public fun record(file: NetDiskFileInfo) {

        val record = LogUploadRecord(
            uploadTime = file.modified.toEpochSecond(),
            filename = file.filename
        )

        val factory = factory

        if (factory != null) {
            factory as SessionFactory

            factory.fromTransaction { session -> session.persist(record) }
        } else {
            NetdiskSyncHistory.log.add(record)
        }
    }

    public fun from(uploaderId: Long, start: Long, end: Long): List<SyncUploadRecord> {
        val factory = factory

        return if (factory != null) {
            factory as SessionFactory

            factory.fromSession { session ->
                session.withCriteria<SyncUploadRecord> { criteria ->
                    val root = criteria.from<SyncUploadRecord>()

                    criteria.select(root)
                        .where(
                            equal(root.get<Long>("uploaderId"), uploaderId),
                            between(root.get("upload_time"), start, end)
                        )
                        .orderBy(desc(root.get<Long>("upload_time")))
                }.list()
            }
        } else {
            NetdiskSyncHistory.sync
                .asSequence()
                .filter { it.uploaderId == uploaderId && it.uploadTime in start..end }
                .toMutableList()
                .apply { sortByDescending { it.uploadTime } }
        }
    }

    public fun target(contactId: Long, start: Long, end: Long): List<SyncUploadRecord> {
        val factory = factory

        return if (factory != null) {
            factory as SessionFactory

            factory.fromSession { session ->
                session.withCriteria<SyncUploadRecord> { criteria ->
                    val root = criteria.from<SyncUploadRecord>()

                    criteria.select(root)
                        .where(equal(root.get<Long>("contactId"), contactId), between(root.get("time"), start, end))
                        .orderBy(desc(root.get<Long>("upload_time")))
                }.list()
            }
        } else {
            NetdiskSyncHistory.sync
                .asSequence()
                .filter { it.contactId == contactId && it.uploadTime in start..end }
                .toMutableList()
                .apply { sortByDescending { it.uploadTime } }
        }
    }
}