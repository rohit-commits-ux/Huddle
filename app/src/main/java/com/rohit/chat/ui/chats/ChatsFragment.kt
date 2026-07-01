package com.rohit.chat.ui.chats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.rohit.chat.R
import com.rohit.chat.databinding.FragmentChatsBinding
import com.rohit.chat.ui.adapters.ChatAdapter
import com.rohit.chat.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatsFragment : Fragment(), MenuProvider {

    private var _binding: FragmentChatsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        setupRecyclerView()
        observeViewModel()

        binding.layoutRequests.setOnClickListener {
            findNavController().navigate(R.id.action_chatsFragment_to_requestsFragment)
        }

        binding.fabNewChat.setOnClickListener {
            findNavController().navigate(R.id.action_chatsFragment_to_searchFragment)
        }

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            viewModel.getChats(currentUserId)
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.main_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.newGroup -> {
                // Future implementation
                Toast.makeText(requireContext(), "New Group coming soon", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.settings -> {
                findNavController().navigate(R.id.action_chatsFragment_to_settingsFragment)
                true
            }
            else -> false
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter { chat ->
            // Navigate to individual chat screen
            val bundle = Bundle().apply {
                putString("chatId", chat.chatId)
            }
            findNavController().navigate(R.id.action_chatsFragment_to_messageFragment, bundle)
        }
        binding.rvChats.adapter = chatAdapter
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.chats.collectLatest { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            binding.progressBar.isVisible = false
                            chatAdapter.submitList(resource.data)
                        }
                        is Resource.Error -> {
                            binding.progressBar.isVisible = false
                            Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                        }
                        is Resource.Loading -> {
                            binding.progressBar.isVisible = true
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
