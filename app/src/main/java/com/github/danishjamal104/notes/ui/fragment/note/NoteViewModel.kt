package com.github.danishjamal104.notes.ui.fragment.note

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.danishjamal104.notes.data.model.Label
import com.github.danishjamal104.notes.data.model.Note
import com.github.danishjamal104.notes.data.repository.label.LabelRepository
import com.github.danishjamal104.notes.data.repository.note.NotesRepository
import com.github.danishjamal104.notes.ui.Event
import com.github.danishjamal104.notes.util.ServiceResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteViewModel
@Inject constructor(
    private val notesRepository: NotesRepository,
    private val labelRepository: LabelRepository
) : ViewModel(), Event<NoteEvent> {

    private val _noteState: MutableLiveData<NoteState> = MutableLiveData()
    val noteSate: LiveData<NoteState> get() = _noteState

    private val _labelState: MutableLiveData<LabelState> = MutableLiveData()
    val labelState: LiveData<LabelState> get() = _labelState

    override fun setEvent(event: NoteEvent) {
        viewModelScope.launch {
            when (event) {
                is NoteEvent.CreateNote -> createNote(event.note, event.title)
                is NoteEvent.DeleteNote -> deleteNote(event.note)
                is NoteEvent.UpdateNote -> updateNote(event.note)
                is NoteEvent.GetNote -> getNote(event.noteId)
            }
        }
    }

    private suspend fun getNote(noteId: Int) {
        _noteState.value = NoteState.Loading
        when (val result = notesRepository.getNote(noteId)) {
            is ServiceResult.Success -> {
                _noteState.postValue(NoteState.GetNoteSuccess(result.data))
            }
            is ServiceResult.Error ->
                _noteState.postValue(NoteState.GetNoteFailure(result.reason))
        }
    }


    private suspend fun createNote(note: String, title: String? = null) {
        _noteState.value = NoteState.Loading
        when (val result = notesRepository.createNote(note, title)) {
            is ServiceResult.Success -> {
                _noteState.postValue(
                    NoteState.EventResult(
                        true,
                        "Note created successfully"
                    )
                )
            }
            is ServiceResult.Error ->
                _noteState.postValue(NoteState.EventResult(false, result.reason))
        }
    }

    private suspend fun updateNote(note: Note) {
        _noteState.value = NoteState.Loading
        when (val result = notesRepository.updateNote(note)) {
            is ServiceResult.Success -> {
                _noteState.postValue(
                    NoteState.EventResult(
                        true,
                        "Note updated successfully"
                    )
                )
            }
            is ServiceResult.Error ->
                _noteState.postValue(NoteState.EventResult(false, result.reason))
        }
    }

    private suspend fun deleteNote(note: Note) {
        _noteState.value = NoteState.Loading
        when (val result = notesRepository.deleteNote(note)) {
            is ServiceResult.Success -> {
                _noteState.postValue(
                    NoteState.EventResult(
                        true,
                        "Note deleted successfully"
                    )
                )
            }
            is ServiceResult.Error ->
                _noteState.postValue(NoteState.EventResult(false, result.reason))
        }
    }

    // label events starts from here

    fun setLabelEvent(event: LabelEvent) {
        viewModelScope.launch {
            when (event) {
                is LabelEvent.GetLabel -> fetchLabel(event.note)
                is LabelEvent.CreateLabel -> {
                    createLabel(event.note, event.labelName)
                }
                is LabelEvent.GetAllLabel -> {
                    fetchAllLabel(event.note)
                }
                is LabelEvent.AddLabelInNote -> addLabelInNote(event.label, event.note)
                is LabelEvent.DeleteLabel -> deleteLabel(event.label)
                is LabelEvent.RemoveLabelFromNote -> removeLabelFromNote(event.label, event.note)
                is LabelEvent.UpdateLabel -> updateLabel(event.label, event.newLabelName)
            }
        }
    }

    private suspend fun fetchLabel(note: Note) {
        when (val result = labelRepository.getLabelOfNote(note)) {
            is ServiceResult.Error -> _labelState.postValue(LabelState.GetLabelFailure(result.reason))
            is ServiceResult.Success -> _labelState.postValue(LabelState.GetLabelSuccess(result.data))
        }
    }

    private suspend fun fetchAllLabel(note: Note) {
        when (val result = labelRepository.fetchAllLabel(note)) {
            is ServiceResult.Error -> _labelState.postValue(
                LabelState.GetAllLabelResult(
                    false,
                    null,
                    result.reason
                )
            )
            is ServiceResult.Success -> _labelState.postValue(
                LabelState.GetAllLabelResult(
                    true,
                    result.data
                )
            )
        }
    }

    private suspend fun createLabel(note: Note, labelName: String) {
        var label: Label? = null
        when (val result = labelRepository.createLabel(labelName)) {
            is ServiceResult.Error -> {
                _labelState.postValue(
                    LabelState.CreateLabelResult(
                        false,
                        null,
                        "Failed to create label $labelName. " + result.reason
                    )
                )
                return
            }
            is ServiceResult.Success -> {
                label = result.data
            }
        }
        when (val result = labelRepository.addLabelInNote(note, label)) {
            is ServiceResult.Error -> {
                _labelState.postValue(
                    LabelState.CreateLabelResult(
                        false,
                        null,
                        "Failed to add label $labelName. " + result.reason
                    )
                )
                return
            }
            is ServiceResult.Success -> {
                label.checked = true
                _labelState.postValue(LabelState.CreateLabelResult(true, label))
            }
        }
    }

    private suspend fun addLabelInNote(label: Label, note: Note) {
        when (val result = labelRepository.addLabelInNote(note, label)) {
            is ServiceResult.Error -> _labelState.postValue(LabelState.Info("Unable to add label ${label.value}. ${result.reason}"))
            is ServiceResult.Success -> _labelState.postValue(
                LabelState.CheckStateUpdated(
                    label,
                    true
                )
            )
        }
    }

    private suspend fun removeLabelFromNote(label: Label, note: Note) {
        when (val result = labelRepository.removeLabelFromNote(note, label)) {
            is ServiceResult.Error -> _labelState.postValue(LabelState.Info("Unable to remove label ${label.value}. ${result.reason}"))
            is ServiceResult.Success -> _labelState.postValue(
                LabelState.CheckStateUpdated(
                    label,
                    false
                )
            )
        }
    }

    private suspend fun deleteLabel(label: Label) {
        when (val result = labelRepository.deleteLabel(label)) {
            is ServiceResult.Error -> _labelState.postValue(
                LabelState.DeleteLabelResult(
                    false,
                    null,
                    result.reason
                )
            )
            is ServiceResult.Success -> _labelState.postValue(
                LabelState.DeleteLabelResult(
                    true,
                    result.data
                )
            )
        }
    }

    private suspend fun updateLabel(label: Label, newLabelName: String) {
        label.value = newLabelName
        when (val result = labelRepository.updateLabel(label)) {
            is ServiceResult.Error -> _labelState.postValue(
                LabelState.UpdateLabelResult(
                    false,
                    null,
                    result.reason
                )
            )
            is ServiceResult.Success -> _labelState.postValue(
                LabelState.UpdateLabelResult(
                    true,
                    result.data
                )
            )
        }
    }

}

