package com.github.danishjamal104.notes.ui.fragment.note

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.github.danishjamal104.notes.R
import com.github.danishjamal104.notes.data.model.Note
import com.github.danishjamal104.notes.databinding.FragmentNoteBinding
import com.github.danishjamal104.notes.util.*
import com.github.danishjamal104.notes.util.sharedpreference.EncryptionPreferences
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.concurrent.schedule

@AndroidEntryPoint
class NoteFragment : Fragment(R.layout.fragment_note) {

    private lateinit var _binding: FragmentNoteBinding
    private val binding get() = _binding

    @SuppressLint("SimpleDateFormat")
    private val sdf = SimpleDateFormat("MMM dd, yyyy")
    private val date get() = sdf.format(Date())

    @Inject
    lateinit var encryptionPreferences: EncryptionPreferences

    private val viewModel: NoteViewModel by viewModels()

    private var noteId: Int? = null
    private lateinit var note: Note

    private var isScreenLocked = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNoteBinding.bind(view)

        noteId = arguments?.getInt(AppConstant.NOTE_ID_KEY, -1)

        setup()
    }

    private fun setup() {
        binding.date.text = date
        registerNoteState()
        registerClickEvents()
        noteId?.let {
            if(it==-1) {
                enableButtons()
                binding.lockScreenContainer.gone()
                return
            }
            viewModel.setEvent(NoteEvent.GetNote(it))
        } ?: enableButtons()

    }

    private fun setupScreenTimeout() {
        Timer("", false).schedule(12000) {
            try {
                requireActivity().runOnUiThread { lockData() }
            } catch (_: Exception) {
                this.cancel()
            }
        }
    }

    private fun registerNoteState() {
        viewModel.noteSate.observe(viewLifecycleOwner) {
            when(it) {
                is NoteState.EventResult -> handleEventResult(it)
                NoteState.Loading -> handleLoading()
                is NoteState.GetNoteFailure -> longToast(it.reason)
                is NoteState.GetNoteSuccess -> handleFetchNoteSuccess(it.note)
            }
        }
    }

    private fun registerClickEvents() {
        binding.deleteButton.setOnClickListener {
            if(this::note.isInitialized) {
                performActionThroughSecuredChannel {
                    viewModel.setEvent(NoteEvent.DeleteNote(note))
                }
            } else {
                clearText()
            }
        }
        binding.saveButton.setOnClickListener {
            if(this::note.isInitialized) {
                updateNote(note)
            } else {
                createNote()
            }
        }
        binding.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_noteFragment_to_homeFragment)
        }
        binding.unlockButton.setOnClickListener {
            performActionThroughSecuredChannel {
                unlockData()
            }
        }
        registerTextWatcher()
    }

    private fun registerTextWatcher() {
        var isNoteValueChanged = false
        var isNoteTitleChanged = false
        val noteTextWatcher = object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if(isScreenLocked) { return }
                val newText = s.toString().trim().encodeToBase64()
                if(isNoteInitialised()) {
                    isNoteValueChanged = if(newText == note.value && !isNoteTitleChanged) {
                        binding.saveButton.gone()
                        false
                    } else {
                        binding.saveButton.visible()
                        true
                    }
                }
                noteId?.let {
                    if(it == -1) {
                        if(newText.isEmpty()) {
                            binding.saveButton.gone()
                        } else {
                            binding.saveButton.visible()
                        }
                    }
                }
            }
        }
        val noteTitleTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if(isScreenLocked) { return }
                val newTitle = s.toString().trim()
                if(isNoteInitialised()) {
                    isNoteTitleChanged = if(newTitle == note.title && !isNoteValueChanged) {
                        binding.saveButton.gone()
                        false
                    } else {
                        binding.saveButton.visible()
                        true
                    }
                }
            }

        }
        binding.note.addTextChangedListener(noteTextWatcher)
        binding.noteTitle.addTextChangedListener(noteTitleTextWatcher)
    }

    private fun createNote() {
        hideKeyboard()
        val text = binding.note.text.toString().trim()
        val title = binding.noteTitle.text.toString().trim()
        if(text.isEmpty()) {
            shortToast("Note can't be empty")
            return
        }
        if(title.isEmpty()) {
            shortToast("Note title can't be empty")
            return
        }
        viewModel.setEvent(NoteEvent.CreateNote(text, title))
    }

    private fun updateNote(note: Note, titleUpdate: Boolean = true, noteValueUpdate: Boolean = true) {
        hideKeyboard()
        val newText = binding.note.text.toString().trim()
        val newTitle = binding.noteTitle.text.toString().trim()
        if(newText.encodeToBase64() == note.value && newTitle == note.title) {
            shortToast("No update")
            return
        }
        if (noteValueUpdate) { note.value = newText }
        if (titleUpdate) { note.title = newTitle }
        viewModel.setEvent(NoteEvent.UpdateNote(note))
    }

    private fun clearText() {
        binding.note.setText("")
    }

    private fun enableButtons() {
        binding.deleteButton.isClickable = true
        binding.saveButton.isClickable = true
    }

    private fun disableButtons() {
        binding.deleteButton.isClickable = false
        binding.saveButton.isClickable = false
    }

    private fun disableEditableFields() {
        binding.note.disable()
        binding.noteTitle.disable()
    }

    private fun enableEditableFields() {
        binding.note.enable()
        binding.noteTitle.enable()
    }

    private fun handleFetchNoteSuccess(note: Note) {
        binding.progressBar.hide()
        enableButtons()
        this.note = note
        biometricPrompt()
    }

    private fun handleEventResult(event: NoteState.EventResult) {
        longToast(event.info)
        if(event.success) {
            findNavController().navigate(R.id.action_noteFragment_to_homeFragment)
        }
    }

    private fun handleLoading() {
        binding.progressBar.show()
        disableButtons()
    }

    private fun biometricPrompt() {
        performActionThroughSecuredChannel({
            shortToast("Authentication error: $it")
            findNavController().navigate(R.id.action_noteFragment_to_homeFragment)
        }, {
            unlockData()
        }, {
            shortToast("Authentication failed")
            findNavController().navigate(R.id.action_noteFragment_to_homeFragment)
        })
    }

    // this method replaces the original text with the encoded text on screen
    private fun lockData() {
        isScreenLocked = true
        binding.saveButton.gone()
        binding.lockScreenContainer.visible()
        disableEditableFields()
        setData(note.value, note.title)
    }

    // this method replaces the encoded text with the original text on screen
    private fun unlockData() {
        setupScreenTimeout()
        isScreenLocked = false
        binding.lockScreenContainer.gone()
        enableEditableFields()
        setData(note.value.decrypt(encryptionPreferences.key), note.title)
    }

    private fun setData(noteData: String, noteTitle: String) {
        binding.note.setText(noteData)
        binding.noteTitle.setText(noteTitle)
    }

    private fun isNoteInitialised(): Boolean {
        return this::note.isInitialized
    }

}