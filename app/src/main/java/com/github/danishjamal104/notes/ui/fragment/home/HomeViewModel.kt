package com.github.danishjamal104.notes.ui.fragment.home

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
class HomeViewModel
@Inject constructor(
    private val notesRepository: NotesRepository,
    private val labelRepository: LabelRepository
) : ViewModel(), Event<HomeEvent> {

    private val _homeState: MutableLiveData<HomeState> = MutableLiveData()
    val authState: LiveData<HomeState> get() = _homeState

    override fun setEvent(event: HomeEvent) {
        viewModelScope.launch {
            when (event) {
                HomeEvent.GetNotes -> getNotes()
                HomeEvent.GetLabels -> getLabels()
                is HomeEvent.GetNotesByLabel -> getNotesByLabel(event.labels)
            }
        }

    }

    private suspend fun getNotes() {
        _homeState.value = HomeState.Loading
        when (val result = notesRepository.getNotes()) {
            is ServiceResult.Error -> _homeState.postValue(HomeState.GetNotesFailure(result.reason))
            is ServiceResult.Success -> _homeState.postValue(HomeState.GetNotesSuccess(result.data))
        }
    }

    private suspend fun getLabels() {
        _homeState.value = HomeState.Loading
        when (val result = labelRepository.fetchAllLabel()) {
            is ServiceResult.Error -> _homeState.postValue(
                HomeState.GetLabelResult(
                    false,
                    reason = result.reason
                )
            )
            is ServiceResult.Success -> _homeState.postValue(
                HomeState.GetLabelResult(
                    true,
                    labels = result.data
                )
            )
        }
    }

    private suspend fun getNotesByLabel(labels: List<Label>) {
        _homeState.value = HomeState.Loading
        when (val result = notesRepository.getNoteOfLabels(labels)) {
            is ServiceResult.Error -> _homeState.postValue(
                HomeState.GetNotesByLabelResult(
                    false,
                    reason = result.reason
                )
            )
            is ServiceResult.Success -> _homeState.postValue(
                HomeState.GetNotesByLabelResult(
                    true,
                    notes = result.data
                )
            )
        }
    }

}

sealed class HomeState {
    data class GetNotesSuccess(val notes: List<Note>) : HomeState()
    data class GetNotesFailure(val reason: String) : HomeState()
    data class GetLabelResult(
        val success: Boolean,
        val labels: List<Label>? = null,
        val reason: String? = null
    ) : HomeState()

    data class GetNotesByLabelResult(
        val success: Boolean,
        val notes: List<Note>? = null,
        val reason: String? = null
    ) : HomeState()

    object Loading : HomeState()
}

sealed class HomeEvent {
    object GetNotes : HomeEvent()
    object GetLabels : HomeEvent()
    data class GetNotesByLabel(val labels: List<Label>) : HomeEvent()
}