package com.github.danishjamal104.notes.data.repository.note

import com.github.danishjamal104.notes.data.local.CacheDataSource
import com.github.danishjamal104.notes.data.model.Label
import com.github.danishjamal104.notes.data.model.Note
import com.github.danishjamal104.notes.util.ServiceResult
import com.github.danishjamal104.notes.util.encrypt
import com.github.danishjamal104.notes.util.sharedpreference.EncryptionPreferences
import com.github.danishjamal104.notes.util.sharedpreference.UserPreferences
import java.util.*

class NotesRepositoryImpl
constructor(
    private val cacheDataSource: CacheDataSource,
    private val userPreferences: UserPreferences,
    private val encryptionPreferences: EncryptionPreferences
) : NotesRepository {

    private val userId get() = userPreferences.getUserId()


    override suspend fun createNote(noteText: String, noteTitle: String?): ServiceResult<Unit> {
        val encryptedNoteText = noteText.encrypt(encryptionPreferences.key)
        val title = noteTitle ?: ""
        val note = Note(-1, userId, encryptedNoteText, title, Date().time)
        return try {
            cacheDataSource.addNote(note)
            ServiceResult.Success(Unit)
        } catch (e: Exception) {
            ServiceResult.Error("" + e.localizedMessage)
        }
    }

    override suspend fun insertNote(note: Note): ServiceResult<Unit> {
        return try {
            cacheDataSource.addNote(note)
            ServiceResult.Success(Unit)
        } catch (e: Exception) {
            ServiceResult.Error("" + e.localizedMessage)
        }
    }

    override suspend fun insertNotes(notes: List<Note>): ServiceResult<Unit> {
        return try {
            notes.forEach {
                cacheDataSource.addNote(it)
            }
            ServiceResult.Success(Unit)
        } catch (e: Exception) {
            ServiceResult.Error("" + e.localizedMessage)
        }
    }

    override suspend fun getNotes(): ServiceResult<List<Note>> {
        return try {
            val result = cacheDataSource.getNotes(userId)
            ServiceResult.Success(result)
        } catch (e: Exception) {
            ServiceResult.Error("" + e.localizedMessage)
        }
    }

    override suspend fun getNoteOfLabels(labels: List<Label>): ServiceResult<List<Note>> {
        return try {
            val result = cacheDataSource.getNotesForLabels(userId, labels.map { it.id })
            ServiceResult.Success(result.toSet().toList())
        } catch (e: Exception) {
            ServiceResult.Error("" + e.localizedMessage)
        }
    }

    override suspend fun getNote(noteId: Int): ServiceResult<Note> {
        return try {
            val result = cacheDataSource.getNote(noteId, userId)
            ServiceResult.Success(result)
        } catch (e: Exception) {
            ServiceResult.Error("" + e.localizedMessage)
        }
    }

    override suspend fun updateNote(note: Note): ServiceResult<Note> {
        note.value = note.value.encrypt(encryptionPreferences.key)
        return try {
            when (cacheDataSource.updateNote(note)) {
                0 -> ServiceResult.Error("Updating failed ")
                1 -> ServiceResult.Success(note)
                else -> ServiceResult.Error("Invalid update")
            }
        } catch (e: Exception) {
            ServiceResult.Error("" + e.localizedMessage)
        }
    }

    override suspend fun deleteNote(note: Note): ServiceResult<Note> {
        return try {
            when (cacheDataSource.deleteNote(note)) {
                0 -> ServiceResult.Error("Deletion failed ")
                1 -> ServiceResult.Success(note)
                else -> ServiceResult.Error("Invalid deletion")
            }
        } catch (e: Exception) {
            ServiceResult.Error("" + e.localizedMessage)
        }
    }

    override suspend fun deleteAllNotes(): ServiceResult<Unit> {
        return try {
            when (cacheDataSource.deleteAllNote(userId)) {
                in 0..Int.MAX_VALUE -> ServiceResult.Success(Unit)
                else -> ServiceResult.Error("Deletion failed")
            }
        } catch (e: Exception) {
            ServiceResult.Error("" + e.localizedMessage)
        }
    }

}