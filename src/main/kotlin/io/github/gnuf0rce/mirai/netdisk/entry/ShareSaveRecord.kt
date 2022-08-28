package io.github.gnuf0rce.mirai.netdisk.entry

import jakarta.persistence.*

@Entity
@Table(name = "share_save_record")
@kotlinx.serialization.Serializable
public data class ShareSaveRecord(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "target_id", nullable = false, updatable = false)
    val targetId: Long = 0,
    @Column(name = "from_id", nullable = false, updatable = false)
    val fromId: Long = 0,
    @Column(name = "time", nullable = false, updatable = false)
    val time: Int = 0,
    @Column(name = "ids", nullable = true, updatable = false)
    val ids: String = "",
    @Column(name = "surl", nullable = false, updatable = false)
    val surl: String = "",
    @Column(name = "password", nullable = false, updatable = false)
    val password: String = "",
) : java.io.Serializable