package com.github.danishjamal104.notes.data.repository.label

import com.github.danishjamal104.notes.data.model.Label
import com.github.danishjamal104.notes.data.model.Note
import com.github.danishjamal104.notes.util.ServiceResult

interface LabelRepository {
    suspend fun createLabel(labelName: String): ServiceResult<Label>
    suspend fun deleteLabel(label: Label): ServiceResult<Label>
    suspend fun updateLabel(label: Label): ServiceResult<Label>
    suspend fun fetchAllLabel(): ServiceResult<List<Label>>

    suspend fun fetchAllLabel(note: Note): ServiceResult<List<Label>>
    suspend fun getLabelOfNote(note: Note): ServiceResult<List<Label>>
    suspend fun addLabelInNote(note: Note, label: Label): ServiceResult<Unit>
    suspend fun removeLabelFromNote(note: Note, label: Label): ServiceResult<Unit>
}