package com.rohit.chat.ui.profile

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
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.google.firebase.auth.FirebaseAuth
import com.rohit.chat.R
import com.rohit.chat.data.models.User
import com.rohit.chat.databinding.FragmentProfileBinding
import com.rohit.chat.ui.auth.AuthViewModel
import com.rohit.chat.utils.ImageUtils
import com.rohit.chat.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var auth: FirebaseAuth

    private var currentUser: User? = null

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
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {
            viewModel.getUserProfile(currentUserId)
        }
        binding.btnSave.setOnClickListener { saveChanges() }
        binding.ivProfile.setOnClickListener { showImageOptions() }
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

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.userProfile.collectLatest { resource ->
                if (resource is Resource.Success) {
                    currentUser = resource.data
                    currentUser?.let { user ->
                        binding.etDisplayName.setText(user.displayName)
                        binding.etAbout.setText(user.about)
                        binding.tvUsername.text = "@${user.username}"
                        binding.ivProfile.load(user.photoUrl) {
                            transformations(CircleCropTransformation())
                            placeholder(R.drawable.ic_profile_default)
                            error(R.drawable.ic_profile_default)
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.imageUploadState.collectLatest { resource ->
                when (resource) {
                    is Resource.Success -> {
                        binding.progressBar.visibility = View.GONE
                        val imageUrl = resource.data
                        currentUser?.let { user ->
                            val updatedUser = user.copy(photoUrl = imageUrl ?: "")
                            viewModel.updateUserProfile(updatedUser)
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

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.profileCreated.collectLatest { resource ->
                when (resource) {
                    is Resource.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnSave.isEnabled = true
                        Toast.makeText(requireContext(), "Profile updated!", Toast.LENGTH_SHORT).show()
                    }
                    is Resource.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnSave.isEnabled = true
                        Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                    }
                    is Resource.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.btnSave.isEnabled = false
                    }
                    else -> {}
                }
            }
        }
    }

    private fun saveChanges() {
        val newName = binding.etDisplayName.text.toString().trim()
        val newAbout = binding.etAbout.text.toString().trim()
        if (newName.isEmpty()) {
            binding.tilDisplayName.error = "Name cannot be empty"
            return
        }
        currentUser?.let { user ->
            val updatedUser = user.copy(displayName = newName, about = newAbout)
            viewModel.updateUserProfile(updatedUser)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
