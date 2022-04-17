package com.github.danishjamal104.notes.data.local

import com.github.danishjamal104.notes.data.model.Label
import com.github.danishjamal104.notes.data.model.Note
import com.github.danishjamal104.notes.data.model.User

interface CacheDataSource {

    suspend fun addUser(user: User): Long
    suspend fun getUser(userId: String): User
    suspend fun deleteUser(userId: String): Int

    suspend fun addNote(note: Note): Long
    suspend fun getNotes(userId: String): List<Note>
    suspend fun getNote(id: Int, userId: String): Note
    suspend fun updateNote(note: Note): Int
    suspend fun deleteNote(note: Note): Int
    suspend fun deleteAllNote(userId: String): Int

    suspend fun getLabels(userId: String): List<Label>
    suspend fun getLabel(userId: String, labelId: Int): Label
    suspend fun createLabel(label: Label): Long
    suspend fun updateLabel(label: Label): Int
    suspend fun deleteLabel(label: Label): Int // deletes the label entry in the label table only

    suspend fun getNotesForLabel(userId: String, labelId: Int): List<Note>
    suspend fun getLabelsForNote(userId: String, noteId: Int): List<Label>
    suspend fun addLabelInNote(userId: String, noteId: Int, labelId: Int): Long
    suspend fun removeLabelInNote(userId: String, noteId: Int, labelId: Int): Int

    // deletes the label from database and association b/w notes and label
    suspend fun deleteLabelAssociatedWithEachNote(userId: String, label: Label): Int
}