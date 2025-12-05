package com.teleconizer.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.teleconizer.app.data.model.Patient

class DeviceRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("saved_devices", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "patient_list"

    fun getSavedPatients(): MutableList<Patient> {
        val json = prefs.getString(key, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<Patient>>() {}.type
        return gson.fromJson(json, type)
    }

    fun addPatient(name: String, macAddress: String) {
        val list = getSavedPatients()
        // Cek duplikasi MAC agar tidak double
        if (list.none { it.status == macAddress }) { // Kita gunakan field 'status' sementara untuk simpan MAC atau tambah field baru di Model
            // Agar lebih bersih, mari kita asumsikan status awal OFFLINE, dan kita butuh field MAC di Patient Model.
            // TAPI, untuk mempermudah tanpa ubah Model: Kita simpan MAC di SharedPreferences saja, Patient ID = Hash dari MAC
             val newPatient = Patient(
                id = macAddress.hashCode(),
                name = name,
                status = "OFFLINE", // Status awal
                latitude = 0.0,
                longitude = 0.0
            )
            // Simpan MAC Address di properti khusus jika ada, atau gunakan logic map di ViewModel
            // Untuk solusi cepat saat ini, pastikan ViewModel tahu MAC addressnya.
            list.add(newPatient)
            saveList(list)
        }
    }
    
    // Fungsi Khusus untuk menyimpan data lengkap dengan MAC Address (Disarankan ubah Model Patient tambah val macAddress: String)
    // Tapi kita pakai trik simpan List Wrapper
    data class SavedDevice(val name: String, val macAddress: String)
    
    fun saveDeviceList(devices: List<SavedDevice>) {
        val json = gson.toJson(devices)
        prefs.edit().putString("device_map", json).apply()
    }

    fun getDeviceList(): MutableList<SavedDevice> {
        val json = prefs.getString("device_map", null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<SavedDevice>>() {}.type
        return gson.fromJson(json, type)
    }

    fun removeDevice(macAddress: String) {
        val list = getDeviceList()
        list.removeAll { it.macAddress == macAddress }
        saveDeviceList(list)
    }
}

