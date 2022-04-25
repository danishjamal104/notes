package com.github.danishjamal104.notes.ui.fragment.note

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.github.danishjamal104.notes.R
import com.github.danishjamal104.notes.data.model.Label
import com.github.danishjamal104.notes.data.model.Note
import com.github.danishjamal104.notes.databinding.FragmentNoteBinding
import com.github.danishjamal104.notes.ui.fragment.note.adapter.DialogAction
import com.github.danishjamal104.notes.ui.fragment.note.adapter.LabelAdapter
import com.github.danishjamal104.notes.ui.fragment.note.labelcomponent.LabelComponent
import com.github.danishjamal104.notes.util.*
import com.github.danishjamal104.notes.util.sharedpreference.EncryptionPreferences
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.concurrent.schedule

@AndroidEntryPoint
class NoteFragment : Fragment(R.layout.fragment_note), DialogAction {

    private lateinit var _binding: FragmentNoteBinding
    private val binding get() = _binding

    @SuppressLint("SimpleDateFormat")
    private val sdf = SimpleDateFormat("MMM dd, yyyy")
    private val date get() = sdf.format(Date())

    @Inject
    lateinit var encryptionPreferences: EncryptionPreferences

    @Inject
    lateinit var labelAdapter: LabelAdapter

    private val viewModel: NoteViewModel by viewModels()

    private var noteId: Int? = null
    private lateinit var note: Note

    private var isScreenLocked = false

    private lateinit var labelComponent: LabelComponent

    private var isNoteValueChanged = false
    private var isNoteTitleChanged = false
    private val hasDataChanged get() = isNoteTitleChanged || isNoteValueChanged
    private var noteHash: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNoteBinding.bind(view)

        noteId = arguments?.getInt(AppConstant.NOTE_ID_KEY, -1)

        setup()
        labelComponent = LabelComponent.bind(requireContext(), labelAdapter, this)

