package com.teleconizer.app.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.teleconizer.app.data.model.EmergencyContact
import com.teleconizer.app.data.model.SensorData
import com.teleconizer.app.data.repository.TeleconizerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = TeleconizerRepository(application)
    private val apiKey = "SECRET_ESP32_KEY"
    
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
        pollingJob = viewModelScope.launch {
            while (true) {
                try {
                    fetchLatestSensorData()
                    delay(5000) // Poll every 5 seconds
                } catch (e: Exception) {
                    // Handle error silently and continue polling
                    delay(10000) // Wait longer on error
                }
            }
        }
    }
    
    fun stopDataPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }
    
    private suspend fun fetchLatestSensorData() {
        // Post static dummy data for UI testing only (no repository or network calls)
        val dummy = SensorData(
            status = "Normal",
            lat = -6.200000,
            lon = 106.816666,
            timestamp = null
        )
        _sensorData.postValue(dummy)
        val isDanger = dummy.status.lowercase() == "danger"
        _isDangerDetected.postValue(isDanger)
    }
    
    fun saveEmergencyContact(contact: EmergencyContact) {
        viewModelScope.launch {
            repository.saveEmergencyContact(contact)
            loadEmergencyContact()
        }
    }
    
    fun getLatestEmergencyContact(): EmergencyContact? {
        return _emergencyContact.value
    }
    
    private fun loadEmergencyContact() {
        viewModelScope.launch {
            val contact = repository.getLatestEmergencyContact()
                .first()
            if (contact != null) {
                _emergencyContact.postValue(contact)
            } else {
                // Provide dummy emergency contact when none exists (UI testing)
                _emergencyContact.postValue(
                    EmergencyContact(
                        name = "Emergency Contact",
                        phoneNumber = "081234567890"
                    )
                )
            }
        }
    }
    
    fun refreshData() {
        viewModelScope.launch {
            fetchLatestSensorData()
        }
    }
}

