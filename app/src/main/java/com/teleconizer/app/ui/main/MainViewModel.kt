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
    
    // Gunakan Repository & Service yang sama dengan Dashboard agar sinkron
    private val deviceRepo = DeviceRepository(application)
    private val realtimeService = RealtimeDatabaseService()
    
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
        // Ambil perangkat pertama yang tersimpan untuk ditampilkan di Main Activity
        val devices = deviceRepo.getSavedPatients()
        if (devices.isNotEmpty()) {
            val firstDeviceMac = devices[0].macAddress
            startListeningToFirebase(firstDeviceMac)
        }
    }
    
    private fun startListeningToFirebase(macAddress: String) {
        stopDataPolling() // Hentikan job lama jika ada
        
        pollingJob = viewModelScope.launch {
            // Dengarkan data Realtime dari Firebase (Bukan Dummy!)
            realtimeService.getStatusUpdates(macAddress).collectLatest { status ->
                if (status != null) {
                    // Konversi DeviceStatus (Firebase) ke SensorData (UI)
                    val newData = SensorData(
                        status = status.status ?: "OFFLINE",
                        lat = status.latitude ?: 0.0,
                        lon = status.longitude ?: 0.0,
                        timestamp = status.timestamp?.toString()
                    )
                    
                    _sensorData.postValue(newData)
                    
                    // Cek Bahaya
                    val isDanger = newData.status.equals("JATUH", ignoreCase = true) || 
                                   newData.status.equals("DANGER", ignoreCase = true)
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
        // Simpan ke LiveData sementara (atau implementasikan simpan ke Prefs global jika perlu)
        _emergencyContact.value = contact
    }

    fun getLatestEmergencyContact(): EmergencyContact? {
        return _emergencyContact.value
    }
    
    private fun loadEmergencyContact() {
        // Load dummy awal atau dari Prefs jika sudah diimplementasikan
        _emergencyContact.value = EmergencyContact(name = "Belum Ada", phoneNumber = "")
    }
}
