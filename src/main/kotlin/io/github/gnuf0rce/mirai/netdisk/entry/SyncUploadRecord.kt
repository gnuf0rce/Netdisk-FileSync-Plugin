package io.github.gnuf0rce.mirai.netdisk.entry

import jakarta.persistence.*

@Entity
@Table(name = "sync_upload_record")
@kotlinx.serialization.Serializable
public data class SyncUploadRecord(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "contact_id", nullable = false, updatable = false)
    val contactId: Long = 0,
    @Column(name = "uploader_id", nullable = false, updatable = false)
    val uploaderId: Long = 0,
    @Column(name = "upload_time", nullable = false, updatable = false)
    val uploadTime: Long = 0,
    @Column(name = "ids", nullable = true, updatable = false)
    val ids: String = "",
    @Column(name = "code", nullable = false, updatable = false)
    val code: String = "",
) : java.io.Serializable