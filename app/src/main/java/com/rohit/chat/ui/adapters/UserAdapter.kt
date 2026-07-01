package com.rohit.chat.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rohit.chat.data.models.User
import com.rohit.chat.databinding.ItemUserBinding
import com.rohit.chat.R
import coil.load
import coil.transform.CircleCropTransformation

class UserAdapter(
    private val onSendRequest: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private var users = listOf<User>()

    fun setUsers(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount() = users.size

    inner class UserViewHolder(private val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {
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
            binding.btnSendRequest.setOnClickListener { onSendRequest(user) }
        }
    }
}
