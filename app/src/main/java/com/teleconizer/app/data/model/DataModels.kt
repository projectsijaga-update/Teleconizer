package com.teleconizer.app.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
data class Patient(
    val id: String,
    val name: String,
    val macAddress: String,
    val status: String,
    val latitude: Double,
    val longitude: Double,
    // Kita tambahkan list kontak di objek lokal agar mudah diakses
    val contacts: List<String> = emptyList() 
) : Parcelable

@Parcelize
data class DeviceStatus(
    val latitude: Double?,
    val longitude: Double?,
    val status: String?,
    val timestamp: Long? = null
) : Parcelable

// [BARU] Model untuk sinkronisasi Info User di Firebase
data class DeviceInfo(
    val name: String? = null,
    val contacts: List<String>? = null
)
