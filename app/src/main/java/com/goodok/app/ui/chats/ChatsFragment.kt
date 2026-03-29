package com.goodok.app.ui.chats

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.goodok.app.R
import com.goodok.app.data.Repository
import com.goodok.app.data.model.Chat
import com.goodok.app.data.model.User
import com.goodok.app.databinding.ActivityCallsBinding
import com.goodok.app.databinding.ItemUserBinding
import kotlinx.coroutines.launch

class ChatsFragment : Fragment() {

    private lateinit var repository: Repository
    private lateinit var adapter: ChatsAdapter
    private var _binding: ActivityCallsBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance(): ChatsFragment = ChatsFragment()
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

        adapter = ChatsAdapter { chat ->
            openChat(chat)
        }

        binding.rvCalls.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCalls.adapter = adapter

        loadChats()
    }

    private fun loadChats() {
        lifecycleScope.launch {
            repository.observeChats().collect { chats ->
                adapter.submitList(chats)
                binding.tvEmpty.visibility = if (chats.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun openChat(chat: Chat) {
        val intent = Intent(requireContext(), ChatActivity::class.java)
        intent.putExtra("chat_id", chat.id)
        val otherUserId = chat.participants.firstOrNull { it != repository.currentUserId }
        intent.putExtra("other_user_id", otherUserId)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class ChatsAdapter(
    private val onClick: (Chat) -> Unit
) : androidx.recyclerview.widget.ListAdapter<Chat, ChatsAdapter.ChatViewHolder>(ChatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChatViewHolder(
        private val binding: ItemUserBinding
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                    onClick(getItem(position))
                }
            }
        }

        fun bind(chat: Chat) {
            binding.tvName.text = chat.name.ifEmpty { "Чат" }
            binding.tvLastMessage.text = chat.lastMessage?.text ?: ""
            binding.tvTime.text = formatTime(chat.updatedAt)

            if (chat.unreadCount > 0) {
                binding.tvUnread.visibility = View.VISIBLE
                binding.tvUnread.text = chat.unreadCount.toString()
            } else {
                binding.tvUnread.visibility = View.GONE
            }
        }

        private fun formatTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 86400000 -> { // Less than 24 hours
                    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
                }
                diff < 172800000 -> { // Less than 48 hours
                    itemView.context.getString(R.string.yesterday)
                }
                else -> {
                    java.text.SimpleDateFormat("dd.MM", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
                }
            }
        }
    }
}

class ChatDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<Chat>() {
    override fun areItemsTheSame(oldItem: Chat, newItem: Chat): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Chat, newItem: Chat): Boolean = oldItem == newItem
}
