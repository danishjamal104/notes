package com.github.danishjamal104.notes.data.local.dao

import androidx.room.*
import com.github.danishjamal104.notes.data.entity.cache.LabelCacheEntity

@Dao
interface LabelDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLabel(noteCacheEntity: LabelCacheEntity): Long

    @Query("SELECT * FROM labels WHERE userId = :userId")
    suspend fun fetchLabels(userId: String): List<LabelCacheEntity>

    @Update
    suspend fun updateLabel(noteCacheEntity: LabelCacheEntity): Int

    @Delete
    suspend fun deleteLabel(noteCacheEntity: LabelCacheEntity): Int

}