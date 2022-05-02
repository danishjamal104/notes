package com.github.danishjamal104.notes.ui.fragment.home

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.work.WorkManager
import com.github.danishjamal104.notes.R
import com.github.danishjamal104.notes.data.model.Label
import com.github.danishjamal104.notes.data.model.Note
import com.github.danishjamal104.notes.databinding.FragmentHomeBinding
import com.github.danishjamal104.notes.ui.fragment.home.adapter.ItemClickListener
import com.github.danishjamal104.notes.ui.fragment.home.adapter.NotesAdapter
import com.github.danishjamal104.notes.ui.main.MainActivity
import com.github.danishjamal104.notes.util.*
import com.github.danishjamal104.notes.util.sharedpreference.UserPreferences
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home), ItemClickListener<Note> {

    private lateinit var _binding: FragmentHomeBinding
    private val binding get() = _binding

    private val viewModel: HomeViewModel by viewModels()

    @Inject
    lateinit var googleSignInClient: GoogleSignInClient

    @Inject
    lateinit var preferences: UserPreferences

    @Inject
    lateinit var adapter: NotesAdapter

    @Inject
    lateinit var workManager: WorkManager

    private val labels: MutableList<Label> = mutableListOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        adapter.clearAll()
        if (!preferences.isAuthenticated()) {
            findNavController().navigate(R.id.action_homeFragment_to_authenticationFragment)
        } else {
            setup()
            viewModel.setEvent(HomeEvent.GetNotes)
        }

    }

    private fun setup() {
        setupRecyclerView()
        registerHomeState()
        registerClickEvents()
    }

    private fun setupRecyclerView() {
        adapter.emptyView = binding.illustration
        binding.noteList.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.noteList.setHasFixedSize(false)
        binding.noteList.adapter = adapter

        adapter.itemClickListener = this
    }

    private fun registerHomeState() {
        viewModel.authState.observe(viewLifecycleOwner) {
            when (it) {
                is HomeState.GetNotesFailure -> handleFailure(it.reason)
                is HomeState.GetNotesSuccess -> handleSuccess(it.notes)
                HomeState.Loading -> handleLoading()
                is HomeState.GetLabelResult -> handleGetLabelResult(it)
                is HomeState.GetNotesByLabelResult -> handleGetNotesByLabelResult(it)
            }
        }
    }

    private fun registerClickEvents() {
        binding.addNote.setOnClickListener {
            (requireActivity() as MainActivity).resetMotionLayout()
            findNavController().navigate(R.id.action_homeFragment_to_noteFragment)
        }
        binding.actionButton.setOnClickListener {
            (requireActivity() as MainActivity).resetMotionLayout()
            val title = getString(R.string.confirm)
            val message = getString(R.string.logout_alert_message)
            requireContext().showDefaultMaterialAlert(title, message) {
                logOut()
            }
        }
        binding.swipeRefresh.setOnRefreshListener {
            if (binding.clearFilter.isVisible) {
                updateNoteList()
                return@setOnRefreshListener
            }
            viewModel.setEvent(HomeEvent.GetNotes)
        }
        binding.filterButton.setOnClickListener {
            adapter.createBackup()
            viewModel.setEvent(HomeEvent.GetLabels)
            binding.filterButton.isClickable = false
        }
        binding.clearFilter.setOnClickListener {
            adapter.restore()
            hideFilterLayout()
            binding.filterButton.isClickable = true
        }
    }

    private fun logOut() {
        googleSignInClient.signOut().addOnCompleteListener {
            preferences.revokeAuthentication()
            findNavController().navigate(R.id.action_homeFragment_to_authenticationFragment)
        }
    }

    private fun handleSuccess(notes: List<Note>) {
        if (binding.swipeRefresh.isRefreshing) {
            adapter.clearAll()
        }
        adapter.addNotes(notes)
        hideProgress()
    }

    private fun handleFailure(reason: String) {
        longToast(reason)
        hideProgress()
    }

    private fun handleGetNotesByLabelResult(state: HomeState.GetNotesByLabelResult) {
        hideProgress()
        if (state.success) {
            adapter.clearAll()
            state.notes?.let { adapter.addNotes(it) }
        } else {
            longToast("Unable to apply filter")
        }
    }

    private fun handleGetLabelResult(state: HomeState.GetLabelResult) {
        if (state.success) {
            labels.clear()
            state.labels!!.forEach {
                labels.add(it)
            }
            loadLabels()
        } else {
            longToast(state.reason!!)
        }
        hideProgress()
        //updateNoteList()
    }

    private fun showProgress() {
        binding.linearProgress.visible()
    }

    private fun hideProgress() {
        binding.linearProgress.invisible()
        binding.swipeRefresh.isRefreshing = false
    }

    private fun handleLoading() {
        showProgress()
    }

    private fun loadLabels() {
        if (labels.size == 0) {
            longToast(getString(R.string.no_label_filter_info_text))
            hideFilterLayout()
            adapter.restore()
            binding.filterButton.isClickable = true
            return
        }
        updateLabels(labels)
        showFilterLayout()

    }

    private fun showFilterLayout() {
        binding.tagContainer.visible()
        binding.clearFilter.visible()
        binding.emptyListText.text = requireContext().getString(R.string.empty_filter_text)
        binding.emptyListIllustration.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.ic_empty_illustration
            )
        )
    }

    private fun hideFilterLayout() {
        binding.tagContainer.gone()
        binding.clearFilter.gone()
        binding.emptyListText.text = requireContext().getString(R.string.empty_notes_list_text)
        binding.emptyListIllustration.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.ic_no_note_illustration
            )
        )
    }

    private fun updateLabels(labels: List<Label>) {
        binding.chipGroup.removeAllViews()
        labels.forEach {
            val chip = Chip(requireContext())
            chip.text = it.value
            chip.isCheckable = true
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.always_white))
            chip.setChipBackgroundColorResource(R.color.slight_primary)
            binding.chipGroup.addView(chip)

            chip.setOnCheckedChangeListener { _, checked ->
                it.checked = checked
                updateNoteList()
            }
        }
    }

    /**
     * this function will take the latest [labels], and apply db query and update adapter list
     * apply filter login in [HomeFragment] to filter the note list
     */
    private fun updateNoteList() {
        val selected = mutableListOf<Label>()
        labels.forEach {
            if (it.checked) {
                selected.add(it)
            }
        }
        viewModel.setEvent(HomeEvent.GetNotesByLabel(selected))
        prettyLogger()
    }

    private fun prettyLogger() {
        val sb = StringBuilder()
        labels.forEach {
            if (it.checked) {
                sb.append("! ")
            }
            sb.append("${it.value}\n")
        }
        Log.i("pretty", sb.toString())
    }

    override fun onItemClicked(item: Note, position: Int, view: View) {
        findNavController().navigate(
            R.id.action_homeFragment_to_noteFragment,
            bundleOf(AppConstant.NOTE_ID_KEY to item.id)
        )
    }

}