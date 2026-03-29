package com.goodok.app.ui.contacts

import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.goodok.app.R
import com.goodok.app.data.Repository
import com.goodok.app.data.model.Contact
import com.goodok.app.databinding.ActivityContactsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactsFragment : Fragment() {

    private lateinit var repository: Repository
    private lateinit var adapter: ContactsAdapter
    private var _binding: ActivityContactsBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance(): ContactsFragment = ContactsFragment()
        private const val REQUEST_CONTACTS = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = Repository(requireContext())

        adapter = ContactsAdapter(
            onMessageClick = { contact ->
                openChat(contact)
            },
            onCallClick = { contact ->
                startCall(contact)
            }
        )

        binding.rvContacts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvContacts.adapter = adapter

        binding.btnImportContacts.setOnClickListener {
            checkAndRequestContactsPermission()
        }

        // Auto-load contacts if permission granted
        if (hasContactsPermission()) {
            loadContacts()
        }
    }

    private fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkAndRequestContactsPermission() {
        if (hasContactsPermission()) {
            loadContacts()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.READ_CONTACTS),
                REQUEST_CONTACTS
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CONTACTS && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadContacts()
        } else {
            Toast.makeText(requireContext(), R.string.contacts_permission, Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadContacts() {
        binding.btnImportContacts.isEnabled = false

        lifecycleScope.launch {
            val contacts = withContext(Dispatchers.IO) {
                importContactsFromPhone()
            }

            adapter.submitList(contacts)
            binding.tvEmpty.visibility = if (contacts.isEmpty()) View.VISIBLE else View.GONE
            binding.btnImportContacts.isEnabled = true

            Toast.makeText(requireContext(), R.string.contacts_imported, Toast.LENGTH_SHORT).show()
        }
    }

    private fun importContactsFromPhone(): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val resolver: ContentResolver = requireContext().contentResolver

        val cursor = resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            null,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)

            while (it.moveToNext()) {
                val id = it.getString(idIndex)
                val name = it.getString(nameIndex) ?: continue

                // Get phone numbers
                val phoneCursor = resolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(id),
                    null
                )

                phoneCursor?.use { phoneIt ->
                    val phoneIndex = phoneIt.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                    while (phoneIt.moveToNext()) {
                        val phone = phoneIt.getString(phoneIndex) ?: continue
                        val cleanPhone = phone.replace("[^+\\d]".toRegex(), "")

                        contacts.add(Contact(
                            id = id,
                            name = name,
                            phone = cleanPhone,
                            registered = false
                        ))
                    }
                }
            }
        }

        return contacts
    }

    private fun openChat(contact: Contact) {
        lifecycleScope.launch {
            // Find user by phone
            val users = repository.observeUsers()
            // For now, create a simple chat flow
            Toast.makeText(requireContext(), "Opening chat with ${contact.name}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCall(contact: Contact) {
        Toast.makeText(requireContext(), "Calling ${contact.name}", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
