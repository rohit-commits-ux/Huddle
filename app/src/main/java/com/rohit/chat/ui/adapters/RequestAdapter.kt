package com.rohit.chat.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.rohit.chat.data.models.ChatRequest
import com.rohit.chat.databinding.ItemRequestBinding
import com.rohit.chat.R
import coil.load
import coil.transform.CircleCropTransformation

class RequestAdapter(
    private val isIncoming: Boolean,
    private val onAccept: (ChatRequest) -> Unit = {},
    private val onDecline: (ChatRequest) -> Unit = {}
) : RecyclerView.Adapter<RequestAdapter.RequestViewHolder>() {

    private var requests = listOf<ChatRequest>()

    fun setRequests(newRequests: List<ChatRequest>) {
        requests = newRequests
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val binding = ItemRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RequestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.bind(requests[position])
    }

    override fun getItemCount() = requests.size

    inner class RequestViewHolder(private val binding: ItemRequestBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(request: ChatRequest) {
            binding.tvName.text = request.senderName
            binding.tvUsername.text = "@${request.senderUsername}"
            binding.ivProfile.load(request.senderPhotoUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_profile_default)
                error(R.drawable.ic_profile_default)
                transformations(CircleCropTransformation())
            }
            
            if (isIncoming) {
                binding.btnAccept.isVisible = true
                binding.btnDecline.isVisible = true
                binding.tvStatus.isVisible = false
                binding.btnAccept.setOnClickListener { onAccept(request) }
                binding.btnDecline.setOnClickListener { onDecline(request) }
            } else {
                binding.btnAccept.isVisible = false
                binding.btnDecline.isVisible = false
                binding.tvStatus.isVisible = true
                binding.tvStatus.text = "Status: ${request.status}"
            }
        }
    }
}
