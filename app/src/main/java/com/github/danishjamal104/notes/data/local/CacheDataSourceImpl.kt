package com.github.danishjamal104.notes.data.local

import com.github.danishjamal104.notes.data.entity.cache.NoteCacheEntity
import com.github.danishjamal104.notes.data.entity.cache.UserCacheEntity
import com.github.danishjamal104.notes.data.local.dao.NoteDao
import com.github.danishjamal104.notes.data.local.dao.UserDao
import com.github.danishjamal104.notes.data.mapper.NoteMapper
import com.github.danishjamal104.notes.data.mapper.UserMapper
import com.github.danishjamal104.notes.data.model.Note
import com.github.danishjamal104.notes.data.model.User
import com.github.danishjamal104.notes.util.exception.UserStateException

class CacheDataSourceImpl
constructor(
    private val userMapper: UserMapper,
    private val noteMapper: NoteMapper,
    private val userDao: UserDao,
    private val noteDao: NoteDao
): CacheDataSource{

    override suspend fun addUser(user: User): Long {
        return userDao.insertUser(userMapper.mapToEntity(user))
    }

    override suspend fun getUser(userId: String): User {
        val result: List<UserCacheEntity> = userDao.getUser(userId)
        if(result.size != 1) {
            throw UserStateException("$userId has multiple account setup")
        }
        return userMapper.mapFromEntity(result[0])
    }

    override suspend fun deleteUser(userId: String): Int {
        return userDao.deleteUser(userId);
    }

    override suspend fun addNote(note: Note): Long {
        return noteDao.insertNote(noteMapper.mapToEntity(note))
    }

    override suspend fun getNotes(userId: String): List<Note> {
        val result: List<NoteCacheEntity> = noteDao.getNotes(userId)
        return noteMapper.mapFromEntityList(result)
    }

    override suspend fun updateNote(note: Note): Int {
        return noteDao.updateNote(noteMapper.mapToEntity(note))
    }
}