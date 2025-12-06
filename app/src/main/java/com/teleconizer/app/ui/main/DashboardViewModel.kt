package com.teleconizer.app.ui.main

import android.app.Application
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

    private val _isAnyPatientInDanger = MutableLiveData<Boolean>()
    val isAnyPatientInDanger: LiveData<Boolean> = _isAnyPatientInDanger

    private val realtimeService = RealtimeDatabaseService()
    private val deviceRepo = DeviceRepository(application)
    
    private var currentList = mutableListOf<Patient>()
    private val listenerJobs = mutableMapOf<String, Job>()

    init {
        loadSavedDevices()
    }

    private fun loadSavedDevices() {
        currentList = deviceRepo.getSavedPatients()
        _patients.value = currentList
        
        // Hentikan listener lama agar tidak duplikat
        listenerJobs.values.forEach { it.cancel() }
        listenerJobs.clear()
        
        // Mulai dengarkan ulang semua device
        currentList.forEach { patient ->
            startListeningToDevice(patient.id, patient.macAddress)
        }
    }

    fun addNewDevice(name: String, mac: String) {
        // 1. Simpan di Lokal (agar muncul di list HP sendiri)
        deviceRepo.addPatient(name, mac)
        
        // 2. Simpan ke Firebase (agar HP lain bisa sync nama ini jika mereka add mac yg sama)
        // Gunakan list kontak kosong untuk inisialisasi
        realtimeService.saveDeviceInfo(mac, name, emptyList())
        
        loadSavedDevices()
    }
    
    fun deleteDevice(patient: Patient) {
        listenerJobs[patient.id]?.cancel()
        listenerJobs.remove(patient.id)
        deviceRepo.removePatient(patient.id)
        loadSavedDevices()
    }

    private fun startListeningToDevice(id: String, macAddress: String) {
        if (listenerJobs.containsKey(id)) return

        val job = viewModelScope.launch {
            // Memanggil fungsi di RealtimeDatabaseService dengan MAC Address
            realtimeService.getStatusUpdates(macAddress).collect { status ->
                if (status != null) {
                    updatePatientData(id, status)
                }
            }
        }
        listenerJobs[id] = job
    }

    private fun updatePatientData(id: String, statusData: DeviceStatus) {
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            val oldData = currentList[index]
            
            // Logika Status Aman/Bahaya
            val rawStatus = statusData.status?.lowercase() ?: "offline"
            val displayStatus = if (rawStatus.contains("jatuh") || rawStatus.contains("danger")) "DANGER" else "SAFE"

            val updatedPatient = oldData.copy(
                status = displayStatus,
                latitude = statusData.latitude ?: oldData.latitude,
                longitude = statusData.longitude ?: oldData.longitude
            )

            currentList[index] = updatedPatient
            _patients.postValue(currentList.toList()) // Update UI

            checkGlobalAlarm()
        }
    }

    private fun checkGlobalAlarm() {
        val danger = currentList.any { it.status == "DANGER" }
        if (_isAnyPatientInDanger.value != danger) {
            _isAnyPatientInDanger.postValue(danger)
        }
    }
}
