package com.teleconizer.app.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
// Removed Compose imports for AGP 4.1.3 compatibility
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.teleconizer.app.R
import com.teleconizer.app.databinding.ActivityMainBinding
import com.teleconizer.app.data.model.EmergencyContact
import com.teleconizer.app.data.model.SensorData
import com.teleconizer.app.data.model.DeviceStatus
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
            Toast.makeText(this, "Location permission required", Toast.LENGTH_LONG).show()
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
        
        // Start periodic data fetching
        viewModel.startDataPolling()

        // Initialize status values - will be updated by real data
        binding.tvStatus.text = "status: Loading..."
        binding.tvMpu6050.text = "mpu6050: Loading..."
        binding.tvGpsLat.text = "gpsLat: Loading..."
        binding.tvGpsLong.text = "gpsLong: Loading..."
        binding.tvPatientStatus.text = "patientStatus: Loading..."
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
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.btnCallEmergency.setOnClickListener {
            viewModel.getLatestEmergencyContact()?.let { contact ->
                makePhoneCall(contact.phoneNumber)
            } ?: run {
                Toast.makeText(this, "No emergency contact saved", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnOpenInMaps.setOnClickListener {
            // Open Google Maps at the dummy/static coordinates
            val gmmIntentUri = Uri.parse("geo:-6.200000,106.816666?q=-6.200000,106.816666")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
        }
    }
    
    private fun observeViewModel() {
        viewModel.sensorData.observe(this) { sensorData ->
            updateSensorStatus(sensorData)
            updateMapLocation(sensorData)
        }
        
        viewModel.emergencyContact.observe(this) { contact ->
            contact?.let {
                binding.tvEmergencyContact.text = "Emergency: ${it.name} - ${it.phoneNumber}"
            }
        }
        
        viewModel.isDangerDetected.observe(this) { isDanger ->
            if (isDanger) {
                triggerEmergencyAlarm()
            }
        }
        
        // Observe device status from Realtime Database
        dashboardViewModel.deviceStatus.observe(this) { deviceStatus ->
            deviceStatus?.let {
                updateDeviceStatusUI(it)
            }
        }
    }
    
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // Check permissions
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
        if (::googleMap.isInitialized) {
            googleMap.isMyLocationEnabled = true
            googleMap.uiSettings.isMyLocationButtonEnabled = true
            googleMap.uiSettings.isZoomControlsEnabled = true
        }
    }
    
    private fun updateSensorStatus(sensorData: SensorData?) {
        sensorData?.let { data ->
            when (data.status.lowercase()) {
                "danger" -> {
                    binding.tvStatus.text = getString(R.string.status_danger)
                    binding.tvStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                    binding.layoutStatus.setBackgroundColor(getColor(android.R.color.holo_red_light))
                }
                "normal" -> {
                    binding.tvStatus.text = getString(R.string.status_safe)
                    binding.tvStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                    binding.layoutStatus.setBackgroundColor(getColor(android.R.color.holo_green_light))
                }
                else -> {
                    binding.tvStatus.text = "Status: ${data.status}"
                    binding.tvStatus.setTextColor(getColor(android.R.color.black))
                    binding.layoutStatus.setBackgroundColor(getColor(android.R.color.white))
                }
            }
        }
    }
    
    private fun updateMapLocation(sensorData: SensorData?) {
        sensorData?.let { data ->
            if (::googleMap.isInitialized) {
                val patientLocation = LatLng(data.lat, data.lon)
                
                // Clear existing markers
                googleMap.clear()
                
                // Add patient location marker
                googleMap.addMarker(
                    MarkerOptions()
                        .position(patientLocation)
                        .title("Patient Location")
                )
                
                // Move camera to patient location
                googleMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(patientLocation, 16f)
                )
                
                // Set satellite view
                googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
                
                // If danger detected, zoom in more
                if (data.status.lowercase() == "danger") {
                    googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(patientLocation, 18f)
                    )
                }
            }
        }
        // Update plain lat/lon text as well
        val text = if (sensorData != null) {
            "Lat: ${sensorData.lat}, Lon: ${sensorData.lon}"
        } else {
            "Lat: -, Lon: -"
        }
        binding.tvLatLon.text = text
    }
    
    private fun updateDeviceStatusUI(deviceStatus: DeviceStatus) {
        // Update UI with real-time device status from Realtime Database
        runOnUiThread {
            binding.tvStatus.text = "status: ${deviceStatus.status}"
            binding.tvGpsLat.text = "gpsLat: ${deviceStatus.latitude}"
            binding.tvGpsLong.text = "gpsLong: ${deviceStatus.longitude}"
            binding.tvPatientStatus.text = "patientStatus: ${deviceStatus.status}"
            
            // Update lat/lon display with null-safe handling
            val latText = deviceStatus.latitude?.toString() ?: "N/A"
            val lonText = deviceStatus.longitude?.toString() ?: "N/A"
            binding.tvLatLon.text = "Lat: $latText, Lon: $lonText"
            
            // Update map with new location (only if coordinates are not null)
            if (::googleMap.isInitialized && deviceStatus.latitude != null && deviceStatus.longitude != null) {
                val deviceLocation = LatLng(deviceStatus.latitude!!, deviceStatus.longitude!!)
                
                // Clear existing markers
                googleMap.clear()
                
                // Add device location marker
                googleMap.addMarker(
                    MarkerOptions()
                        .position(deviceLocation)
                        .title("Device Location")
                )
                
                // Move camera to device location
                googleMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(deviceLocation, 16f)
                )
                
                // Set satellite view
                googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
                
                // If danger detected, zoom in more
                if (deviceStatus.status?.lowercase() == "danger") {
                    googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(deviceLocation, 18f)
                    )
                }
            }
            
            android.util.Log.d("MainActivity", "Updated UI with device status: ${deviceStatus.status} at ${deviceStatus.latitude}, ${deviceStatus.longitude}")
        }
    }

    // Removed Compose function - replaced with XML TextViews
    
    private fun triggerEmergencyAlarm() {
        // Start alarm service
        val intent = Intent(this, AlarmService::class.java)
        startService(intent)
        
        // Show emergency notification
        Toast.makeText(this, "DANGER DETECTED! Emergency alert activated!", Toast.LENGTH_LONG).show()
        
        // Vibrate device
        val vibrator = getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator
        vibrator.vibrate(longArrayOf(0, 1000, 500, 1000), 0)
    }
    
    private fun makePhoneCall(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_CALL)
        intent.data = Uri.parse("tel:$phoneNumber")
        
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Phone permission required", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopDataPolling()
    }
}

