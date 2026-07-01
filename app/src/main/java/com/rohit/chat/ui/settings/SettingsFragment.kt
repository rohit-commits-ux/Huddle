package com.rohit.chat.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.google.firebase.auth.FirebaseAuth
import com.rohit.chat.R
import com.rohit.chat.data.repository.AuthRepository
import com.rohit.chat.databinding.FragmentSettingsBinding
import com.rohit.chat.ui.auth.AuthActivity
import com.rohit.chat.ui.auth.AuthViewModel
import com.rohit.chat.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var auth: FirebaseAuth

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()

        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {
            viewModel.getUserProfile(currentUserId)
        }
    }

    private fun setupUI() {
        binding.layoutProfile.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_profileFragment)
        }

        binding.tvTheme.setOnClickListener {
            toggleTheme()
        }

        binding.tvLogout.setOnClickListener {
            authRepository.logout()
            startActivity(Intent(requireContext(), AuthActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.userProfile.collectLatest { resource ->
                if (resource is Resource.Success) {
                    val user = resource.data
                    binding.tvDisplayName.text = user?.displayName
                    binding.tvAbout.text = user?.about
                    binding.ivProfile.load(user?.photoUrl) {
                        placeholder(R.drawable.ic_profile_default)
                        error(R.drawable.ic_profile_default)
                        transformations(coil.transform.CircleCropTransformation())
                    }
                }
            }
        }
    }

    private fun toggleTheme() {
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
