package com.goodok.app.ui.channels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.goodok.app.R
import com.goodok.app.data.Repository
import com.goodok.app.data.model.Channel
import com.goodok.app.databinding.ActivityChannelsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch

class ChannelsFragment : Fragment() {

    private lateinit var repository: Repository
    private lateinit var adapter: ChannelsAdapter
    private var _binding: ActivityChannelsBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance(): ChannelsFragment = ChannelsFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityChannelsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = Repository(requireContext())

        adapter = ChannelsAdapter { channel ->
            openChannel(channel)
        }

        binding.rvChannels.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChannels.adapter = adapter

        loadChannels()
    }

    private fun loadChannels() {
        lifecycleScope.launch {
            // For now, show empty state
            binding.tvEmpty.visibility = View.VISIBLE
        }
    }

    private fun openChannel(channel: Channel) {
        // TODO: Open channel chat
    }

    private fun showCreateChannelDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_channel, null)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.create_channel)
            .setView(dialogView)
            .setPositiveButton(R.string.create) { _, _ ->
                val nameInput = dialogView.findViewById<android.widget.EditText>(R.id.etChannelName)
                val descInput = dialogView.findViewById<android.widget.EditText>(R.id.etChannelDescription)

                val name = nameInput?.text?.toString()?.trim() ?: ""
                val description = descInput?.text?.toString()?.trim() ?: ""

                if (name.isNotEmpty()) {
                    createChannel(name, description)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun createChannel(name: String, description: String) {
        val currentId = repository.currentUserId ?: return

        lifecycleScope.launch {
            val channelRef = FirebaseDatabase.getInstance().getReference("channels").push()
            val channel = Channel(
                id = channelRef.key!!,
                name = name,
                description = description,
                ownerId = currentId,
                subscribers = listOf(currentId),
                createdAt = System.currentTimeMillis()
            )

            channelRef.setValue(channel)
                .addOnSuccessListener {
                    android.widget.Toast.makeText(
                        requireContext(),
                        R.string.success,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                .addOnFailureListener { e ->
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Error: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class ChannelsAdapter(
    private val onClick: (Channel) -> Unit
) : androidx.recyclerview.widget.ListAdapter<Channel, ChannelsAdapter.ChannelViewHolder>(ChannelDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val binding = com.goodok.app.databinding.ItemChannelBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChannelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChannelViewHolder(
        private val binding: com.goodok.app.databinding.ItemChannelBinding
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                    onClick(getItem(position))
                }
            }
        }

        fun bind(channel: Channel) {
            binding.tvName.text = channel.name
            binding.tvDescription.text = channel.description
            binding.tvSubscribers.text = "${channel.subscriberCount} ${itemView.context.getString(R.string.subscribers)}"
        }
    }
}

class ChannelDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<Channel>() {
    override fun areItemsTheSame(oldItem: Channel, newItem: Channel): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Channel, newItem: Channel): Boolean = oldItem == newItem
}
