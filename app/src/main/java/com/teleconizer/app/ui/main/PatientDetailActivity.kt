package com.teleconizer.app.ui.main

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.teleconizer.app.R
import com.teleconizer.app.data.model.ContactModel
import com.teleconizer.app.data.model.Patient
import com.teleconizer.app.data.realtime.RealtimeDatabaseService
import com.teleconizer.app.data.repository.DeviceRepository
import com.teleconizer.app.databinding.ActivityPatientDetailBinding
import kotlinx.coroutines.launch

class PatientDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPatientDetailBinding
    private lateinit var phoneAdapter: PhoneNumberAdapter
    private var contactList = mutableListOf<ContactModel>()
    
    private var mediaPlayer: MediaPlayer? = null
    private val realtimeService = RealtimeDatabaseService()
    private lateinit var currentPatient: Patient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatientDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val patientData = intent.getParcelableExtra<Patient>(EXTRA_PATIENT)
        if (patientData != null) {
            currentPatient = patientData
            bindUI(currentPatient)
            startSyncingContacts()
            
            if (currentPatient.status == "DANGER" || currentPatient.status.equals("JATUH", true)) {
                startAlarm()
            }
        } else {
            Toast.makeText(this, "Error memuat data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupButtons()
        setupRecycler()
    }

    private fun bindUI(patient: Patient) {
        binding.toolbar.title = patient.name
        binding.tvStatus.text = patient.status.uppercase()
        val isDanger = patient.status.equals("DANGER", true) || patient.status.equals("JATUH", true)
        binding.tvStatus.setTextColor(if(isDanger) getColor(android.R.color.holo_red_dark) else getColor(android.R.color.holo_green_dark))
        binding.tvCoordinates.text = "Lat: ${patient.latitude}\nLon: ${patient.longitude}"
    }

    private fun startSyncingContacts() {
        lifecycleScope.launch {
            realtimeService.getDeviceInfo(currentPatient.macAddress).collect { info ->
                if (info != null && info.contacts != null) {
                    contactList.clear()
                    contactList.addAll(info.contacts)
                    phoneAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun setupRecycler() {
        phoneAdapter = PhoneNumberAdapter(
            contacts = contactList,
            onEdit = { contact, idx -> showEditContactDialog(contact, idx) },
            onDelete = { idx -> deleteContact(idx) }
        )
        binding.rvPhoneNumbers.layoutManager = LinearLayoutManager(this)
        binding.rvPhoneNumbers.adapter = phoneAdapter
    }

    private fun saveContactsToFirebase() {
        realtimeService.saveDeviceInfo(currentPatient.macAddress, currentPatient.name, contactList)
    }

    private fun showAddContactDialog() {
        showContactDialog(null, -1)
    }

    private fun showEditContactDialog(contact: ContactModel, index: Int) {
        showContactDialog(contact, index)
    }

    private fun showContactDialog(existingContact: ContactModel?, index: Int) {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val etName = EditText(this)
        etName.hint = "Nama Kontak (Misal: Ayah)"
        layout.addView(etName)

        val etNumber = EditText(this)
        etNumber.hint = "Nomor HP (0812...)"
        etNumber.inputType = android.text.InputType.TYPE_CLASS_PHONE
        layout.addView(etNumber)

        if (existingContact != null) {
            etName.setText(existingContact.name)
            etNumber.setText(existingContact.number)
        }

        val title = if (existingContact == null) "Tambah Kontak" else "Edit Kontak"

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(layout)
            .setPositiveButton("Simpan") { _, _ ->
                val name = etName.text.toString().trim()
                val number = etNumber.text.toString().trim()

                if (name.isNotEmpty() && number.isNotEmpty()) {
                    val newContact = ContactModel(name, number)
                    if (index == -1) {
                        contactList.add(newContact)
                    } else {
                        contactList[index] = newContact
                    }
                    saveContactsToFirebase()
                    phoneAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "Nama dan Nomor harus diisi", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteContact(index: Int) {
        if (index in contactList.indices) {
            contactList.removeAt(index)
            saveContactsToFirebase()
            phoneAdapter.notifyItemRemoved(index)
        }
    }

    private fun setupButtons() {
        binding.btnOpenInMaps.setOnClickListener {
            val uri = Uri.parse("geo:${currentPatient.latitude},${currentPatient.longitude}?q=${currentPatient.latitude},${currentPatient.longitude}(${currentPatient.name})")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")
            try { startActivity(intent) } catch (e: Exception) { 
                Toast.makeText(this, "Maps tidak ditemukan", Toast.LENGTH_SHORT).show() 
            }
        }

        binding.btnAddContact.setOnClickListener {
            showAddContactDialog()
        }

        binding.btnCall.setOnClickListener {
            if (contactList.isEmpty()) {
                Toast.makeText(this, "Belum ada kontak darurat", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val items = contactList.map { "${it.name} (${it.number})" }.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle("Pilih Kontak")
                .setItems(items) { _, w ->
                    val number = contactList[w].number
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
                }
                .show()
        }

        binding.btnStopAlarm.setOnClickListener {
            stopAlarm()
            Toast.makeText(this, "Alarm Matikan", Toast.LENGTH_SHORT).show()
        }

        binding.btnDeleteContact.setOnClickListener {
             AlertDialog.Builder(this)
                .setTitle("Hapus User?")
                .setMessage("Hapus ${currentPatient.name} dari daftar aplikasi? Data di cloud juga akan dihapus.")
                .setPositiveButton("Ya") { _, _ ->
                    val repo = DeviceRepository(this)
                    repo.removePatient(currentPatient.id)
                    realtimeService.deleteDeviceInfo(currentPatient.macAddress)

                    val intent = Intent(this, DashboardActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Batal", null).show()
        }
    }

    private fun startAlarm() {
        try {
             if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound)
                mediaPlayer?.isLooping = true
                mediaPlayer?.start()
            }
        } catch (e: Exception) {}
    }

    private fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }

    companion object {
        const val EXTRA_PATIENT = "extra_patient"
    }
}
