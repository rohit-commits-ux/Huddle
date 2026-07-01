package com.rohit.chat.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.rohit.chat.MainActivity
import com.rohit.chat.R
import com.rohit.chat.databinding.FragmentPhoneAuthBinding
import com.rohit.chat.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class PhoneAuthFragment : Fragment() {

    private var _binding: FragmentPhoneAuthBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()
    private var verificationId: String? = null
    private var forceResendingToken: PhoneAuthProvider.ForceResendingToken? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPhoneAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnVerify.setOnClickListener {
            if (verificationId == null) {
                val phoneNumber = binding.etPhone.text.toString()
                if (phoneNumber.isNotEmpty()) {
                    startPhoneNumberVerification(phoneNumber)
                } else {
                    Toast.makeText(requireContext(), "Enter phone number", Toast.LENGTH_SHORT).show()
                }
            } else {
                val otp = binding.etOtp.text.toString()
                if (otp.isNotEmpty()) {
                    val credential = PhoneAuthProvider.getCredential(verificationId!!, otp)
                    viewModel.loginWithCredential(credential)
                } else {
                    Toast.makeText(requireContext(), "Enter OTP", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authState.collectLatest { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            binding.progressBar.visibility = View.GONE
                            // After login, check if user profile exists or go to profile setup
                            // For simplicity, we'll go to a "Setup Profile" fragment if it's a new user
                            // or MainActivity if it's an existing one.
                            // For now, let's assume we need to setup profile.
                            findNavController().navigate(R.id.action_phoneAuthFragment_to_setupProfileFragment)
                        }
                        is Resource.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.btnVerify.isEnabled = true
                            Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                        }
                        is Resource.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.btnVerify.isEnabled = false
                        }
                        null -> {}
                    }
                }
            }
        }
    }

    private fun startPhoneNumberVerification(phoneNumber: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnVerify.isEnabled = false

        val options = PhoneAuthOptions.newBuilder(com.google.firebase.auth.FirebaseAuth.getInstance())
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    binding.progressBar.visibility = View.GONE
                    viewModel.loginWithCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnVerify.isEnabled = true
                    Toast.makeText(requireContext(), e.localizedMessage, Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(verId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnVerify.isEnabled = true
                    verificationId = verId
                    forceResendingToken = token
                    
                    binding.tilPhone.visibility = View.GONE
                    binding.tilOtp.visibility = View.VISIBLE
                    binding.btnVerify.text = "Verify OTP"
                    binding.tvDescription.text = "OTP sent to $phoneNumber"
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}