package com.rohit.chat.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rohit.chat.R
import com.rohit.chat.data.models.Chat
import com.rohit.chat.data.models.User
import com.rohit.chat.databinding.ItemChatBinding
import coil.load
import coil.transform.CircleCropTransformation
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val onChatClick: (Chat) -> Unit
) : ListAdapter<Chat, ChatAdapter.ChatViewHolder>(ChatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChatViewHolder(private val binding: ItemChatBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(chat: Chat) {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            val otherUserId = chat.participants.find { it != currentUserId }
            
            if (otherUserId != null) {
                FirebaseFirestore.getInstance().collection("users").document(otherUserId)
                    .addSnapshotListener { snapshot, _ ->
                        val user = snapshot?.toObject(User::class.java)
                        binding.tvName.text = user?.displayName ?: "Unknown"
                        binding.ivProfile.load(user?.photoUrl) {
                            crossfade(true)
                            placeholder(R.drawable.ic_profile_default)
                            error(R.drawable.ic_profile_default)
                            transformations(CircleCropTransformation())
                        }
                        binding.viewOnlineStatus.isVisible = user?.online == true
                    }
            }

            binding.tvLastMessage.text = chat.lastMessage
            binding.tvTime.text = formatTime(chat.lastMessageTime)
            
            val unreadCount = chat.unreadCount[currentUserId] ?: 0
            if (unreadCount > 0) {
                binding.tvUnreadCount.isVisible = true
                binding.tvUnreadCount.text = unreadCount.toString()
            } else {
                binding.tvUnreadCount.isVisible = false
            }

            binding.root.setOnClickListener { onChatClick(chat) }
        }

        private fun formatTime(time: Long): String {
            if (time == 0L) return ""
            val now = Calendar.getInstance()
            val messageTime = Calendar.getInstance().apply { timeInMillis = time }
            
            return when {
                now.get(Calendar.DATE) == messageTime.get(Calendar.DATE) -> {
                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(time))
                }
                now.get(Calendar.DATE) - messageTime.get(Calendar.DATE) == 1 -> "Yesterday"
                else -> SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(time))
            }
        }
    }

    class ChatDiffCallback : DiffUtil.ItemCallback<Chat>() {
        override fun areItemsTheSame(oldItem: Chat, newItem: Chat): Boolean =
            oldItem.chatId == newItem.chatId

        override fun areContentsTheSame(oldItem: Chat, newItem: Chat): Boolean =
            oldItem == newItem
    }
}
