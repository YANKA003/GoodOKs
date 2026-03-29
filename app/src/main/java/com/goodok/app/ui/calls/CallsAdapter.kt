package com.goodok.app.ui.calls

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.goodok.app.data.model.Call
import com.goodok.app.databinding.ItemCallBinding
import java.text.SimpleDateFormat
import java.util.*

class CallsAdapter :
    ListAdapter<Call, CallsAdapter.CallViewHolder>(CallDiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallViewHolder {
        val binding = ItemCallBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CallViewHolder(binding, dateFormat)
    }

    override fun onBindViewHolder(holder: CallViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CallViewHolder(
        private val binding: ItemCallBinding,
        private val dateFormat: SimpleDateFormat
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(call: Call) {
            binding.tvName.text = call.callerName.ifEmpty { "Unknown" }
            binding.tvTime.text = dateFormat.format(Date(call.timestamp))

            val statusText = when (call.status) {
                "MISSED" -> "Missed"
                "INCOMING" -> "Incoming"
                "OUTGOING" -> "Outgoing"
                else -> "Ended"
            }
            binding.tvStatus.text = statusText

            val durationText = if (call.duration > 0) {
                val minutes = call.duration / 60
                val seconds = call.duration % 60
                String.format("%d:%02d", minutes, seconds)
            } else {
                ""
            }
            binding.tvDuration.text = durationText
        }
    }

    class CallDiffCallback : DiffUtil.ItemCallback<Call>() {
        override fun areItemsTheSame(oldItem: Call, newItem: Call): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Call, newItem: Call): Boolean {
            return oldItem == newItem
        }
    }
}
