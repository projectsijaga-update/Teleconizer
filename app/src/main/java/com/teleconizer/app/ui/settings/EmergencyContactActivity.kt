package com.teleconizer.app.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.teleconizer.app.databinding.ActivityEmergencyContactBinding
import com.teleconizer.app.ui.main.PhoneNumberAdapter

class EmergencyContactActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmergencyContactBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var adapter: PhoneNumberAdapter
    private var contactList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmergencyContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // GUNAKAN NAMA PREFS YANG SAMA DI SEMUA HALAMAN (Global)
        prefs = getSharedPreferences("TeleconizerGlobalPrefs", Context.MODE_PRIVATE)

        setupList()
        
        binding.btnSave.setOnClickListener {
            val phone = binding.etPhone.text.toString().trim()
            if (phone.isNotEmpty()) {
                contactList.add(phone)
                saveToGlobal()
                adapter.notifyDataSetChanged()
                binding.etPhone.setText("")
                Toast.makeText(this, "Nomor tersimpan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupList() {
        // Ambil data dari Global Prefs
        val savedSet = prefs.getStringSet("GlobalContacts", emptySet()) ?: emptySet()
        contactList = savedSet.toMutableList()

        adapter = PhoneNumberAdapter(
            contacts = contactList,
            onEdit = { newNum, idx -> 
                contactList[idx] = newNum
                saveToGlobal()
                adapter.notifyItemChanged(idx)
            },
            onDelete = { idx ->
                contactList.removeAt(idx)
                saveToGlobal()
                adapter.notifyDataSetChanged()
            }
        )

        // Pastikan di layout XML ada RecyclerView dengan id rvContacts
        // Jika belum ada, tambahkan di activity_emergency_contact.xml
        // Untuk sementara, kita asumsikan Anda punya list di sana atau hanya input field.
        // Jika hanya input field, bagian adapter ini bisa disesuaikan.
    }

    private fun saveToGlobal() {
        prefs.edit().putStringSet("GlobalContacts", contactList.toSet()).apply()
    }
}
