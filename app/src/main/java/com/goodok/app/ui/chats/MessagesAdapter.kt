package com.goodok.app.ui.chats

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.goodok.app.data.model.Message
import com.goodok.app.databinding.ItemMessageBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessagesAdapter(
    private val currentUserId: String?
) : ListAdapter<Message, MessagesAdapter.MessageViewHolder>(MessageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MessageViewHolder(
        private val binding: ItemMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            val isSent = message.senderId == currentUserId

            if (isSent) {
                // Show sent message
                binding.layoutSent.visibility = View.VISIBLE
                binding.layoutReceived.visibility = View.GONE

                binding.tvTextSent.text = message.text
                binding.tvTimeSent.text = formatTime(message.timestamp)

                // Show media if exists
                if (message.mediaUrl != null) {
                    binding.ivMediaSent.visibility = View.VISIBLE
                    Glide.with(binding.root.context)
                        .load(message.mediaUrl)
                        .into(binding.ivMediaSent)
                } else {
                    binding.ivMediaSent.visibility = View.GONE
                }
            } else {
                // Show received message
                binding.layoutSent.visibility = View.GONE
                binding.layoutReceived.visibility = View.VISIBLE

                binding.tvTextReceived.text = message.text
                binding.tvTimeReceived.text = formatTime(message.timestamp)

                // Show sender name in group chats
                if (message.senderName.isNotEmpty()) {
                    binding.tvSenderName.visibility = View.VISIBLE
                    binding.tvSenderName.text = message.senderName
                } else {
                    binding.tvSenderName.visibility = View.GONE
                }

                // Show media if exists
                if (message.mediaUrl != null) {
                    binding.ivMediaReceived.visibility = View.VISIBLE
                    Glide.with(binding.root.context)
                        .load(message.mediaUrl)
                        .into(binding.ivMediaReceived)
                } else {
                    binding.ivMediaReceived.visibility = View.GONE
                }
            }
        }

        private fun formatTime(timestamp: Long): String {
            return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
    }
}

class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean = oldItem == newItem
}
