package com.github.danishjamal104.notes.data.repository.note

import com.github.danishjamal104.notes.data.model.Note
import com.github.danishjamal104.notes.util.ServiceResult

interface NotesRepository {
    suspend fun createNote(noteText: String, noteTitle: String? = null): ServiceResult<Unit>
    suspend fun insertNotes(notes: List<Note>): ServiceResult<Unit>
    suspend fun insertNote(note: Note): ServiceResult<Unit>
    suspend fun getNotes(): ServiceResult<List<Note>>
    suspend fun getNote(noteId: Int): ServiceResult<Note>
    suspend fun updateNote(note: Note): ServiceResult<Note>
    suspend fun deleteNote(note: Note): ServiceResult<Note>
    suspend fun deleteAllNotes(): ServiceResult<Unit>
}