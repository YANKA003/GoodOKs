package com.goodok.app.ui.calls

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.goodok.app.R
import com.goodok.app.data.Repository
import com.goodok.app.data.model.Call
import com.goodok.app.databinding.ActivityCallsBinding
import kotlinx.coroutines.launch

class CallsFragment : Fragment() {

    private lateinit var repository: Repository
    private lateinit var adapter: CallsAdapter
    private var _binding: ActivityCallsBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance(): CallsFragment = CallsFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityCallsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = Repository(requireContext())

        adapter = CallsAdapter(repository.currentUserId)
        binding.rvCalls.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCalls.adapter = adapter

        loadCalls()
    }

    private fun loadCalls() {
        lifecycleScope.launch {
            repository.observeCalls().collect { calls ->
                adapter.submitList(calls)
                binding.tvEmpty.visibility = if (calls.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class CallsAdapter(
    private val currentUserId: String?
) : androidx.recyclerview.widget.ListAdapter<Call, CallsAdapter.CallViewHolder>(CallDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallViewHolder {
        val binding = com.goodok.app.databinding.ItemCallBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CallViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CallViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CallViewHolder(
        private val binding: com.goodok.app.databinding.ItemCallBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(call: Call) {
            val isOutgoing = call.callerId == currentUserId

            binding.tvName.text = if (isOutgoing) call.receiverName else call.callerName

            // Call type icon
            val callTypeIcon = if (call.type == com.goodok.app.data.model.CallType.VIDEO) {
                android.R.drawable.ic_menu_camera
            } else {
                android.R.drawable.ic_menu_call
            }
            binding.ivCallType.setImageResource(callTypeIcon)

            // Call info
            val callInfo = when {
                call.status == com.goodok.app.data.model.CallStatus.MISSED -> {
                    itemView.context.getString(R.string.call_missed)
                }
                call.status == com.goodok.app.data.model.CallStatus.DECLINED -> {
                    itemView.context.getString(R.string.call_declined)
                }
                isOutgoing -> {
                    val duration = formatDuration(call.duration)
                    "${itemView.context.getString(R.string.voice_call)}, $duration"
                }
                else -> {
                    val duration = formatDuration(call.duration)
                    "${itemView.context.getString(R.string.incoming_call)}, $duration"
                }
            }
            binding.tvCallInfo.text = callInfo

            // Time
            binding.tvTime.text = formatTime(call.startTime ?: call.timestamp)
        }

        private fun formatDuration(seconds: Long): String {
            if (seconds <= 0) return "0 сек"
            val minutes = seconds / 60
            val secs = seconds % 60
            return if (minutes > 0) {
                "$minutes мин $secs сек"
            } else {
                "$secs сек"
            }
        }

        private fun formatTime(timestamp: Long): String {
            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(timestamp))
        }
    }
}

class CallDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<Call>() {
    override fun areItemsTheSame(oldItem: Call, newItem: Call): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Call, newItem: Call): Boolean = oldItem == newItem
}
