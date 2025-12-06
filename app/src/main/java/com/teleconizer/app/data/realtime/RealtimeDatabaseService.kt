package com.teleconizer.app.data.realtime

import android.util.Log
import com.google.firebase.database.*
import com.teleconizer.app.data.model.ContactModel
import com.teleconizer.app.data.model.DeviceInfo
import com.teleconizer.app.data.model.DeviceStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class RealtimeDatabaseService {

    // [PERBAIKAN UTAMA] Gunakan URL spesifik Asia Tenggara
    private val db = FirebaseDatabase.getInstance("https://sijaga-95af3-default-rtdb.asia-southeast1.firebasedatabase.app/")

    // 1. Status Updates
    fun getStatusUpdates(macAddress: String): Flow<DeviceStatus?> = callbackFlow {
        val cleanMac = macAddress.replace(":", "").uppercase()
        val path = "devices/$cleanMac/status"
        val ref = db.getReference(path)

        Log.d("RealtimeDB", "Connecting to: $path at Asia-Southeast1")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    // Gunakan safe casting (convert ke String dulu baru ke Double/Long) untuk menghindari crash tipe data
                    val status = snapshot.child("status").getValue(String::class.java) ?: "OFFLINE"
                    
                    val lat = try {
                        snapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                    } catch (e: Exception) { 0.0 }

                    val lon = try {
                        snapshot.child("longitude").getValue(Double::class.java) ?: 0.0
                    } catch (e: Exception) { 0.0 }

                    val ts = try {
                        snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                    } catch (e: Exception) { 0L }

                    trySend(DeviceStatus(lat, lon, status, ts))
                    
                } catch (e: Exception) { 
                    Log.e("RealtimeDB", "Parsing Error: ${e.message}")
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("RealtimeDB", "Cancelled: ${error.message}")
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // 2. Info Updates (Nama & Kontak)
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
                } catch (e: Exception) { }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // 3. Save Info
    fun saveDeviceInfo(macAddress: String, name: String, contacts: List<ContactModel>) {
        val cleanMac = macAddress.replace(":", "").uppercase()
        val path = "devices/$cleanMac/info"
        db.getReference(path).setValue(DeviceInfo(name, contacts))
    }

    // 4. Delete Device
    fun deleteDeviceInfo(macAddress: String) {
        val cleanMac = macAddress.replace(":", "").uppercase()
        db.getReference("devices/$cleanMac").removeValue()
    }
}
