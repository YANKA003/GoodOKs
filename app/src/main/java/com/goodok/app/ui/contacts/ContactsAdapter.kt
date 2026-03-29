package com.goodok.app.ui.contacts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.goodok.app.data.model.Contact
import com.goodok.app.databinding.ItemContactBinding

class ContactsAdapter(private val onClick: (Contact) -> Unit) :
    ListAdapter<Contact, ContactsAdapter.ContactViewHolder>(ContactDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContactViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ContactViewHolder(
        private val binding: ItemContactBinding,
        private val onClick: (Contact) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: Contact) {
            binding.tvName.text = contact.displayName
            binding.tvPhone.text = contact.phoneNumber

            if (contact.isRegistered) {
                binding.tvStatus.text = "On GoodOK"
                binding.tvStatus.setTextColor(0xFF4CAF50.toInt())
            } else {
                binding.tvStatus.text = "Invite"
                binding.tvStatus.setTextColor(0xFF9E9E9E.toInt())
            }

            binding.root.setOnClickListener { onClick(contact) }
        }
    }

    class ContactDiffCallback : DiffUtil.ItemCallback<Contact>() {
        override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean {
            return oldItem == newItem
        }
    }
}
