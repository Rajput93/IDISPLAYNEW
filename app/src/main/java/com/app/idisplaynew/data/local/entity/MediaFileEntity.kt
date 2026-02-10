package com.app.idisplaynew.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_file",
    indices = [Index(value = ["fileName"], unique = true), Index(value = ["scheduleId"])]
)
data class MediaFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val localPath: String?,
    val url: String,
    val scheduleId: Int,
    val zoneId: Int,
    val mediaId: Int,
    val type: String,
    val duration: Int,
    val fileSizeBytes: Long,
    val checksum: String?
)
