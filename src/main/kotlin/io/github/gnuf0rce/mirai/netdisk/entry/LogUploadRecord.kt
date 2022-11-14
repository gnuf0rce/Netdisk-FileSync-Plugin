package io.github.gnuf0rce.mirai.netdisk.entry

import jakarta.persistence.*

@Entity
@Table(name = "log_upload_record")
@kotlinx.serialization.Serializable
public data class LogUploadRecord(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "upload_time", nullable = false, updatable = false)
    val uploadTime: Long = 0,
    @Column(name = "filename", nullable = false, updatable = false)
    val filename: String = "",
) : java.io.Serializable