package com.rohit.chat.ui.chats

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.google.firebase.auth.FirebaseAuth
import com.rohit.chat.R
import com.rohit.chat.data.models.Message
import com.rohit.chat.databinding.FragmentMessageBinding
import com.rohit.chat.ui.adapters.MessageAdapter
import com.rohit.chat.utils.ImageUtils
import com.rohit.chat.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class MessageFragment : Fragment() {

    private var _binding: FragmentMessageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var messageAdapter: MessageAdapter
    private var chatId: String? = null
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private var otherUserId: String? = null
    private var replyingTo: Message? = null

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
        _binding = FragmentMessageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatId = arguments?.getString("chatId")
        otherUserId = chatId?.split("_")?.find { it != currentUserId }

        setupRecyclerView()
        observeViewModel()
        setupInput()

        chatId?.let { 
            viewModel.getMessages(it)
            viewModel.observeChatRoom(it)
            viewModel.resetUnread(it, currentUserId)
        }
        otherUserId?.let { viewModel.observeUserPresence(it) }
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(currentUserId) { message ->
            showOptionsDialog(message)
        }
        binding.rvMessages.adapter = messageAdapter
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
    }

    private fun setupInput() {
        binding.btnAttach.setOnClickListener { showImageOptions() }
        binding.btnCancelReply.setOnClickListener { cancelReply() }
        binding.btnSend.setOnClickListener {
            val content = binding.etMessage.text.toString().trim()
            if (content.isNotEmpty() && chatId != null && otherUserId != null) {
                val message = Message(
                    chatId = chatId!!,
                    senderId = currentUserId,
                    receiverId = otherUserId!!,
                    content = content,
                    timestamp = System.currentTimeMillis(),
                    replyTo = replyingTo
                )
                viewModel.sendMessage(message)
                binding.etMessage.setText("")
                cancelReply()
            }
        }

        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                chatId?.let { viewModel.setTyping(it, currentUserId, s.toString().isNotEmpty()) }
                if (s.toString().isNotEmpty()) {
                    binding.btnSend.setImageResource(android.R.drawable.ic_menu_send)
                } else {
                    binding.btnSend.setImageResource(android.R.drawable.ic_btn_speak_now)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun showReplyPreview(message: Message) {
        replyingTo = message
        binding.layoutReplyPreview.visibility = View.VISIBLE
        binding.tvReplyName.text = if (message.senderId == currentUserId) "You" else "Other"
        binding.tvReplyContent.text = if (message.messageType == "image") "📷 Photo" else message.content
    }

    private fun cancelReply() {
        replyingTo = null
        binding.layoutReplyPreview.visibility = View.GONE
    }

    private fun showImageOptions() {
        val options = arrayOf("Camera", "Gallery")
        AlertDialog.Builder(requireContext())
            .setTitle("Send Photo")
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
            viewModel.uploadImage(it)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.messages.collectLatest { messages ->
                messageAdapter.submitList(messages)
                if (messages.isNotEmpty()) {
                    binding.rvMessages.smoothScrollToPosition(messages.size - 1)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.otherUserPresence.collectLatest { resource ->
                if (resource is Resource.Success) {
                    val user = resource.data
                    binding.tvToolbarName.text = user?.displayName
                    binding.ivToolbarProfile.load(user?.photoUrl) {
                        placeholder(R.drawable.ic_profile_default)
                        error(R.drawable.ic_profile_default)
                    }
                    binding.tvToolbarStatus.text = if (user?.online == true) "Online" else "Offline"
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentChat.collectLatest { resource ->
                if (resource is Resource.Success) {
                    val chat = resource.data
                    val isTyping = chat?.typingStatus?.get(otherUserId) ?: false
                    if (isTyping) {
                        binding.tvToolbarStatus.text = "Typing..."
                    } else {
                        // Fallback to online/offline logic if not typing
                        viewModel.otherUserPresence.value?.let { presence ->
                            if (presence is Resource.Success) {
                                binding.tvToolbarStatus.text = if (presence.data?.online == true) "Online" else "Offline"
                            }
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.imageUploadState.collectLatest { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val mediaUrl = resource.data ?: ""
                        val message = Message(
                            chatId = chatId ?: return@collectLatest,
                            senderId = currentUserId,
                            receiverId = otherUserId ?: return@collectLatest,
                            content = mediaUrl,
                            messageType = "image",
                            timestamp = System.currentTimeMillis()
                        )
                        viewModel.sendMessage(message)
                        viewModel.resetImageUploadState()
                        Toast.makeText(requireContext(), "Photo sent!", Toast.LENGTH_SHORT).show()
                    }
                    is Resource.Error -> {
                        Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun showOptionsDialog(message: Message) {
        val options = mutableListOf("Copy", "Forward", "Reply")
        if (message.senderId == currentUserId) {
            options.add("Edit")
            options.add("Delete for Everyone")
        }
        options.add("Delete for Me")

        AlertDialog.Builder(requireContext())
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "Reply" -> showReplyPreview(message)
                    "Delete for Everyone" -> chatId?.let { viewModel.deleteMessage(it, message.messageId, true, currentUserId) }
                    "Delete for Me" -> chatId?.let { viewModel.deleteMessage(it, message.messageId, false, currentUserId) }
                    "Copy" -> {
                        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("message", message.content)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                }
            }.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        chatId?.let { viewModel.setTyping(it, currentUserId, false) }
        _binding = null
    }
}
