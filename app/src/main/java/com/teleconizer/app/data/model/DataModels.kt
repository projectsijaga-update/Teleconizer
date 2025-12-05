package com.teleconizer.app.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
data class SensorData(
    val status: String,
    val lat: Double,
    val lon: Double,
    val timestamp: String? = null
) : Parcelable

data class ApiResponse<T>(
    val data: T? = null,
    val error: String? = null,
    val message: String? = null
)

@Entity(tableName = "emergency_contact")
data class EmergencyContact(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val phoneNumber: String
)

@Parcelize
data class Patient(
    val id: String,          // Pastikan ini String
    val name: String,
    val macAddress: String,  // Field wajib untuk multi-user
    val status: String,
    val latitude: Double,
    val longitude: Double,
    val lastUpdate: Long = 0
) : Parcelable

@Parcelize
data class DeviceStatus(
    val latitude: Double?,
    val longitude: Double?,
    val status: String?,
    val timestamp: Long? = null
) : Parcelable
