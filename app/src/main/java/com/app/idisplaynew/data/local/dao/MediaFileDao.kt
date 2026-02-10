package com.app.idisplaynew.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app.idisplaynew.data.local.entity.MediaFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaFileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(media: MediaFileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(media: List<MediaFileEntity>)

    @Delete
    suspend fun delete(media: MediaFileEntity)

    @Query("DELETE FROM media_file WHERE fileName = :fileName")
    suspend fun deleteByFileName(fileName: String)

    @Query("DELETE FROM media_file WHERE scheduleId = :scheduleId")
    suspend fun deleteByScheduleId(scheduleId: Int)

    @Query("SELECT * FROM media_file WHERE fileName = :fileName LIMIT 1")
    suspend fun getByFileName(fileName: String): MediaFileEntity?

    @Query("SELECT * FROM media_file WHERE scheduleId = :scheduleId")
    suspend fun getAllByScheduleId(scheduleId: Int): List<MediaFileEntity>

    @Query("SELECT * FROM media_file WHERE scheduleId = :scheduleId")
    fun getAllByScheduleIdFlow(scheduleId: Int): Flow<List<MediaFileEntity>>
}
