package com.github.danishjamal104.notes.ui.fragment.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.github.danishjamal104.notes.R
import com.github.danishjamal104.notes.databinding.FragmentAuthenticationBinding
import com.github.danishjamal104.notes.databinding.FragmentHomeBinding
import com.github.danishjamal104.notes.util.sharedpreference.UserPreferences
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var _binding: FragmentHomeBinding
    private val binding get() = _binding

    @Inject
    lateinit var preferences: UserPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)


        if(!preferences.isAuthenticated()) {
            findNavController().navigate(R.id.action_homeFragment_to_authenticationFragment)
        }
    }


}