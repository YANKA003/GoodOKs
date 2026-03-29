package com.goodok.app.ui.channels

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.goodok.app.data.model.Channel
import com.goodok.app.databinding.ItemChannelBinding

class ChannelsAdapter(private val onClick: (Channel) -> Unit) :
    ListAdapter<Channel, ChannelsAdapter.ChannelViewHolder>(ChannelDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val binding = ItemChannelBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChannelViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ChannelViewHolder(
        private val binding: ItemChannelBinding,
        private val onClick: (Channel) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(channel: Channel) {
            binding.tvName.text = channel.name
            binding.tvDescription.text = channel.description.ifEmpty { "No description" }
            binding.tvSubscribers.text = "${channel.subscribersCount} subscribers"

            binding.root.setOnClickListener { onClick(channel) }
        }
    }

    class ChannelDiffCallback : DiffUtil.ItemCallback<Channel>() {
        override fun areItemsTheSame(oldItem: Channel, newItem: Channel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Channel, newItem: Channel): Boolean {
            return oldItem == newItem
        }
    }
}
