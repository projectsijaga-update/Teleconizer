package com.teleconizer.app.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

// Model untuk Patient (User)
@Parcelize
data class Patient(
    val id: String,
    val name: String,
    val macAddress: String,
    val status: String,
    val latitude: Double,
    val longitude: Double,
    val contacts: List<String> = emptyList()
) : Parcelable

// Model untuk Status Device dari Firebase
@Parcelize
data class DeviceStatus(
    val latitude: Double?,
    val longitude: Double?,
    val status: String?,
    val timestamp: Long? = null
) : Parcelable

// Model untuk Info Tambahan (Kontak) dari Firebase
data class DeviceInfo(
    val name: String? = null,
    val contacts: List<String>? = null
)

// [DITAMBAHKAN] Model SensorData (yang hilang sebelumnya)
@Parcelize
data class SensorData(
    val status: String,
    val lat: Double,
    val lon: Double,
    val timestamp: String? = null
) : Parcelable

// [DITAMBAHKAN] Model EmergencyContact (yang hilang sebelumnya)
@Entity(tableName = "emergency_contact")
data class EmergencyContact(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val phoneNumber: String
)
