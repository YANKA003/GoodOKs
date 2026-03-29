package com.goodok.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.goodok.app.data.model.User
import com.goodok.app.databinding.ItemUserBinding

class UsersAdapter(private val onClick: (User) -> Unit) :
    ListAdapter<User, UsersAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class UserViewHolder(
        private val binding: ItemUserBinding,
        private val onClick: (User) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.tvName.text = user.username
            binding.tvStatus.text = if (user.isOnline) "Online" else "Offline"

            binding.root.setOnClickListener { onClick(user) }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}
