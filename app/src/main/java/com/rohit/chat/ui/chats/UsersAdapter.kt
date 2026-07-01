package com.rohit.chat.ui.chats

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.rohit.chat.R
import com.rohit.chat.data.models.User
import com.rohit.chat.databinding.ItemUserBinding

class UsersAdapter(
    private val onUserClick: (User) -> Unit,
    private val onAddContactClick: (User) -> Unit
) : ListAdapter<User, UsersAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(private val binding: ItemUserBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.tvName.text = user.displayName
            binding.tvUsername.text = "@${user.username}"
            binding.tvAbout.text = user.about
            binding.ivProfile.load(user.photoUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_profile_default)
                error(R.drawable.ic_profile_default)
                transformations(CircleCropTransformation())
            }
            binding.btnSendRequest.setOnClickListener { onAddContactClick(user) }
            binding.root.setOnClickListener { onUserClick(user) }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}