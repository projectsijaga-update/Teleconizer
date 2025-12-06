package com.teleconizer.app.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.teleconizer.app.R
// [PERBAIKAN] Hanya import model yang diperlukan, hapus duplikat
import com.teleconizer.app.data.model.EmergencyContact
import com.teleconizer.app.data.model.SensorData
import com.teleconizer.app.databinding.ActivityMainBinding
import com.teleconizer.app.service.AlarmService

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) setupMap()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        
        setupClickListeners()
        observeViewModel()
        
        viewModel.startDataPolling()
    }

    private fun setupClickListeners() {
        binding.btnSaveContact.setOnClickListener {
            val name = binding.etContactName.text.toString()
            val phone = binding.etContactPhone.text.toString()
            if (name.isNotEmpty() && phone.isNotEmpty()) {
                viewModel.saveEmergencyContact(EmergencyContact(name = name, phoneNumber = phone))
                Toast.makeText(this, "Tersimpan", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.btnCallEmergency.setOnClickListener {
            val contact = viewModel.getLatestEmergencyContact()
            if (contact != null && contact.phoneNumber.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${contact.phoneNumber}"))
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Izin panggilan dibutuhkan", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Kontak darurat belum diset", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun observeViewModel() {
        viewModel.sensorData.observe(this) { data ->
            data?.let {
                binding.tvStatus.text = if(it.status.contains("JATUH", true)) "BAHAYA" else "AMAN"
                binding.tvLatLon.text = "${it.lat}, ${it.lon}"
                
                if (it.status.contains("JATUH", true) || it.status.contains("DANGER", true)) {
                    binding.tvStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                    // Zoom map ke lokasi bahaya
                    if (::googleMap.isInitialized) {
                        val loc = LatLng(it.lat, it.lon)
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 18f))
                    }
                } else {
                    binding.tvStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                }
            }
        }

        viewModel.isDangerDetected.observe(this) { isDanger ->
            if (isDanger) {
                val intent = Intent(this, AlarmService::class.java)
                startService(intent)
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            setupMap()
        } else {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }
    
    private fun setupMap() {
        try {
            if (::googleMap.isInitialized) {
                googleMap.isMyLocationEnabled = true
                googleMap.uiSettings.isZoomControlsEnabled = true
            }
        } catch (e: SecurityException) {}
    }
    
    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopDataPolling()
    }
}
