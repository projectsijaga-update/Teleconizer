package com.teleconizer.app.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.teleconizer.app.data.model.EmergencyContact
import com.teleconizer.app.data.model.SensorData
import com.teleconizer.app.data.realtime.RealtimeDatabaseService
import com.teleconizer.app.data.repository.DeviceRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val realtimeService = RealtimeDatabaseService()
    // Kita butuh akses ke DeviceRepository untuk mengetahui MAC Address perangkat yang tersimpan
    // Karena DeviceRepository belum diinject, kita inisialisasi manual (untuk sementara)
    // PENTING: Pastikan DeviceRepository.kt sudah ada dan benar.
    private val deviceRepo = com.teleconizer.app.data.repository.DeviceRepository(application)
    
    private val _sensorData = MutableLiveData<SensorData?>()
    val sensorData: LiveData<SensorData?> = _sensorData

    private val _emergencyContact = MutableLiveData<EmergencyContact?>()
    val emergencyContact: LiveData<EmergencyContact?> = _emergencyContact
    
    private val _isDangerDetected = MutableLiveData<Boolean>()
    val isDangerDetected: LiveData<Boolean> = _isDangerDetected
    
    private var pollingJob: Job? = null
    
    init {
        loadEmergencyContact()
    }
    
    fun startDataPolling() {
        // Ambil data device pertama yang tersimpan di aplikasi
        // Karena MainViewModel ini untuk demo single device di halaman depan
        val savedDevices = deviceRepo.getSavedPatients() // Fungsi ini harus ada di DeviceRepository
        
        if (savedDevices.isNotEmpty()) {
            val firstDeviceMac = savedDevices[0].macAddress
            startListeningToFirebase(firstDeviceMac)
        } else {
            // Jika belum ada device, tampilkan status kosong
            _sensorData.postValue(SensorData("Belum ada alat", 0.0, 0.0, null))
        }
    }
    
    private fun startListeningToFirebase(macAddress: String) {
        stopDataPolling()
        
        pollingJob = viewModelScope.launch {
            // Memanggil getStatusUpdates dari RealtimeDatabaseService
            realtimeService.getStatusUpdates(macAddress).collectLatest { status ->
                if (status != null) {
                    val newData = SensorData(
                        status = status.status ?: "OFFLINE",
                        lat = status.latitude ?: 0.0,
                        lon = status.longitude ?: 0.0,
                        timestamp = status.timestamp?.toString()
                    )
                    _sensorData.postValue(newData)
                    
                    val isDanger = newData.status.contains("JATUH", ignoreCase = true) || 
                                   newData.status.contains("DANGER", ignoreCase = true)
                    _isDangerDetected.postValue(isDanger)
                }
            }
        }
    }
    
    fun stopDataPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }
    
    fun saveEmergencyContact(contact: EmergencyContact) {
        _emergencyContact.value = contact
    }

    fun getLatestEmergencyContact(): EmergencyContact? {
        return _emergencyContact.value
    }
    
    private fun loadEmergencyContact() {
        _emergencyContact.value = EmergencyContact(name = "Belum Ada", phoneNumber = "")
    }
}
