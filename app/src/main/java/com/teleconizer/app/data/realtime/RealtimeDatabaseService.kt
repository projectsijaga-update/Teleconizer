package com.teleconizer.app.data.realtime

import android.util.Log
import com.google.firebase.database.*
import com.teleconizer.app.data.model.ContactModel
import com.teleconizer.app.data.model.DeviceInfo
import com.teleconizer.app.data.model.DeviceStatus
import com.teleconizer.app.data.model.Patient
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class RealtimeDatabaseService {

    private val db = FirebaseDatabase.getInstance("https://sijaga-95af3-default-rtdb.asia-southeast1.firebasedatabase.app/")

    // [FUNGSI YANG HILANG DITAMBAHKAN KEMBALI]
    // Fungsi ini untuk mendengarkan semua device sekaligus (Solusi User Hilang)
    fun getGlobalDeviceList(): Flow<List<Patient>> = callbackFlow {
        val ref = db.getReference("devices")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val patients = mutableListOf<Patient>()
                
                for (deviceSnap in snapshot.children) {
                    try {
                        val mac = deviceSnap.key ?: continue
                        
                        // Ambil Info
                        val infoSnap = deviceSnap.child("info")
                        val name = infoSnap.child("name").getValue(String::class.java) ?: continue 
                        
                        val typeIndicator = object : GenericTypeIndicator<List<ContactModel>>() {}
                        val contacts = infoSnap.child("contacts").getValue(typeIndicator) ?: emptyList()

                        // Ambil Status dengan Safe Parsing
                        val statusSnap = deviceSnap.child("status")
                        
                        fun safeDouble(key: String): Double {
                            val v = statusSnap.child(key).value
                            return when (v) {
                                is Double -> v
                                is Long -> v.toDouble()
                                is String -> v.toDoubleOrNull() ?: 0.0
                                else -> 0.0
                            }
                        }

                        val statusStr = statusSnap.child("status").getValue(String::class.java) ?: "OFFLINE"
                        val lat = safeDouble("latitude")
                        val lon = safeDouble("longitude")
                        
                        patients.add(Patient(
                            id = mac,
                            name = name,
                            macAddress = mac,
                            status = statusStr,
                            latitude = lat,
                            longitude = lon,
                            contacts = contacts
                        ))
                        
                    } catch (e: Exception) {
                        Log.e("RealtimeDB", "Error parsing device: ${e.message}")
                    }
                }
                trySend(patients)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun getStatusUpdates(macAddress: String): Flow<DeviceStatus?> = callbackFlow {
        val cleanMac = macAddress.replace(":", "").uppercase()
        val path = "devices/$cleanMac/status"
        val ref = db.getReference(path)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    if (!snapshot.exists()) {
                        trySend(null)
                        return
                    }
                    val statusVal = snapshot.child("status").value?.toString() ?: "OFFLINE"
                    
                    fun safeDouble(k: String): Double = snapshot.child(k).value.toString().toDoubleOrNull() ?: 0.0
                    fun safeLong(k: String): Long = snapshot.child(k).value.toString().toLongOrNull() ?: 0L

                    val lat = safeDouble("latitude")
                    val lon = safeDouble("longitude")
                    val ts = safeLong("timestamp")

                    trySend(DeviceStatus(lat, lon, statusVal, ts))
                } catch (e: Exception) { }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun getDeviceInfo(macAddress: String): Flow<DeviceInfo?> = callbackFlow {
        val cleanMac = macAddress.replace(":", "").uppercase()
        val path = "devices/$cleanMac/info"
        val ref = db.getReference(path)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val name = snapshot.child("name").getValue(String::class.java)
                    val typeIndicator = object : GenericTypeIndicator<List<ContactModel>>() {}
                    val contacts = snapshot.child("contacts").getValue(typeIndicator) ?: emptyList()
                    trySend(DeviceInfo(name, contacts))
                } catch (e: Exception) {}
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun saveDeviceInfo(macAddress: String, name: String, contacts: List<ContactModel>) {
        val cleanMac = macAddress.replace(":", "").uppercase()
        val path = "devices/$cleanMac/info"
        db.getReference(path).setValue(DeviceInfo(name, contacts))
    }

    fun deleteDeviceInfo(macAddress: String) {
        val cleanMac = macAddress.replace(":", "").uppercase()
        db.getReference("devices/$cleanMac/info").removeValue()
    }
}
