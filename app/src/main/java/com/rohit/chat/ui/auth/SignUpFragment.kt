package com.rohit.chat.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.rohit.chat.MainActivity
import com.rohit.chat.databinding.FragmentSignUpBinding
import com.rohit.chat.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.etUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val username = s.toString().trim()
                if (username.isEmpty()) {
                    binding.tilUsername.error = null
                    binding.tilUsername.helperText = null
                    return
                }
                viewModel.checkUsername(username)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnSignUp.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                binding.tilConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            if (binding.tilUsername.error != null) {
                Toast.makeText(requireContext(), "Fix User ID error", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.signUp(name, username, password)
        }

        binding.tvLogin.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.usernameAvailable.collectLatest { resource ->
                when (resource) {
                    is Resource.Success -> {
                        if (resource.data == true) {
                            binding.tilUsername.error = null
                            binding.tilUsername.helperText = "✅ User ID Available"
                        } else {
                            binding.tilUsername.helperText = null
                            binding.tilUsername.error = "❌ User ID already exists"
                        }
                    }
                    is Resource.Error -> {
                        binding.tilUsername.helperText = null
                        binding.tilUsername.error = resource.message
                    }
                    else -> {
                        binding.tilUsername.error = null
                        binding.tilUsername.helperText = null
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.authState.collectLatest { resource ->
                when (resource) {
                    is Resource.Success -> {
                        binding.progressBar.isVisible = false
                        binding.btnSignUp.isEnabled = true
                        startActivity(Intent(requireContext(), MainActivity::class.java))
                        requireActivity().finish()
                    }
                    is Resource.Error -> {
                        binding.progressBar.isVisible = false
                        binding.btnSignUp.isEnabled = true
                        Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                    }
                    is Resource.Loading -> {
                        binding.progressBar.isVisible = true
                        binding.btnSignUp.isEnabled = false
                    }
                    null -> {}
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
