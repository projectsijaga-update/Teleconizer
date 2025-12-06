package com.teleconizer.app.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.teleconizer.app.data.model.Patient
import com.teleconizer.app.data.realtime.RealtimeDatabaseService
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val _patients = MutableLiveData<List<Patient>>()
    val patients: LiveData<List<Patient>> = _patients

    private val _isAnyPatientInDanger = MutableLiveData<Boolean>()
    val isAnyPatientInDanger: LiveData<Boolean> = _isAnyPatientInDanger

    private val realtimeService = RealtimeDatabaseService()
    
    init {
        startListeningToCloud()
    }

    private fun startListeningToCloud() {
        viewModelScope.launch {
            // Mendengarkan data global langsung dari Firebase
            realtimeService.getGlobalDeviceList().collect { deviceList ->
                // Update UI
                _patients.postValue(deviceList)
                
                // Cek Alarm
                val danger = deviceList.any { 
                    it.status.contains("JATUH", true) || it.status.contains("DANGER", true) 
                }
                if (_isAnyPatientInDanger.value != danger) {
                    _isAnyPatientInDanger.postValue(danger)
                }
            }
        }
    }

    fun addNewDevice(name: String, mac: String) {
        // Cukup tulis ke Firebase. 
        // Listener 'getGlobalDeviceList' di atas akan otomatis mendeteksi perubahan 
        // dan memperbarui UI tanpa kita perlu reload manual.
        realtimeService.saveDeviceInfo(mac, name, emptyList())
    }
    
    fun deleteDevice(patient: Patient) {
        // Cukup hapus dari Firebase. UI akan otomatis update.
        realtimeService.deleteDeviceInfo(patient.macAddress)
    }
}
