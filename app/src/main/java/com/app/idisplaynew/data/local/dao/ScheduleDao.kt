package com.app.idisplaynew.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app.idisplaynew.data.local.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: ScheduleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schedules: List<ScheduleEntity>)

    @Query("UPDATE schedule SET layoutJson = :layoutJson, tickersJson = :tickersJson, lastUpdated = :lastUpdated WHERE scheduleId = :scheduleId")
    suspend fun updateLayoutAndTickers(scheduleId: Int, layoutJson: String, tickersJson: String, lastUpdated: String?)

    @Delete
    suspend fun delete(schedule: ScheduleEntity)

    @Query("DELETE FROM schedule WHERE scheduleId = :scheduleId")
    suspend fun deleteByScheduleId(scheduleId: Int)

    /** Delete schedules whose endTime has passed (endTime < current time). */
    @Query("DELETE FROM schedule WHERE endTime IS NOT NULL AND endTime != '' AND endTime < :nowIso")
    suspend fun deleteExpired(nowIso: String)

    @Query("SELECT * FROM schedule WHERE scheduleId = :scheduleId LIMIT 1")
    suspend fun getByScheduleId(scheduleId: Int): ScheduleEntity?

    @Query("SELECT * FROM schedule WHERE (startTime IS NULL OR startTime = '' OR startTime <= :nowIso) AND (endTime IS NULL OR endTime = '' OR endTime >= :nowIso) ORDER BY priority DESC LIMIT 1")
    fun getCurrentSchedule(nowIso: String): Flow<ScheduleEntity?>

    @Query("SELECT * FROM schedule WHERE (startTime IS NULL OR startTime = '' OR startTime <= :nowIso) AND (endTime IS NULL OR endTime = '' OR endTime >= :nowIso) ORDER BY priority DESC LIMIT 1")
    suspend fun getCurrentScheduleOnce(nowIso: String): ScheduleEntity?

    @Query("SELECT * FROM schedule")
    suspend fun getAll(): List<ScheduleEntity>
}
