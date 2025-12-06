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
    private val deviceRepo = DeviceRepository(application)
    
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
        val savedDevices = deviceRepo.getSavedPatients()
        if (savedDevices.isNotEmpty()) {
            startListeningToFirebase(savedDevices[0].macAddress)
        } else {
            _sensorData.postValue(SensorData("Belum ada alat", 0.0, 0.0, null))
        }
    }
    
    private fun startListeningToFirebase(macAddress: String) {
        stopDataPolling()
        pollingJob = viewModelScope.launch {
            // [PERBAIKAN] Mengakses properti nullable dengan aman
            realtimeService.getStatusUpdates(macAddress).collectLatest { deviceStatus ->
                if (deviceStatus != null) {
                    val statusStr = deviceStatus.status ?: "OFFLINE"
                    val lat = deviceStatus.latitude ?: 0.0
                    val lon = deviceStatus.longitude ?: 0.0
                    val ts = deviceStatus.timestamp?.toString() ?: ""

                    val newData = SensorData(
                        status = statusStr,
                        lat = lat,
                        lon = lon,
                        timestamp = ts
                    )
                    _sensorData.postValue(newData)
                    
                    val isDanger = statusStr.contains("JATUH", ignoreCase = true) || 
                                   statusStr.contains("DANGER", ignoreCase = true)
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
