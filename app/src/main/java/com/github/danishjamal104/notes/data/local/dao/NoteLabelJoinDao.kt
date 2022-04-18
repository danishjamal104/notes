package com.github.danishjamal104.notes.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.github.danishjamal104.notes.data.entity.cache.LabelCacheEntity
import com.github.danishjamal104.notes.data.entity.cache.NoteCacheEntity
import com.github.danishjamal104.notes.data.entity.cache.NoteLabelJoinEntity

@Dao
interface NoteLabelJoinDao {

    @Insert
    suspend fun insert(noteLabelJoinEntity: NoteLabelJoinEntity): Long

    @Query("SELECT * FROM notes INNER JOIN note_label_join ON id=noteId WHERE labelId=:labelId")
    suspend fun getNotesForLabel(labelId: Int): List<NoteCacheEntity>

    @Query("SELECT * FROM notes INNER JOIN note_label_join ON id=noteId WHERE labelId IN (:labelIds)")
    suspend fun getNotesForLabels(labelIds: List<Int>): List<NoteCacheEntity>

    @Query("SELECT * FROM labels AS l1 INNER JOIN note_label_join ON l1.id=labelId WHERE l1.userId=:userId AND noteId=:noteId")
    suspend fun getLabelsForNote(userId: String, noteId: Int): List<LabelCacheEntity>

    @Delete
    suspend fun deleteNoteLabelJoinEntry(noteLabelJoinEntity: NoteLabelJoinEntity): Int

    @Query("DELETE FROM note_label_join WHERE labelId=:labelId")
    suspend fun deleteByLabelId(labelId: Int): Int

}