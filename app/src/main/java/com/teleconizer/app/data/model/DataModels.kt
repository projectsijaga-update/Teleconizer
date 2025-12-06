package com.teleconizer.app.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

// Model Kontak Baru (Nama & Nomor)
@Parcelize
data class ContactModel(
    val name: String = "",
    val number: String = ""
) : Parcelable

@Parcelize
data class Patient(
    val id: String,
    val name: String,
    val macAddress: String,
    val status: String,
    val latitude: Double,
    val longitude: Double,
    // Ubah dari List<String> ke List<ContactModel>
    val contacts: List<ContactModel> = emptyList() 
) : Parcelable

@Parcelize
data class DeviceStatus(
    val latitude: Double?,
    val longitude: Double?,
    val status: String?,
    val timestamp: Long? = null
) : Parcelable

// Model untuk Firebase Info
data class DeviceInfo(
    val name: String? = null,
    val contacts: List<ContactModel>? = null
)
