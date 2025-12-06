package com.teleconizer.app.ui.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.teleconizer.app.R
import com.teleconizer.app.databinding.ActivityDashboardBinding
import com.teleconizer.app.ui.login.LoginActivity
import com.teleconizer.app.service.AlarmService

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var viewModel: DashboardViewModel
    private lateinit var patientAdapter: PatientAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Dashboard Teleconizer"

        viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]

        // [PENTING] Nyalakan Service Pemantau Latar Belakang
        val serviceIntent = Intent(this, AlarmService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        setupRecycler()
        observePatients()
        // HAPUS observeAlarm() - Biarkan Service yang menangani alarm
    }

    private fun setupRecycler() {
        patientAdapter = PatientAdapter { patient ->
            val intent = Intent(this, PatientDetailActivity::class.java)
            intent.putExtra(PatientDetailActivity.EXTRA_PATIENT, patient)
            startActivity(intent)
        }

        binding.recyclerPatients.apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = patientAdapter
        }
    }

    private fun observePatients() {
        viewModel.patients.observe(this) { list ->
            patientAdapter.submitList(list)
            
            // Opsional: Ubah warna toolbar jika ada bahaya (hanya visual)
            val anyDanger = list.any { 
                it.status.contains("JATUH", true) || it.status.contains("DANGER", true)
            }
            if (anyDanger) {
                binding.toolbar.setBackgroundColor(getColor(android.R.color.holo_red_dark))
            } else {
                binding.toolbar.setBackgroundColor(getColor(R.color.primary))
            }
        }
    }

    // [DIHAPUS: triggerEmergencyAlarm, stopEmergencyAlarm, onStop, onStart]
    // Semua logika suara dan getaran sudah ditangani oleh AlarmService.

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_dashboard, menu)
        
        val menuTextBlue = ContextCompat.getColor(this, R.color.menu_text_blue)
        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i)
            // Hapus action_emergency_contact karena sudah dihapus dari XML
            if (menuItem.itemId == R.id.action_exit) {
                val title = menuItem.title ?: ""
                val spannableTitle = android.text.SpannableString(title)
                spannableTitle.setSpan(
                    android.text.style.ForegroundColorSpan(menuTextBlue),
                    0,
                    title.length,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                menuItem.title = spannableTitle
            }
        }

        val addDeviceItem = menu.add(0, 101, 0, "Add Device")
        addDeviceItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        addDeviceItem.setIcon(android.R.drawable.ic_input_add)
        
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            101 -> { 
                showAddDeviceDialog()
                true
            }
            R.id.action_exit -> {
                logoutUser()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAddDeviceDialog() {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(60, 40, 60, 10)

        val nameInput = EditText(this)
        nameInput.hint = "Nama Pasien (Contoh: Kakek)"
        layout.addView(nameInput)

        val macInput = EditText(this)
        macInput.hint = "MAC Address (Lihat di Serial Monitor ESP32)"
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 20, 0, 0)
        macInput.layoutParams = params
        layout.addView(macInput)

        AlertDialog.Builder(this)
            .setTitle("Tambah Perangkat Baru")
            .setView(layout)
            .setPositiveButton("Simpan") { _, _ ->
                val name = nameInput.text.toString().trim()
                val mac = macInput.text.toString().trim()
                
                if (name.isNotEmpty() && mac.isNotEmpty()) {
                    viewModel.addNewDevice(name, mac)
                    Toast.makeText(this, "Perangkat berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Mohon isi semua kolom.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun logoutUser() {
        // Matikan alarm via Service sebelum logout
        val stopIntent = Intent(this, AlarmService::class.java)
        stopIntent.action = AlarmService.ACTION_STOP_ALARM
        startService(stopIntent)
        
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
        }
        
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
