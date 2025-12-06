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

    private val db = FirebaseDatabase.getInstance("https://sijaga-95af3-default-rtdb.asia-southeast1.firebasedatabase.app/")

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
                } catch (e: Exception) { Log.e("RealtimeDB", "Error", e) }
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
