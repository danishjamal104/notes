package com.github.danishjamal104.notes.data.repository.label

import com.github.danishjamal104.notes.data.local.CacheDataSource
import com.github.danishjamal104.notes.data.model.Label
import com.github.danishjamal104.notes.data.model.Note
import com.github.danishjamal104.notes.util.ServiceResult
import com.github.danishjamal104.notes.util.sharedpreference.UserPreferences
import java.lang.Exception

class LabelRepositoryImpl
constructor(
    private val cacheDataSource: CacheDataSource,
    private val userPreferences: UserPreferences
): LabelRepository {

    private val userId get() = userPreferences.getUserId()

    override suspend fun createLabel(labelName: String): ServiceResult<Label> {
        val label = Label(-1, userId, labelName, false)
        return try {
            val labelId = cacheDataSource.createLabel(label)
            ServiceResult.Success(cacheDataSource.getLabel(userId, labelId.toInt()))
        } catch (e: Exception) {
            ServiceResult.Error(""+e.localizedMessage)
        }
    }

    override suspend fun deleteLabel(label: Label): ServiceResult<Label> {
        return try {
            when (cacheDataSource.deleteLabelAssociatedWithEachNote(userId, label)) {
                in 1..Int.MAX_VALUE -> ServiceResult.Success(label)
                else -> ServiceResult.Error("Deletion failed")
            }
        } catch (e: Exception) {
            ServiceResult.Error(""+e.localizedMessage)
        }
    }

    override suspend fun updateLabel(label: Label): ServiceResult<Label> {
        return try {
            when(cacheDataSource.updateLabel(label)) {
                in 0..Int.MAX_VALUE -> ServiceResult.Success(label)
                else -> ServiceResult.Error("Update failed")
            }
        } catch (e: Exception) {
            ServiceResult.Error(""+e.localizedMessage)
        }
    }

    override suspend fun fetchAllLabel(): ServiceResult<List<Label>> {
        return try {
            val result = cacheDataSource.getLabels(userId)
            ServiceResult.Success(result)
        } catch (e: Exception) {
            ServiceResult.Error(""+e.localizedMessage)
        }
    }

    override suspend fun fetchAllLabel(note: Note): ServiceResult<List<Label>> {
        var noteLabel: List<Label>? = null
        when(val res = getLabelOfNote(note)) {
            is ServiceResult.Error -> {
                return ServiceResult.Error("Unable to fetch label for this note")
            }
            is ServiceResult.Success -> {
                noteLabel = res.data
            }
        }
        return try {
            val allLabels = cacheDataSource.getLabels(userId)
            allLabels.forEach { allLabel ->
                val match = noteLabel.find { noteLabel ->
                    noteLabel.id == allLabel.id
                }
                match?.let {
                    allLabel.checked = true
                }
            }
            ServiceResult.Success(allLabels)
        } catch (e: Exception) {
            ServiceResult.Error(""+e.localizedMessage)
        }
    }

    override suspend fun getLabelOfNote(note: Note): ServiceResult<List<Label>> {
        return try {
            val result = cacheDataSource.getLabelsForNote(userId, note.id)
            ServiceResult.Success(result)
        } catch (e: Exception) {
            ServiceResult.Error(""+e.localizedMessage)
        }
    }

    override suspend fun addLabelInNote(note: Note, label: Label): ServiceResult<Unit> {
       return try {
           cacheDataSource.addLabelInNote(userId, note.id, label.id)
           ServiceResult.Success(Unit)
       } catch (e: Exception) {
           ServiceResult.Error(""+e.localizedMessage)
       }
    }

    override suspend fun removeLabelFromNote(note: Note, label: Label): ServiceResult<Unit> {
        return try {
            when (cacheDataSource.removeLabelInNote(userId, note.id, label.id)) {
                in 0..Int.MAX_VALUE -> ServiceResult.Success(Unit)
                else -> ServiceResult.Error("Deletion failed")
            }
        } catch (e: Exception) {
            ServiceResult.Error(""+e.localizedMessage)
        }
    }
}