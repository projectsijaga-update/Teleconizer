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

    private val _isAnyPatientInDanger = MutableLiveData<Boolean>()
    val isAnyPatientInDanger: LiveData<Boolean> = _isAnyPatientInDanger

    private val realtimeService = RealtimeDatabaseService()
    private val deviceRepo = DeviceRepository(application)
    
    // Kita simpan list di memori untuk manipulasi cepat
    private var currentList = mutableListOf<Patient>()
    
    // Map untuk menyimpan Job listener agar bisa dibatalkan jika perlu
    private val listenerJobs = mutableMapOf<String, Job>()

    init {
        loadSavedDevices()
    }

    private fun loadSavedDevices() {
        currentList = deviceRepo.getSavedPatients()
        _patients.value = currentList
        
        // Mulai listen untuk setiap perangkat yang tersimpan
        currentList.forEach { patient ->
            startListeningToDevice(patient.id, patient.macAddress)
        }
    }

    fun addNewDevice(name: String, mac: String) {
        deviceRepo.addPatient(name, mac)
        // Reload list
        val updatedList = deviceRepo.getSavedPatients()
        
        // Cari item baru
        val newItem = updatedList.find { it.macAddress.equals(mac, ignoreCase = true) }
        newItem?.let { 
            currentList.add(it)
            _patients.value = currentList
            startListeningToDevice(it.id, it.macAddress)
        }
    }
    
    fun deleteDevice(patient: Patient) {
        // Hentikan listener Firebase untuk perangkat ini
        listenerJobs[patient.id]?.cancel()
        listenerJobs.remove(patient.id)
        
        // Hapus dari penyimpanan
        deviceRepo.removePatient(patient.id)
        currentList.removeAll { it.id == patient.id }
        _patients.value = currentList
    }

    private fun startListeningToDevice(id: String, macAddress: String) {
        // Jangan duplikasi listener
        if (listenerJobs.containsKey(id)) return

        val job = viewModelScope.launch {
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
            
            // Konversi status text
            val newStatusDisplay = when (statusData.status?.lowercase()) {
                "aman" -> "SAFE"
                "jatuh" -> "DANGER"
                else -> statusData.status ?: "UNKNOWN"
            }

            val updatedPatient = oldData.copy(
                status = newStatusDisplay,
                latitude = statusData.latitude ?: oldData.latitude,
                longitude = statusData.longitude ?: oldData.longitude
            )

            currentList[index] = updatedPatient
            _patients.postValue(currentList.toList()) // Trigger UI update

            checkGlobalAlarm()
        }
    }

    private fun checkGlobalAlarm() {
        // Cek jika ADA SATU SAJA pasien yang statusnya DANGER
        val danger = currentList.any { 
            it.status.equals("DANGER", ignoreCase = true) || 
            it.status.equals("JATUH", ignoreCase = true) 
        }
        
        if (_isAnyPatientInDanger.value != danger) {
            _isAnyPatientInDanger.postValue(danger)
        }
    }
}
