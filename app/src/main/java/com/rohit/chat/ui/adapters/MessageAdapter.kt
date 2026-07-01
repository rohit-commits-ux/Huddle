package com.rohit.chat.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rohit.chat.data.models.Message
import com.rohit.chat.databinding.ItemMessageReceivedBinding
import com.rohit.chat.databinding.ItemMessageSentBinding
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val currentUserId: String,
    private val onLongClick: (Message) -> Unit
) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    private val VIEW_TYPE_SENT = 1
    private val VIEW_TYPE_RECEIVED = 2

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            SentViewHolder(ItemMessageSentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            ReceivedViewHolder(ItemMessageReceivedBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        if (holder is SentViewHolder) holder.bind(message)
        else if (holder is ReceivedViewHolder) holder.bind(message)
    }

    inner class SentViewHolder(private val binding: ItemMessageSentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.tvReplyPreview.isVisible = message.replyTo != null
            message.replyTo?.let { reply ->
                binding.tvReplyPreview.text = "${if (reply.senderId == currentUserId) "You" else "Other"}: ${if (reply.messageType == "image") "📷 Photo" else reply.content}"
            }

            if (message.messageType == "image") {
                binding.tvMessage.isVisible = false
                binding.ivMedia.isVisible = true
                binding.ivMedia.load(message.content)
            } else {
                binding.tvMessage.isVisible = true
                binding.ivMedia.isVisible = false
                binding.tvMessage.text = message.content
            }
            binding.tvTime.text = formatTime(message.timestamp)
            binding.tvEdited.isVisible = message.isEdited
            
            // Status Icons
            val statusIcon = when (message.status) {
                "sending" -> android.R.drawable.progress_horizontal // Simplified
                "sent" -> android.R.drawable.checkbox_off_background // Single check placeholder
                "delivered" -> android.R.drawable.checkbox_on_background // Double check placeholder
                "read" -> android.R.drawable.checkbox_on_background // Blue double check placeholder
                else -> android.R.drawable.checkbox_off_background
            }
            binding.ivStatus.setImageResource(statusIcon)
            if (message.status == "read") {
                binding.ivStatus.setColorFilter(android.graphics.Color.BLUE)
            } else {
                binding.ivStatus.setColorFilter(android.graphics.Color.GRAY)
            }
            
            binding.root.setOnLongClickListener {
                onLongClick(message)
                true
            }
        }
    }

    inner class ReceivedViewHolder(private val binding: ItemMessageReceivedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.tvReplyPreview.isVisible = message.replyTo != null
            message.replyTo?.let { reply ->
                binding.tvReplyPreview.text = "${if (reply.senderId == currentUserId) "You" else "Other"}: ${if (reply.messageType == "image") "📷 Photo" else reply.content}"
            }

            if (message.messageType == "image") {
                binding.tvMessage.isVisible = false
                binding.ivMedia.isVisible = true
                binding.ivMedia.load(message.content)
            } else {
                binding.tvMessage.isVisible = true
                binding.ivMedia.isVisible = false
                binding.tvMessage.text = message.content
            }
            binding.tvTime.text = formatTime(message.timestamp)
            
            binding.root.setOnLongClickListener {
                onLongClick(message)
                true
            }
        }
    }

    private fun formatTime(time: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(time))
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean =
            oldItem.messageId == newItem.messageId
        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean =
            oldItem == newItem
    }
}
