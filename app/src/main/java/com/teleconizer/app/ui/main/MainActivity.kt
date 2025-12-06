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
import com.teleconizer.app.data.model.DeviceStatus
import com.teleconizer.app.data.model.EmergencyContact
import com.teleconizer.app.data.model.SensorData
import com.teleconizer.app.databinding.ActivityMainBinding
import com.teleconizer.app.service.AlarmService

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            setupMap()
        } else {
            Toast.makeText(this, "Izin lokasi diperlukan", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupViewModel()
        setupLocationClient()
        setupMapFragment()
        setupClickListeners()
        observeViewModel()
        
        viewModel.startDataPolling()

        binding.tvStatus.text = "Status: Memuat..."
        binding.tvMpu6050.text = "Sensor: Memuat..."
    }
    
    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        dashboardViewModel = ViewModelProvider(this)[DashboardViewModel::class.java]
    }
    
    private fun setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }
    
    private fun setupMapFragment() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }
    
    private fun setupClickListeners() {
        binding.btnSaveContact.setOnClickListener {
            val name = binding.etContactName.text.toString().trim()
            val phoneNumber = binding.etContactPhone.text.toString().trim()
            
            if (name.isNotEmpty() && phoneNumber.isNotEmpty()) {
                val contact = EmergencyContact(
                    name = name,
                    phoneNumber = phoneNumber
                )
                viewModel.saveEmergencyContact(contact)
                Toast.makeText(this, "Kontak tersimpan", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Mohon isi semua bidang", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.btnCallEmergency.setOnClickListener {
            viewModel.getLatestEmergencyContact()?.let { contact ->
                makePhoneCall(contact.phoneNumber)
            } ?: run {
                Toast.makeText(this, "Belum ada kontak darurat", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnOpenInMaps.setOnClickListener {
            val gmmIntentUri = Uri.parse("geo:-6.200000,106.816666?q=-6.200000,106.816666")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            try {
                startActivity(mapIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Aplikasi Maps tidak ditemukan", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun observeViewModel() {
        viewModel.sensorData.observe(this) { sensorData ->
            updateSensorStatus(sensorData)
            updateMapLocation(sensorData)
        }
        
        viewModel.emergencyContact.observe(this) { contact ->
            contact?.let {
                binding.tvEmergencyContact.text = "Darurat: ${it.name} - ${it.phoneNumber}"
            }
        }
        
        viewModel.isDangerDetected.observe(this) { isDanger ->
            if (isDanger) {
                triggerEmergencyAlarm()
            }
        }
    }
    
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermissions()
            return
        }
        setupMap()
    }
    
    private fun requestLocationPermissions() {
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
    
    private fun setupMap() {
        try {
            if (::googleMap.isInitialized) {
                googleMap.isMyLocationEnabled = true
                googleMap.uiSettings.isMyLocationButtonEnabled = true
                googleMap.uiSettings.isZoomControlsEnabled = true
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
    
    private fun updateSensorStatus(sensorData: SensorData?) {
        sensorData?.let { data ->
            if (data.status.lowercase() == "danger") {
                binding.tvStatus.text = "BAHAYA"
                binding.tvStatus.setTextColor(getColor(android.R.color.holo_red_dark))
            } else {
                binding.tvStatus.text = "AMAN"
                binding.tvStatus.setTextColor(getColor(android.R.color.holo_green_dark))
            }
        }
    }
    
    private fun updateMapLocation(sensorData: SensorData?) {
        sensorData?.let { data ->
            if (::googleMap.isInitialized) {
                val loc = LatLng(data.lat, data.lon)
                googleMap.clear()
                googleMap.addMarker(MarkerOptions().position(loc).title("Lokasi Pasien"))
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 16f))
            }
            binding.tvLatLon.text = "Lat: ${data.lat}, Lon: ${data.lon}"
        }
    }
    
    private fun triggerEmergencyAlarm() {
        val intent = Intent(this, AlarmService::class.java)
        startService(intent)
        Toast.makeText(this, "BAHAYA TERDETEKSI!", Toast.LENGTH_LONG).show()
    }
    
    private fun makePhoneCall(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_CALL)
        intent.data = Uri.parse("tel:$phoneNumber")
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Izin telepon diperlukan", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopDataPolling()
    }
}
