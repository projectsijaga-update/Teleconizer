package com.teleconizer.app.data.realtime

import android.util.Log
import com.google.firebase.database.*
import com.teleconizer.app.data.model.DeviceStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class RealtimeDatabaseService {

    // Fungsi ini menerima MAC Address spesifik untuk didengarkan
    fun getStatusUpdates(macAddress: String): Flow<DeviceStatus?> = callbackFlow {
        
        // 1. Bersihkan MAC Address (Hapus titik dua ':' dan ubah ke Huruf Besar)
        // Contoh: "AA:BB:CC" -> "AABBCC" (Sesuai format ESP32)
        val cleanMac = macAddress.replace(":", "").uppercase()
        
        // 2. Tentukan Path yang tepat
        val path = "devices/$cleanMac/status"
        val database = FirebaseDatabase.getInstance().getReference(path)

        Log.d("RealtimeDB", "Connecting to: $path")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    // 3. Ambil data sesuai struktur JSON Anda
                    val status = snapshot.child("status").getValue(String::class.java)
                    val lat = snapshot.child("latitude").getValue(Double::class.java)
                    val lon = snapshot.child("longitude").getValue(Double::class.java)
                    val ts = snapshot.child("timestamp").getValue(Long::class.java)

                    if (status != null) {
                        val data = DeviceStatus(lat, lon, status, ts)
                        trySend(data)
                    }
                } catch (e: Exception) {
                    Log.e("RealtimeDB", "Error parsing data", e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("RealtimeDB", "Firebase Cancelled: ${error.message}")
            }
        }

        database.addValueEventListener(listener)

        awaitClose {
            database.removeEventListener(listener)
        }
    }
}
