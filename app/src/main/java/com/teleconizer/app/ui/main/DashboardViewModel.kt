package com.teleconizer.app.ui.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.teleconizer.app.data.model.DeviceStatus
import com.teleconizer.app.data.model.Patient
import com.teleconizer.app.data.realtime.RealtimeDatabaseService
import com.teleconizer.app.data.repository.DeviceRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val _patients = MutableLiveData<List<Patient>>()
    val patients: LiveData<List<Patient>> = _patients

    private val _isAliceInDanger = MutableLiveData<Boolean>() // Global Alarm
    val isAliceInDanger: LiveData<Boolean> = _isAliceInDanger

    private val realtimeService = RealtimeDatabaseService()
    private val repository = DeviceRepository(application)
    
    // Map untuk menyimpan Job listener per device (Key: Mac Address)
    private val listenerJobs = mutableMapOf<String, Job>()
    
    // List internal untuk memantau status terkini
    private var currentPatientList = mutableListOf<Patient>()
    // Map untuk menghubungkan Patient ID ke MAC Address
    private val patientMacMap = mutableMapOf<Int, String>()

    init {
        loadSavedDevices()
    }

    private fun loadSavedDevices() {
        val savedDevices = repository.getDeviceList()
        currentPatientList.clear()
        patientMacMap.clear()
        
        // Bersihkan listener lama
        listenerJobs.values.forEach { it.cancel() }
        listenerJobs.clear()

        savedDevices.forEach { device ->
            val id = device.macAddress.hashCode()
            val patient = Patient(
                id = id,
                name = device.name,
                status = "CONNECTING...",
                latitude = 0.0,
                longitude = 0.0
            )
            currentPatientList.add(patient)
            patientMacMap[id] = device.macAddress
            
            // Mulai listen ke Firebase untuk device ini
            startListeningToDevice(device.macAddress, id)
        }
        _patients.value = currentPatientList
    }

    fun addNewUser(name: String, macAddress: String) {
        val savedList = repository.getDeviceList()
        if (savedList.none { it.macAddress.equals(macAddress, ignoreCase = true) }) {
            savedList.add(DeviceRepository.SavedDevice(name, macAddress))
            repository.saveDeviceList(savedList)
            loadSavedDevices() // Reload semua
        }
    }
    
    fun deleteUser(patientId: Int) {
        val mac = patientMacMap[patientId] ?: return
        repository.removeDevice(mac)
        loadSavedDevices() // Reload dan stop listener
    }

    private fun startListeningToDevice(macAddress: String, patientId: Int) {
        val job = viewModelScope.launch {
            // Pastikan RealtimeDatabaseService.getStatusUpdates menerima parameter MAC Address!
            // Anda perlu update RealtimeDatabaseService agar support parameter macAddress
            realtimeService.getStatusUpdates(macAddress).collect { status ->
                if (status != null) {
                    updatePatientStatus(patientId, status)
                }
            }
        }
        listenerJobs[macAddress] = job
    }

    private fun updatePatientStatus(patientId: Int, deviceStatus: DeviceStatus) {
        val index = currentPatientList.indexOfFirst { it.id == patientId }
        if (index != -1) {
            val oldData = currentPatientList[index]
            
            val newStatusDisplay = when (deviceStatus.status?.lowercase()) {
                "aman" -> "SAFE"
                "jatuh" -> "DANGER"
                else -> deviceStatus.status ?: "UNKNOWN"
            }

            val updatedPatient = oldData.copy(
                status = newStatusDisplay,
                latitude = deviceStatus.latitude ?: oldData.latitude,
                longitude = deviceStatus.longitude ?: oldData.longitude
            )

            currentPatientList[index] = updatedPatient
            _patients.postValue(currentPatientList.toList())
            
            checkGlobalAlarm()
        }
    }
    
    private fun checkGlobalAlarm() {
        val anyDanger = currentPatientList.any { 
            it.status == "DANGER" || it.status.equals("jatuh", ignoreCase = true) 
        }
        if (_isAliceInDanger.value != anyDanger) {
            _isAliceInDanger.postValue(anyDanger)
        }
    }
}
