package com.rohit.chat.ui.chats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.rohit.chat.databinding.FragmentSelectUserBinding
import com.rohit.chat.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SelectUserFragment : Fragment() {

    private var _binding: FragmentSelectUserBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SelectUserViewModel by viewModels()
    private lateinit var adapter: UsersAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSelectUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = UsersAdapter(
            onUserClick = { user ->
                viewModel.createChat(user.userId)
            },
            onAddContactClick = { user ->
                viewModel.addContact(user.userId)
            }
        )
        binding.rvUsers.adapter = adapter

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.searchUsers(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { viewModel.searchUsers(it) }
                return true
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.usersState.collectLatest { resource ->
                        when (resource) {
                            is Resource.Success -> {
                                binding.progressBar.visibility = View.GONE
                                adapter.submitList(resource.data)
                                binding.tvNoUsers.visibility = if (resource.data.isNullOrEmpty()) View.VISIBLE else View.GONE
                            }
                            is Resource.Error -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                            }
                            is Resource.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                launch {
                    viewModel.addContactState.collectLatest { resource ->
                        when (resource) {
                            is Resource.Success -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(requireContext(), "Contact added!", Toast.LENGTH_SHORT).show()
                            }
                            is Resource.Error -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                            }
                            is Resource.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                            }
                            null -> {}
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.createChatState.collectLatest { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            binding.progressBar.visibility = View.GONE
                            // Navigate to ChatFragment with chatId
                            // findNavController().navigate(...)
                            // For now just go back
                            findNavController().popBackStack()
                        }
                        is Resource.Error -> {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                        }
                        is Resource.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        null -> {}
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