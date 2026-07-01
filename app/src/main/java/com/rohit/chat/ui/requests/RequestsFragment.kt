package com.rohit.chat.ui.requests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.rohit.chat.databinding.FragmentRequestsBinding
import com.rohit.chat.ui.adapters.RequestAdapter
import com.rohit.chat.ui.chats.ChatViewModel
import com.rohit.chat.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class RequestsFragment : Fragment() {

    private var _binding: FragmentRequestsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var incomingAdapter: RequestAdapter
    private lateinit var outgoingAdapter: RequestAdapter

    @Inject
    lateinit var auth: FirebaseAuth

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        
        val currentUserId = auth.currentUser?.uid ?: ""
        viewModel.getIncomingRequests(currentUserId)
        viewModel.getOutgoingRequests(currentUserId)

        observeViewModel()
    }

    private fun setupRecyclerViews() {
        incomingAdapter = RequestAdapter(true, 
            onAccept = { request -> viewModel.updateRequestStatus(request, "accepted") },
            onDecline = { request -> viewModel.updateRequestStatus(request, "declined") }
        )
        binding.rvIncoming.adapter = incomingAdapter

        outgoingAdapter = RequestAdapter(false)
        binding.rvOutgoing.adapter = outgoingAdapter
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.incomingRequests.collectLatest { resource ->
                if (resource is Resource.Success) {
                    incomingAdapter.setRequests(resource.data ?: emptyList())
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.outgoingRequests.collectLatest { resource ->
                if (resource is Resource.Success) {
                    outgoingAdapter.setRequests(resource.data ?: emptyList())
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
