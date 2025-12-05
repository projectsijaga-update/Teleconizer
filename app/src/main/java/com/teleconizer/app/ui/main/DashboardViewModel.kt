package com.teleconizer.app.ui.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.teleconizer.app.data.model.Patient
import com.teleconizer.app.data.model.DeviceStatus
import com.teleconizer.app.data.realtime.RealtimeDatabaseService
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

	private val _patients = MutableLiveData<List<Patient>>()
	val patients: LiveData<List<Patient>> = _patients

	private val _deviceStatus = MutableLiveData<DeviceStatus?>()
	val deviceStatus: LiveData<DeviceStatus?> = _deviceStatus

	private val _isAliceInDanger = MutableLiveData<Boolean>()
	val isAliceInDanger: LiveData<Boolean> = _isAliceInDanger

	private val realtimeService = RealtimeDatabaseService()

	// Dynamic Zainabun
	private var alicePatient = Patient(
		id = 1,
		name = "Zainabun",
		status = "SAFE",
		latitude = 0.0,
		longitude = 0.0
	)

	// Other dummy patients (optional)
	private val otherPatients = listOf(
		Patient(2, "Laham", "SAFE", -6.202, 106.818),
		Patient(3, "Siti", "SAFE", -6.203, 106.819),
		Patient(4, "Ipin", "SAFE", -6.204, 106.820),
		Patient(5, "Husna", "SAFE", -6.205, 106.821)
	)

	init {
		Log.d("DashboardViewModel", "ViewModel initialized")
		updatePatientList()
		startRealtimeStatusUpdates()
	}

	private fun updatePatientList() {
		val combined = mutableListOf<Patient>()
		combined.add(alicePatient)
		combined.addAll(otherPatients)
		_patients.postValue(combined)
	}

	private fun startRealtimeStatusUpdates() {
		Log.d("DashboardViewModel", "Listening to Firebase...")
		viewModelScope.launch {
			realtimeService.getStatusUpdates().collect { status ->
				if (status != null) {
					Log.d("DashboardViewModel", "✅ Firebase update received: $status")
					_deviceStatus.postValue(status)
					updateAlice(status)
				} else {
					Log.w("DashboardViewModel", "⚠️ Null status from Firebase")
				}
			}
		}
	}

	private fun updateAlice(deviceStatus: DeviceStatus) {
		val newStatus = when (deviceStatus.status?.lowercase()) {
			"aman" -> "SAFE"
			"jatuh" -> "DANGER"
			else -> "SAFE"
		}

		alicePatient = alicePatient.copy(
			status = newStatus,
			latitude = deviceStatus.latitude ?: alicePatient.latitude,
			longitude = deviceStatus.longitude ?: alicePatient.longitude
		)

		updatePatientList()

		updateAlarmState(newStatus)

		Log.d("DashboardViewModel", "✅ Zainabun updated: $alicePatient")
	}

	private fun updateAlarmState(displayStatus: String) {
		val isDanger = displayStatus == "DANGER"
		if (isDanger != (_isAliceInDanger.value == true)) {
			_isAliceInDanger.postValue(isDanger)
			Log.d("AlarmUpdate", "✅ Zainabun alarm changed: $isDanger")
		}
	}
}