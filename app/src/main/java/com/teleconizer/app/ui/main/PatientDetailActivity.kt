package com.teleconizer.app.ui.main

import android.Manifest
import android.content.Context // [PERBAIKAN] Import ini sebelumnya hilang
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
import com.teleconizer.app.data.repository.DeviceRepository
import com.teleconizer.app.databinding.ActivityPatientDetailBinding

class PatientDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPatientDetailBinding
    private lateinit var phoneAdapter: PhoneNumberAdapter
    private lateinit var contactList: MutableList<String>
    private var mediaPlayer: MediaPlayer? = null
    
    private lateinit var prefs: SharedPreferences
    private lateinit var viewModel: DashboardViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatientDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]
        
        // Menggunakan Context.MODE_PRIVATE yang sekarang sudah dikenali
        prefs = getSharedPreferences("TeleconizerGlobalPrefs", Context.MODE_PRIVATE)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val patient = intent.getParcelableExtra<Patient>(EXTRA_PATIENT)
        if (patient != null) {
            bindPatient(patient)
            if (patient.status.equals("DANGER", ignoreCase = true) || 
                patient.status.equals("JATUH", ignoreCase = true)) {
                startAlarm()
            }
            setupButtons(patient)
        } else {
            Toast.makeText(this, "Data pasien error", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupContactList()
    }
    
    private fun setupButtons(patient: Patient) {
        binding.btnBack.setOnClickListener { finish() }
        
        binding.btnStopAlarm.setOnClickListener {
            stopAlarm()
            Toast.makeText(this, "Alarm Dihentikan", Toast.LENGTH_SHORT).show()
        }

        binding.btnDeleteContact?.text = "Hapus Perangkat Ini"
        binding.btnDeleteContact?.setOnClickListener {
             AlertDialog.Builder(this)
                .setTitle("Hapus Perangkat?")
                .setMessage("Apakah anda yakin ingin menghapus ${patient.name} dari daftar?")
                .setPositiveButton("Ya") { _, _ ->
                    val repo = DeviceRepository(this)
                    repo.removePatient(patient.id)
                    
                    Toast.makeText(this, "Perangkat dihapus.", Toast.LENGTH_LONG).show()
                    val intent = Intent(this, DashboardActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun setupContactList() {
        val savedSet = prefs.getStringSet("GlobalContacts", mutableSetOf()) ?: mutableSetOf()
        contactList = savedSet.toMutableList()

        phoneAdapter = PhoneNumberAdapter(
            contacts = contactList,
            onEdit = { newNumber, index -> updateContact(index, newNumber) },
            onDelete = { index -> deleteContact(index) }
        )
        
        binding.rvPhoneNumbers.layoutManager = LinearLayoutManager(this)
        binding.rvPhoneNumbers.adapter = phoneAdapter
        
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
        prefs.edit().putStringSet("GlobalContacts", contactList.toSet()).apply()
        binding.btnCall.isEnabled = contactList.isNotEmpty()
    }

    private fun bindPatient(patient: Patient) {
        binding.toolbar.title = patient.name
        binding.tvStatus.text = patient.status.uppercase()
        
        val isDanger = patient.status.equals("DANGER", ignoreCase = true) || 
                       patient.status.equals("JATUH", ignoreCase = true)
                       
        binding.tvStatus.setTextColor(if(isDanger) getColor(android.R.color.holo_red_dark) else getColor(android.R.color.holo_green_dark))

        binding.tvCoordinates.text = "Lat: ${patient.latitude}\nLon: ${patient.longitude}"

        binding.btnOpenInMaps.setOnClickListener {
            if (patient.latitude != 0.0 && patient.longitude != 0.0) {
                val uri = Uri.parse("geo:${patient.latitude},${patient.longitude}?q=${patient.latitude},${patient.longitude}(${patient.name})")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setPackage("com.google.android.apps.maps")
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://googleusercontent.com/maps.google.com/maps?q=${patient.latitude},${patient.longitude}")))
                }
            } else {
                Toast.makeText(this, "Lokasi belum tersedia (0.0)", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.btnCall.setOnClickListener {
            if (contactList.isEmpty()) {
                Toast.makeText(this, "Tidak ada nomor darurat!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val numbers = contactList.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle("Pilih Nomor")
                .setItems(numbers) { _, which ->
                    val selected = numbers[which]
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$selected")
                    }
                    startActivity(intent)
                }
                .show()
        }
    }

    private fun startAlarm() {
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
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_patient_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            R.id.action_back -> { finish(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    companion object {
        const val EXTRA_PATIENT = "extra_patient"
    }
}
