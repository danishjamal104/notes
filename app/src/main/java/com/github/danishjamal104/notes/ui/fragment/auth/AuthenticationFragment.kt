package com.github.danishjamal104.notes.ui.fragment.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.github.danishjamal104.notes.R
import com.github.danishjamal104.notes.databinding.FragmentAuthenticationBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import dagger.hilt.android.AndroidEntryPoint
import com.google.android.gms.common.api.ApiException

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.github.danishjamal104.notes.util.Disable
import com.github.danishjamal104.notes.util.Enable
import com.github.danishjamal104.notes.util.LongToast
import com.github.danishjamal104.notes.util.Visible
import javax.inject.Inject

@AndroidEntryPoint
class AuthenticationFragment : Fragment(R.layout.fragment_authentication) {

    private lateinit var _binding: FragmentAuthenticationBinding
    private val binding get() = _binding

    private val viewModel: AuthViewModel by viewModels()

    private lateinit var signInIntentLauncher: ActivityResultLauncher<Intent>

    @Inject
    lateinit var googleSignInClient: GoogleSignInClient

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAuthenticationBinding.bind(view)

        binding.signInButton.setOnClickListener {
            login()
            //findNavController().navigate(R.id.action_authenticationFragment_to_homeFragment)
        }

        signInIntentLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if(it.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
                handleSignInResult(task)
            } else {
                Toast.makeText(requireContext(), "Invalid result code : ${it.resultCode}, data: ${it.data}", Toast.LENGTH_LONG).show()
            }
        }

        registerViewModelAuthState()
    }

    fun registerViewModelAuthState() {
        viewModel.authState.observe(viewLifecycleOwner) {
            when(it) {
                AuthState.Loading -> {
                    binding.progressBar.Visible()
                    binding.signInButton.Disable()
                }
                is AuthState.LogInFailure -> {
                    LongToast(it.reason)
                    binding.signInButton.Enable()
                }
                is AuthState.LogInSuccess -> {
                    LongToast("Welcome ${it.user.username}")
                    findNavController().navigate(R.id.action_authenticationFragment_to_homeFragment)
                }
            }
        }
    }

    fun login() {
        val signInIntent = googleSignInClient.signInIntent
        signInIntentLauncher.launch(signInIntent)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        viewModel.setEvent(AuthEvent.Login(completedTask))
    }

}