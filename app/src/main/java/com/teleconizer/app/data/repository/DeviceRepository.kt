package com.teleconizer.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.teleconizer.app.data.model.Patient
import java.util.UUID

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
        // Cek duplikasi MAC
        if (list.none { it.macAddress.equals(macAddress, ignoreCase = true) }) {
            val newPatient = Patient(
                id = UUID.randomUUID().toString(),
                name = name,
                macAddress = macAddress,
                status = "OFFLINE",
                latitude = 0.0,
                longitude = 0.0
            )
            list.add(newPatient)
            saveList(list)
        }
    }
    
    fun removePatient(id: String) {
        val list = getSavedPatients()
        list.removeAll { it.id == id }
        saveList(list)
    }

    private fun saveList(list: List<Patient>) {
        val json = gson.toJson(list)
        prefs.edit().putString(key, json).apply()
    }
}
