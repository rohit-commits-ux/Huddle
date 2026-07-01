package com.rohit.chat.ui.auth

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import coil.transform.CircleCropTransformation
import com.rohit.chat.MainActivity
import com.rohit.chat.R
import com.rohit.chat.databinding.FragmentSetupProfileBinding
import com.rohit.chat.utils.ImageUtils
import com.rohit.chat.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class SetupProfileFragment : Fragment() {

    private var _binding: FragmentSetupProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()
    private var photoUrl: String = ""

    private var latestTmpUri: Uri? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { handleImageSelection(it) }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            latestTmpUri?.let { handleImageSelection(it) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSetupProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivProfile.setOnClickListener { showImageOptions() }

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString()
            val username = binding.etUsername.text.toString().lowercase()
            if (name.isNotEmpty() && username.isNotEmpty()) {
                viewModel.createUserProfile(name, username, photoUrl)
            } else {
                Toast.makeText(requireContext(), "Enter your name and username", Toast.LENGTH_SHORT).show()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.profileCreated.collectLatest { resource ->
                    when (resource) {
                        is Resource.Success<*> -> {
                            binding.progressBar.visibility = View.GONE
                            startActivity(android.content.Intent(requireContext(), MainActivity::class.java))
                            requireActivity().finish()
                        }
                        is Resource.Error<*> -> {
                            binding.progressBar.visibility = View.GONE
                            binding.btnSave.isEnabled = true
                            Toast.makeText(requireContext(), resource.message ?: "An error occurred", Toast.LENGTH_SHORT).show()
                        }
                        is Resource.Loading<*> -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.btnSave.isEnabled = false
                        }
                        null -> {}
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.imageUploadState.collectLatest { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            binding.progressBar.visibility = View.GONE
                            photoUrl = resource.data ?: ""
                            binding.ivProfile.load(photoUrl) {
                                transformations(CircleCropTransformation())
                                placeholder(R.drawable.ic_profile_default)
                                error(R.drawable.ic_profile_default)
                            }
                            Toast.makeText(requireContext(), "Image uploaded!", Toast.LENGTH_SHORT).show()
                        }
                        is Resource.Error -> {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                        }
                        is Resource.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun showImageOptions() {
        val options = arrayOf("Camera", "Gallery")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Image")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> takePicture()
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun takePicture() {
        lifecycleScope.launchWhenStarted {
            getTmpFileUri().let { uri ->
                latestTmpUri = uri
                cameraLauncher.launch(uri)
            }
        }
    }

    private fun getTmpFileUri(): Uri {
        val tmpFile = File(requireContext().cacheDir, "tmp_image_${System.currentTimeMillis()}.jpg").apply {
            createNewFile()
            deleteOnExit()
        }
        return FileProvider.getUriForFile(requireContext(), "com.rohit.chat.fileprovider", tmpFile)
    }

    private fun handleImageSelection(uri: Uri) {
        val compressedFile = ImageUtils.compressImage(requireContext(), uri)
        compressedFile?.let {
            viewModel.uploadProfilePicture(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}