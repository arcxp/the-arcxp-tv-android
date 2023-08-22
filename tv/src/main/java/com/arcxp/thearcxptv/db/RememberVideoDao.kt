package com.arcxp.thearcxptv.db

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RememberVideoDao {

    @Query("SELECT * FROM videoToRemember where uuid = :uuid")
    suspend fun getVideoById(uuid: String): VideoToRemember?

    @Query("SELECT * FROM videoToRemember where uuid = :uuid")
    fun getVideoFlowById(uuid: String): Flow<VideoToRemember>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun rememberVideo(videoToRemember: VideoToRemember)

    @Query("SELECT * FROM videoToRemember WHERE playPosition > 0 AND playPosition < playLength ORDER BY createdAt LIMIT :size")
    fun getRememberedVideos(size: Int = 20): LiveData<List<VideoToRemember>> //TODO make this default value a constant


    @Update(entity = VideoToRemember::class)
    suspend fun updatePosition(update: UpdatePosition)

    @Query("SELECT playPosition FROM videoToRemember where uuid = :uuid")
    fun observePosition(uuid: String): LiveData<Long?>
}