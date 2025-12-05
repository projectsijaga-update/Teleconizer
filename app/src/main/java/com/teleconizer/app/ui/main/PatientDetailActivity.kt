package com.teleconizer.app.ui.main

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.teleconizer.app.R
import com.teleconizer.app.data.model.Patient
import com.teleconizer.app.databinding.ActivityPatientDetailBinding

class PatientDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPatientDetailBinding
    private lateinit var phoneAdapter: PhoneNumberAdapter
    private lateinit var contactList: MutableList<String>
    private var mediaPlayer: MediaPlayer? = null
    
    // SharedPreferences khusus untuk nomor telepon per pasien (ideally)
    // Tapi untuk simplifikasi kita pakai 1 global dulu bernama "contact_book"
    private lateinit var prefs: SharedPreferences
    
    // Kita butuh ViewModel untuk menghapus user
    private lateinit var viewModel: DashboardViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatientDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Init ViewModel
        viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]

        prefs = getSharedPreferences("contact_book", MODE_PRIVATE)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val patient = intent.getParcelableExtra<Patient>(EXTRA_PATIENT)
        if (patient != null) {
            bindPatient(patient)
            // Alarm logic
            if (patient.status.equals("DANGER", ignoreCase = true) || 
                patient.status.equals("JATUH", ignoreCase = true)) {
                startAlarm()
            }
        } else {
            Toast.makeText(this, "Data pasien error", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupContactList()
        setupButtons(patient!!)
    }
    
    private fun setupButtons(patient: Patient) {
        binding.btnBack.setOnClickListener { finish() }
        
        // Tombol STOP ALARM
        binding.btnStopAlarm.setOnClickListener {
            stopAlarm()
            Toast.makeText(this, "Alarm Dihentikan", Toast.LENGTH_SHORT).show()
        }

        // Tombol HAPUS USER (Baru)
        // Pastikan anda menambahkan Button ini di XML layout activity_patient_detail.xml jika belum ada
        // Atau gunakan btnDeleteContact yang sudah ada untuk fungsi hapus user
        binding.btnDeleteContact?.text = "Hapus Perangkat Ini"
        binding.btnDeleteContact?.setOnClickListener {
             AlertDialog.Builder(this)
                .setTitle("Hapus Perangkat?")
                .setMessage("Apakah anda yakin ingin menghapus ${patient.name} dari daftar?")
                .setPositiveButton("Ya") { _, _ ->
                    // Panggil fungsi delete di ViewModel (harus diakses lewat activity utama atau shared VM)
                    // Karena Activity ini terpisah, cara termudahnya adalah kirim sinyal balik
                    // Tapi solusi paling bersih adalah manipulasi Repository langsung disini:
                    val repo = com.teleconizer.app.data.repository.DeviceRepository(this)
                    // Kita butuh MAC address, tapi di model Patient sekarang cuma ada ID (hash).
                    // Ini kelemahan struktur saat ini.
                    // SOLUSI: Hapus berdasarkan iterasi list di repo.
                    val list = repo.getDeviceList()
                    // Hapus item yang namanya sama (simple approach) atau gunakan ID jika disimpan
                    list.removeAll { it.name == patient.name } 
                    repo.saveDeviceList(list)
                    
                    Toast.makeText(this, "Perangkat dihapus. Restart aplikasi untuk refresh.", Toast.LENGTH_LONG).show()
                    finish()
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun setupContactList() {
        // Ambil list kontak dari SharedPreferences yang KONSISTEN
        val savedSet = prefs.getStringSet("saved_contacts", mutableSetOf()) ?: mutableSetOf()
        contactList = savedSet.toMutableList()

        phoneAdapter = PhoneNumberAdapter(
            contacts = contactList,
            onEdit = { newNumber, index -> updateContact(index, newNumber) },
            onDelete = { index -> deleteContact(index) }
        )
        
        binding.rvPhoneNumbers.layoutManager = LinearLayoutManager(this)
        binding.rvPhoneNumbers.adapter = phoneAdapter
        
        // Tombol Tambah Kontak
        binding.btnAddContact.setOnClickListener {
            showAddContactDialog()
        }
    }

    private fun updateContact(index: Int, newNumber: String) {
        if (index in contactList.indices) {
            contactList[index] = newNumber
            saveContacts()
            phoneAdapter.notifyItemChanged(index)
        }
    }

    private fun deleteContact(index: Int) {
        if (index in contactList.indices) {
            contactList.removeAt(index)
            saveContacts()
            phoneAdapter.notifyItemRemoved(index)
            phoneAdapter.notifyItemRangeChanged(index, contactList.size)
        }
    }
    
    private fun showAddContactDialog() {
        val input = EditText(this)
        input.hint = "0812..."
        AlertDialog.Builder(this)
            .setTitle("Tambah Nomor Darurat")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val number = input.text.toString().trim()
                if (number.isNotEmpty()) {
                    contactList.add(number)
                    saveContacts()
                    phoneAdapter.notifyItemInserted(contactList.size - 1)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun saveContacts() {
        prefs.edit().putStringSet("saved_contacts", contactList.toSet()).apply()
        // Update tombol call enable/disable
        binding.btnCall.isEnabled = contactList.isNotEmpty()
    }

    private fun bindPatient(patient: Patient) {
        binding.toolbar.title = patient.name
        binding.tvStatus.text = patient.status.uppercase()
        
        val isDanger = patient.status.equals("DANGER", ignoreCase = true) || 
                       patient.status.equals("JATUH", ignoreCase = true)
                       
        binding.tvStatus.setTextColor(if(isDanger) getColor(android.R.color.holo_red_dark) else getColor(android.R.color.holo_green_dark))

        // Fix Lat/Long Display
        binding.tvCoordinates.text = "Lat: ${patient.latitude}\nLon: ${patient.longitude}"

        // Fix Google Maps
        binding.btnOpenInMaps.setOnClickListener {
            if (patient.latitude != 0.0 && patient.longitude != 0.0) {
                val uri = Uri.parse("geo:${patient.latitude},${patient.longitude}?q=${patient.latitude},${patient.longitude}(${patient.name})")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setPackage("com.google.android.apps.maps")
                // Cek jika maps ada
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    // Fallback ke browser jika tidak ada app maps
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${patient.latitude},${patient.longitude}")))
                }
            } else {
                Toast.makeText(this, "Lokasi belum tersedia (Masih 0.0)", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Logika Call Button
        binding.btnCall.setOnClickListener {
            if (contactList.isEmpty()) {
                Toast.makeText(this, "Tidak ada nomor darurat!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Tampilkan pilihan nomor jika lebih dari 1
            val numbers = contactList.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle("Pilih Nomor untuk Dihubungi")
                .setItems(numbers) { _, which ->
                    val selected = numbers[which]
                    makeCall(selected)
                }
                .show()
        }
    }
    
    private fun makeCall(number: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$number")
        }
        startActivity(intent)
    }

    private fun startAlarm() {
        // Gunakan raw/alarm_sound.mp3
        try {
             if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound)
                mediaPlayer?.isLooping = true
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }
    
    companion object {
        const val EXTRA_PATIENT = "extra_patient"
    }
}
