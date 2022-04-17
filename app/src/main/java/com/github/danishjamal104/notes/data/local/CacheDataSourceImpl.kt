package com.github.danishjamal104.notes.data.local

import com.github.danishjamal104.notes.data.entity.cache.NoteCacheEntity
import com.github.danishjamal104.notes.data.entity.cache.NoteLabelJoinEntity
import com.github.danishjamal104.notes.data.entity.cache.UserCacheEntity
import com.github.danishjamal104.notes.data.local.dao.LabelDao
import com.github.danishjamal104.notes.data.local.dao.NoteDao
import com.github.danishjamal104.notes.data.local.dao.NoteLabelJoinDao
import com.github.danishjamal104.notes.data.local.dao.UserDao
import com.github.danishjamal104.notes.data.mapper.LabelMapper
import com.github.danishjamal104.notes.data.mapper.NoteMapper
import com.github.danishjamal104.notes.data.mapper.UserMapper
import com.github.danishjamal104.notes.data.model.Label
import com.github.danishjamal104.notes.data.model.Note
import com.github.danishjamal104.notes.data.model.User
import com.github.danishjamal104.notes.util.exception.UserStateException
import java.lang.Exception

class CacheDataSourceImpl
constructor(
    private val userDao: UserDao,
    private val noteDao: NoteDao,
    private val labelDao: LabelDao,
    private val noteLabelJoinDao: NoteLabelJoinDao
): CacheDataSource{

    override suspend fun addUser(user: User): Long {
        return userDao.insertUser(UserMapper.mapToEntity(user))
    }

    override suspend fun getUser(userId: String): User {
        val result: List<UserCacheEntity> = userDao.getUser(userId)
        if(result.size != 1) {
            throw UserStateException("$userId has multiple account setup")
        }
        return UserMapper.mapFromEntity(result[0])
    }

    override suspend fun deleteUser(userId: String): Int {
        return userDao.deleteUser(userId)
    }

    override suspend fun addNote(note: Note): Long {
        return noteDao.insertNote(NoteMapper.mapToEntity(note))
    }

    override suspend fun getNotes(userId: String): List<Note> {
        val result: List<NoteCacheEntity> = noteDao.getNotes(userId)
        return NoteMapper.mapFromEntityList(result)
    }

    override suspend fun getNote(id: Int, userId: String): Note {
        val result = noteDao.getNote(id, userId)
        if(result.size != 1) {
            throw Exception("Note doesn't exist with id = $id")
        }
        return NoteMapper.mapFromEntity(result[0])
    }

    override suspend fun updateNote(note: Note): Int {
        val cacheNoteEntity = NoteMapper.mapToEntity(note)
        cacheNoteEntity.id = note.id
        return noteDao.updateNote(cacheNoteEntity)
    }

    override suspend fun deleteNote(note: Note): Int {
        val cacheNoteEntity = NoteMapper.mapToEntity(note)
        cacheNoteEntity.id = note.id
        return noteDao.deleteNote(cacheNoteEntity)
    }

    override suspend fun deleteAllNote(userId: String): Int {
        return noteDao.deleteAllNoteOfUser(userId)
    }

    override suspend fun getLabels(userId: String): List<Label> {
        val result = labelDao.fetchLabels(userId)
        return LabelMapper.mapFromEntityList(result)
    }

    override suspend fun getLabel(userId: String, labelId: Int): Label {
        val result = labelDao.getLabel(labelId, userId)
        if(result.size != 1) {
            throw Exception("Label doesn't exist with id = $labelId")
        }
        return LabelMapper.mapFromEntity(result[0])
    }

    override suspend fun createLabel(label: Label): Long {
        return labelDao.insertLabel(LabelMapper.mapToEntity(label))
    }

    override suspend fun updateLabel(label: Label): Int {
        return labelDao.updateLabel(LabelMapper.mapToEntity(label))
    }

    override suspend fun deleteLabel(label: Label): Int {
        return labelDao.deleteLabel(LabelMapper.mapToEntity(label))
    }

    override suspend fun getNotesForLabel(userId: String, labelId: Int): List<Note> {
        val result = noteLabelJoinDao.getNotesForLabel(labelId)
        return NoteMapper.mapFromEntityList(result)
    }

    override suspend fun getLabelsForNote(userId: String, noteId: Int): List<Label> {
        val result = noteLabelJoinDao.getLabelsForNote(userId, noteId)
        return LabelMapper.mapFromEntityList(result)
    }

    override suspend fun addLabelInNote(userId: String, noteId: Int, labelId: Int): Long {
        val entity = NoteLabelJoinEntity().apply {
            this.noteId = noteId
            this.labelId = labelId
        }
        return noteLabelJoinDao.insert(entity)
    }

    override suspend fun removeLabelInNote(userId: String, noteId: Int, labelId: Int): Int {
        val entity = NoteLabelJoinEntity().apply {
            this.noteId = noteId
            this.labelId = labelId
        }
        return noteLabelJoinDao.deleteNoteLabelJoinEntry(entity)
    }

    override suspend fun deleteLabelAssociatedWithEachNote(userId: String, label: Label): Int {
        when (noteLabelJoinDao.deleteByLabelId(label.id)) {
            !in 0..Int.MAX_VALUE -> return -1
        }
        return deleteLabel(label)
    }

}