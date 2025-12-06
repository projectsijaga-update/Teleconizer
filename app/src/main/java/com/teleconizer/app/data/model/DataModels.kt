package com.teleconizer.app.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

// Model untuk Data Pasien di UI
@Parcelize
data class Patient(
    val id: String,
    val name: String,
    val macAddress: String,
    val status: String,
    val latitude: Double,
    val longitude: Double,
    val contacts: List<ContactModel> = emptyList()
) : Parcelable

// Model untuk Status Device dari Firebase
@Parcelize
data class DeviceStatus(
    val latitude: Double? = 0.0,
    val longitude: Double? = 0.0,
    val status: String? = "OFFLINE",
    val timestamp: Long? = 0L
) : Parcelable

// Model Kontak (Nama & Nomor)
@Parcelize
data class ContactModel(
    val name: String = "",
    val number: String = ""
) : Parcelable

// Model untuk Info User di Firebase
data class DeviceInfo(
    val name: String? = null,
    val contacts: List<ContactModel>? = null
)

// Model untuk MainViewModel (UI Sederhana)
@Parcelize
data class SensorData(
    val status: String,
    val lat: Double,
    val lon: Double,
    val timestamp: String? = null
) : Parcelable

@Entity(tableName = "emergency_contact")
data class EmergencyContact(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val phoneNumber: String
)
