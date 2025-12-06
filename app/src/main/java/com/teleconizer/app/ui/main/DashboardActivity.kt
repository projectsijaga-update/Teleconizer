package com.teleconizer.app.ui.main

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import com.teleconizer.app.ui.settings.EmergencyContactActivity
import com.teleconizer.app.service.AlarmService

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var viewModel: DashboardViewModel
    private lateinit var patientAdapter: PatientAdapter

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Dashboard Teleconizer"

        viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibrator = vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val serviceIntent = Intent(this, AlarmService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        setupRecycler()
        observePatients()
        observeAlarm()
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
        }
    }

    private fun observeAlarm() {
        viewModel.isAnyPatientInDanger.observe(this) { isDanger ->
            if (isDanger) {
                triggerEmergencyAlarm()
            } else {
                stopEmergencyAlarm()
            }
        }
    }

    private fun triggerEmergencyAlarm() {
        if (mediaPlayer?.isPlaying == true) return

        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()

            if (vibrator?.hasVibrator() == true) {
                val pattern = longArrayOf(0, 500, 200, 500)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            }
            
            binding.toolbar.setBackgroundColor(getColor(android.R.color.holo_red_dark))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopEmergencyAlarm() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
            }
        } catch (e: Exception) {}
        
        vibrator?.cancel()
        binding.toolbar.setBackgroundColor(getColor(R.color.primary)) 
    }

    // [PERBAIKAN UTAMA] Matikan alarm saat pindah ke halaman lain (Detail Pasien)
    override fun onStop() {
        super.onStop()
        stopEmergencyAlarm()
    }
    
    // Nyalakan lagi saat kembali ke Dashboard jika masih bahaya
    override fun onStart() {
        super.onStart()
        if (viewModel.isAnyPatientInDanger.value == true) {
            triggerEmergencyAlarm()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_dashboard, menu)
        
        val menuTextBlue = ContextCompat.getColor(this, R.color.menu_text_blue)
        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i)
            if (menuItem.itemId == R.id.action_emergency_contact || menuItem.itemId == R.id.action_exit) {
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
        stopEmergencyAlarm()
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
