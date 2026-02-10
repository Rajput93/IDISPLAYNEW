package com.app.idisplaynew.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.app.idisplaynew.data.local.dao.MediaFileDao
import com.app.idisplaynew.data.local.dao.ScheduleDao
import com.app.idisplaynew.data.local.entity.MediaFileEntity
import com.app.idisplaynew.data.local.entity.ScheduleEntity

@Database(
    entities = [ScheduleEntity::class, MediaFileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
    abstract fun mediaFileDao(): MediaFileDao
}
