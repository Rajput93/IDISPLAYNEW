package com.app.idisplaynew.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule")
data class ScheduleEntity(
    @PrimaryKey val scheduleId: Int,
    val layoutId: Int?,
    val layoutName: String?,
    val startTime: String?,
    val endTime: String?,
    val priority: Int,
    val lastUpdated: String?,
    val layoutJson: String,
    val tickersJson: String
)
