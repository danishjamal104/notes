package com.github.danishjamal104.notes.ui.fragment.note

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.View
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.github.danishjamal104.notes.R
import com.github.danishjamal104.notes.data.model.Note
import com.github.danishjamal104.notes.databinding.FragmentNoteBinding
import com.github.danishjamal104.notes.util.*
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class NoteFragment : Fragment(R.layout.fragment_note) {

    private lateinit var _binding: FragmentNoteBinding
    private val binding get() = _binding

    @SuppressLint("SimpleDateFormat")
    private val sdf = SimpleDateFormat("MMM dd, yyyy")
    private val date get() = sdf.format(Date())

    private val viewModel: NoteViewModel by viewModels()

    private var noteId: Int? = null
    private lateinit var note: Note

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
                return
            }
            viewModel.setEvent(NoteEvent.GetNote(it))
        } ?: enableButtons()

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
                viewModel.setEvent(NoteEvent.DeleteNote(note))
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
        val isNoteInitialised = this::note.isInitialized
        binding.note.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val newText = s.toString().trim().encodeToBase64()
                if(isNoteInitialised) {
                    if(newText == note.value) {
                        binding.saveButton.gone()
                    } else {
                        binding.saveButton.visible()
                    }
                } else {
                    if(newText.isEmpty()) {
                        binding.saveButton.gone()
                    } else {
                        binding.saveButton.visible()
                    }
                }
            }

        })
    }

    private fun createNote() {
        hideKeyboard()
        val text = binding.note.text.toString().trim()
        var title = binding.noteTitle.text.toString().trim()
        if(text.isEmpty()) {
            shortToast("Note can't be empty")
            return
        }
        if(text.isEmpty()) {
            shortToast("Note title is empty")
        }
        viewModel.setEvent(NoteEvent.CreateNote(text, title))
    }

    private fun updateNote(note: Note) {
        hideKeyboard()
        val newText = binding.note.text.toString().trim()
        val newTitle = binding.noteTitle.text.toString().trim()
        if(newText.encodeToBase64() == note.value && newTitle == note.title) {
            shortToast("No update")
            return
        }
        note.value = newText
        note.title = newTitle
        viewModel.setEvent(NoteEvent.UpdateNote(note))
    }

    private fun clearText() {
        binding.note.setText("")
    }

    private fun enableButtons() {
        binding.deleteButton.isClickable = true
        binding.saveButton.isClickable = true
    }

    @SuppressLint("NewApi")
    private fun handleFetchNoteSuccess(note: Note) {
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
        binding.deleteButton.isClickable = false
        binding.saveButton.isClickable = false
    }

    @SuppressLint("NewApi")
    private fun biometricPrompt() {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verify to see content")
            .setSubtitle("Only verified user are allowed to see the content")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()
        val executor = ContextCompat.getMainExecutor(requireContext())
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int,
                                                   errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    shortToast("Authentication error: $errString")
                    findNavController().navigate(R.id.action_noteFragment_to_homeFragment)
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    binding.note.setText(note.value.decodeFromBase64())
                    binding.noteTitle.setText(note.title)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    shortToast("Authentication failed")
                    findNavController().navigate(R.id.action_noteFragment_to_homeFragment)
                }
            })
        biometricPrompt.authenticate(promptInfo)
    }

}