        binding.addLabel.setOnClickListener {
            if (!this::note.isInitialized) {
                return@setOnClickListener
            }
            viewModel.setLabelEvent(LabelEvent.GetAllLabel(note))
            labelComponent.show()
        }
    }

    private fun setup() {
        binding.date.text = date
        registerNoteState()
        registerLabelState()
        registerClickEvents()
        noteId?.let {
            if (it == -1) {
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
            when (it) {
                is NoteState.EventResult -> handleEventResult(it)
                NoteState.Loading -> handleLoading()
                is NoteState.GetNoteFailure -> longToast(it.reason)
                is NoteState.GetNoteSuccess -> handleFetchNoteSuccess(it.note)
            }
        }
    }

    private fun registerLabelState() {
        viewModel.labelState.observe(viewLifecycleOwner) {
            when (it) {
                is LabelState.Info -> longToast(it.info)
                is LabelState.GetLabelFailure -> longToast(it.reason)
                is LabelState.GetLabelSuccess -> {
                    shortToast("${it.labels.size} labels loaded")
                    updateLabels(it.labels)
                }
                is LabelState.CreateLabelResult -> {
                    if (it.success) {
                        labelAdapter.add(it.label!!)
                    } else {
                        longToast(it.reason!!)
                    }
                }
                is LabelState.GetAllLabelResult -> {
                    if (it.success) {
                        it.labels!!
                        shortToast("Adding ${it.labels.size} labels")
                        labelAdapter.setData(it.labels)
                    } else {
                        longToast(it.reason!!)
                    }
                }
                is LabelState.CheckStateUpdated -> labelAdapter.updateLabel(
                    it.label.id,
                    checked = it.isChecked
                )
                is LabelState.DeleteLabelResult -> {
                    if (!it.success) {
                        it.reason!!
                        longToast(it.reason)
                        return@observe
                    }
                    it.label!!
                    labelAdapter.deleteLabel(it.label.id)
                    labelComponent.releaseFocus()
                }
                is LabelState.UpdateLabelResult -> {
                    if (!it.success) {
                        it.reason!!
                        longToast(it.reason)
                        return@observe
                    }
                    it.label!!
                    labelAdapter.updateLabel(it.label.id, value = it.label.value)
                    labelComponent.releaseFocus()
                }
            }
        }
    }

    private fun registerClickEvents() {
        binding.deleteButton.setOnClickListener {
            if (this::note.isInitialized) {
                performActionThroughSecuredChannel {
                    viewModel.setEvent(NoteEvent.DeleteNote(note))
                }
            } else {
                clearText()
            }
        }
        binding.saveButton.setOnClickListener {
            if (this::note.isInitialized) {
                updateNote(note)
            } else {
                createNote()
            }
        }
        binding.backButton.setOnClickListener {
            val isExecuted = executeIfNoteIdIsNegativeOne {
                val title = binding.noteTitle.text.toString().trim()
                val noteValue = binding.note.text.toString().trim()
                if(title.isEmpty() && noteValue.isEmpty()) {
                    findNavController().navigate(R.id.action_noteFragment_to_homeFragment)
                    return@executeIfNoteIdIsNegativeOne true
                }
                false
            }
            if(!isExecuted && !hasDataChanged) {
                findNavController().navigate(R.id.action_noteFragment_to_homeFragment)
                return@setOnClickListener
            }
            if(!isExecuted) {
                requireContext().showDefaultMaterialAlert(
                    "Confirm",
                    "Do you want to discard your changes"
                ) {
                    findNavController().navigate(R.id.action_noteFragment_to_homeFragment)
                    hideKeyboard()
                }
            }
        }
        binding.unlockButton.setOnClickListener {
            performActionThroughSecuredChannel {
                unlockData()
            }
        }
        registerTextWatcher()
    }

    private fun registerTextWatcher() {
        val noteTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isScreenLocked) {
                    return
                }
                val newText = s.toString().trim()
                if (isNoteInitialised()) {
                    isNoteValueChanged = hasNoteTextChanged(newText)
                    if (hasDataChanged) {
                        binding.saveButton.visible()
                    } else {
                        binding.saveButton.gone()
                    }
                }
                executeIfNoteIdIsNegativeOne {
                    isNoteValueChanged = newText.isNotEmpty()
                    Log.i("hasDataChanged", "" + hasDataChanged)
                    if(hasDataChanged) {
                        binding.saveButton.visible()
                    } else {
                        binding.saveButton.gone()
                    }
                    true
                }
            }
        }
        val noteTitleTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isScreenLocked) {
                    return
                }
                val newTitle = s.toString().trim()
                if (isNoteInitialised()) {
                    isNoteTitleChanged = newTitle != note.title
                    if(hasDataChanged) {
                        binding.saveButton.visible()
                    } else {
                        binding.saveButton.gone()
                    }
                }
                executeIfNoteIdIsNegativeOne {
                    isNoteTitleChanged = newTitle.isNotEmpty()
                    Log.i("hasDataChanged", "" + hasDataChanged)
                    if(hasDataChanged) {
                        binding.saveButton.visible()
                    } else {
                        binding.saveButton.gone()
                    }
                    true
                }
            }

        }
        binding.note.addTextChangedListener(noteTextWatcher)
        binding.noteTitle.addTextChangedListener(noteTitleTextWatcher)
    }

    private fun hasNoteTextChanged(newText: String): Boolean {
        return newText.toSHA1() != noteHash
    }

    private fun executeIfNoteIdIsNegativeOne(block: () -> Boolean): Boolean {
         return noteId?.let {
            return if(it == -1) {
                block.invoke()
            } else {
                false
            }
        } ?: false
    }

    private fun createNote() {
        hideKeyboard()
        val text = binding.note.text.toString().trim()
        val title = binding.noteTitle.text.toString().trim()
        if (text.isEmpty()) {
            shortToast("Note can't be empty")
            return
        }
        if (title.isEmpty()) {
            shortToast("Note title can't be empty")
            return
        }
        viewModel.setEvent(NoteEvent.CreateNote(text, title))
    }

    private fun updateNote(
        note: Note,
        titleUpdate: Boolean = true,
        noteValueUpdate: Boolean = true
    ) {
        hideKeyboard()
        val newText = binding.note.text.toString().trim()
        val newTitle = binding.noteTitle.text.toString().trim()
        if (newText.encodeToBase64() == note.value && newTitle == note.title) {
            shortToast("No update")
            return
        }
        if (noteValueUpdate) {
            note.value = newText
        }
        if (titleUpdate) {
            note.title = newTitle
        }
        viewModel.setEvent(NoteEvent.UpdateNote(note))
    }

    // adds the chip in the chip group
    private fun updateLabels(labels: List<Label>) {
        labels.forEach {
            val chip = Chip(requireContext())
            chip.text = it.value
            chip.isCheckable = false
            chip.setChipBackgroundColorResource(R.color.chip_background)
            chip.setOnLongClickListener {
                binding.chipGroup.removeView(chip)
                return@setOnLongClickListener true
            }
            binding.chipGroup.addView(chip, 0)
        }
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
        viewModel.setLabelEvent(LabelEvent.GetLabel(note))
        binding.progressBar.hide()
        binding.addLabel.enable()
        enableButtons()
        this.note = note
        biometricPrompt()
    }

    private fun handleEventResult(event: NoteState.EventResult) {
        longToast(event.info)
        if (event.success) {
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
        val decryptedData = note.value.decrypt(encryptionPreferences.key)
        noteHash = decryptedData.toSHA1()
        setData(decryptedData, note.title)
    }

    private fun setData(noteData: String, noteTitle: String) {
        binding.note.setText(noteData)
        binding.noteTitle.setText(noteTitle)
    }

    private fun isNoteInitialised(): Boolean {
        return this::note.isInitialized
    }

    override fun createLabel(labelName: String) {
        labelComponent.releaseFocus()
        viewModel.setLabelEvent(LabelEvent.CreateLabel(note, labelName))
    }

    override fun deleteLabel(label: Label) {
        viewModel.setLabelEvent(LabelEvent.DeleteLabel(label))
    }

    override fun updateLabelName(oldLabel: Label, newLabelName: String) {
        viewModel.setLabelEvent(LabelEvent.UpdateLabel(oldLabel, newLabelName))
    }

    override fun updateLabelCheck(oldLabel: Label, checked: Boolean) {
        if (checked) {
            viewModel.setLabelEvent(LabelEvent.AddLabelInNote(oldLabel, note))
        } else {
            viewModel.setLabelEvent(LabelEvent.RemoveLabelFromNote(oldLabel, note))
        }
    }

}