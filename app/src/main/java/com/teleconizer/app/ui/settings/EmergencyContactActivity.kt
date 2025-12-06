package com.teleconizer.app.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.teleconizer.app.data.model.ContactModel
import com.teleconizer.app.databinding.ActivityEmergencyContactBinding
import com.teleconizer.app.ui.main.PhoneNumberAdapter

class EmergencyContactActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmergencyContactBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var adapter: PhoneNumberAdapter
    private var contactList = mutableListOf<ContactModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmergencyContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("TeleconizerGlobalPrefs", Context.MODE_PRIVATE)

        setupList()
        
        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            if (name.isNotEmpty() && phone.isNotEmpty()) {
                contactList.add(ContactModel(name, phone))
                saveToGlobal()
                adapter.notifyDataSetChanged()
                binding.etName.setText("")
                binding.etPhone.setText("")
                Toast.makeText(this, "Nomor tersimpan", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Isi Nama dan Nomor", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupList() {
        val savedSet = prefs.getStringSet("GlobalContacts", emptySet()) ?: emptySet()
        contactList = savedSet.map { 
            val parts = it.split("|")
            if (parts.size >= 2) ContactModel(parts[0], parts[1]) else ContactModel("Unknown", it)
        }.toMutableList()

        adapter = PhoneNumberAdapter(
            contacts = contactList,
            onEdit = { contact, idx -> showEditDialog(contact, idx) },
            onDelete = { idx ->
                contactList.removeAt(idx)
                saveToGlobal()
                adapter.notifyItemRemoved(idx)
            }
        )
    }

    private fun showEditDialog(contact: ContactModel, index: Int) {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val etName = EditText(this)
        etName.setText(contact.name)
        layout.addView(etName)

        val etPhone = EditText(this)
        etPhone.setText(contact.number)
        layout.addView(etPhone)

        AlertDialog.Builder(this)
            .setTitle("Edit Kontak")
            .setView(layout)
            .setPositiveButton("Simpan") { _, _ ->
                val newName = etName.text.toString()
                val newPhone = etPhone.text.toString()
                if (newName.isNotEmpty() && newPhone.isNotEmpty()) {
                    contactList[index] = ContactModel(newName, newPhone)
                    saveToGlobal()
                    adapter.notifyItemChanged(index)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun saveToGlobal() {
        val set = contactList.map { "${it.name}|${it.number}" }.toSet()
        prefs.edit().putStringSet("GlobalContacts", set).apply()
    }
}
