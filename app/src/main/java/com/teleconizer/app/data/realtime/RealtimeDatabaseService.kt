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

    private val db = FirebaseDatabase.getInstance()

    // 1. Status Updates (Tidak berubah)
    fun getStatusUpdates(macAddress: String): Flow<DeviceStatus?> = callbackFlow {
        val cleanMac = macAddress.replace(":", "").uppercase()
        val path = "devices/$cleanMac/status"
        val ref = db.getReference(path)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val status = snapshot.child("status").getValue(String::class.java)
                    val lat = snapshot.child("latitude").getValue(Double::class.java)
                    val lon = snapshot.child("longitude").getValue(Double::class.java)
                    val ts = snapshot.child("timestamp").getValue(Long::class.java)

                    if (status != null) {
                        trySend(DeviceStatus(lat, lon, status, ts))
                    } else {
                         // Jika status null, coba kirim data kosong/offline agar UI tau
                         trySend(null)
                    }
                } catch (e: Exception) { Log.e("RealtimeDB", "Error status", e) }
            }
            override fun onCancelled(error: DatabaseError) {}
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
                    
                    // Parsing List ContactModel
                    val typeIndicator = object : GenericTypeIndicator<List<ContactModel>>() {}
                    val contacts = snapshot.child("contacts").getValue(typeIndicator) ?: emptyList()
                    
                    trySend(DeviceInfo(name, contacts))
                } catch (e: Exception) { Log.e("RealtimeDB", "Error info", e) }
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
        val ref = db.getReference(path)
        val info = DeviceInfo(name, contacts)
        ref.setValue(info)
    }
    
    // 4. [FIX BUG 5] Hapus Data User di Firebase
    fun deleteDeviceInfo(macAddress: String) {
        val cleanMac = macAddress.replace(":", "").uppercase()
        // Hapus folder 'info' agar user hilang dari sync, 
        // Opsional: Hapus folder 'status' juga jika mau bersih total
        db.getReference("devices/$cleanMac").removeValue()
    }
}