sealed class LabelState {
    data class Info(val info: String) : LabelState()
    data class GetLabelSuccess(val labels: List<Label>) : LabelState()
    data class GetLabelFailure(val reason: String) : LabelState()
    data class GetAllLabelResult(
        val success: Boolean,
        val labels: List<Label>? = null,
        val reason: String? = ""
    ) : LabelState()

    data class CreateLabelResult(
        val success: Boolean,
        val label: Label? = null,
        val reason: String? = ""
    ) : LabelState()

    data class DeleteLabelResult(
        val success: Boolean,
        val label: Label? = null,
        val reason: String? = ""
    ) : LabelState()

    data class UpdateLabelResult(
        val success: Boolean,
        val label: Label? = null,
        val reason: String? = ""
    ) : LabelState()

    data class CheckStateUpdated(val label: Label, val isChecked: Boolean) : LabelState()
}

sealed class LabelEvent {
    data class GetLabel(val note: Note) : LabelEvent()
    data class GetAllLabel(val note: Note) : LabelEvent()
    data class CreateLabel(val note: Note, val labelName: String) : LabelEvent()
    data class UpdateLabel(val label: Label, val newLabelName: String) : LabelEvent()
    data class DeleteLabel(val label: Label) : LabelEvent()
    data class AddLabelInNote(val label: Label, val note: Note) : LabelEvent()
    data class RemoveLabelFromNote(val label: Label, val note: Note) : LabelEvent()
}

sealed class NoteState {
    data class GetNoteSuccess(val note: Note) : NoteState()
    data class GetNoteFailure(val reason: String) : NoteState()
    data class EventResult(val success: Boolean, val info: String) : NoteState()
    object Loading : NoteState()
}

sealed class NoteEvent {
    data class GetNote(val noteId: Int) : NoteEvent()
    data class CreateNote(val note: String, val title: String? = null) : NoteEvent()
    data class UpdateNote(val note: Note) : NoteEvent()
    data class DeleteNote(val note: Note) : NoteEvent()
}