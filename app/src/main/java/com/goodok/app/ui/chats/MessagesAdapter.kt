package com.goodok.app.ui.chats

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.goodok.app.data.model.Message
import com.goodok.app.databinding.ItemMessageBinding
import java.text.SimpleDateFormat
import java.util.*

class MessagesAdapter(private val currentUserId: String) :
    ListAdapter<Message, MessagesAdapter.MessageViewHolder>(MessageDiffCallback()) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageViewHolder(binding, currentUserId, timeFormat)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MessageViewHolder(
        private val binding: ItemMessageBinding,
        private val currentUserId: String,
        private val timeFormat: SimpleDateFormat
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            val isSent = message.senderId == currentUserId

            binding.tvMessage.text = if (message.isDeleted) {
                "Message deleted"
            } else {
                message.content
            }

            binding.tvTime.text = timeFormat.format(Date(message.timestamp))

            if (message.isEdited && !message.isDeleted) {
                binding.tvMessage.text = "${message.content} (edited)"
            }

            // Change appearance based on sent/received
            val context = binding.root.context
            if (isSent) {
                binding.root.layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
            } else {
                binding.root.layoutDirection = android.view.View.LAYOUT_DIRECTION_LTR
            }
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}
