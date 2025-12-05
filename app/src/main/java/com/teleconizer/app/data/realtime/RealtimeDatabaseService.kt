package com.teleconizer.app.data.realtime

import android.util.Log
import com.google.firebase.database.*
import com.teleconizer.app.data.model.DeviceStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class RealtimeDatabaseService {

    private val tag = "RealtimeDB"

    // [MODIFIKASI] Menerima macAddress sebagai parameter
    fun getStatusUpdates(macAddress: String): Flow<DeviceStatus?> = callbackFlow {
        
        // Bersihkan MAC address (hapus titik dua dan uppercase) sesuai format ESP32
        val cleanMac = macAddress.replace(":", "").uppercase()
        
        // Path dinamis sesuai ESP32: devices/MAC_ADDRESS/status
        val path = "devices/$cleanMac/status"
        val database = FirebaseDatabase.getInstance().getReference(path)

        Log.d(tag, "✅ Listening to: $path")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    // Baca data aman (Safe Call)
                    val rawStatus = snapshot.child("status").getValue(String::class.java)
                    val lat = snapshot.child("latitude").getValue(Double::class.java)
                    val lon = snapshot.child("longitude").getValue(Double::class.java)
                    // ESP32 mengirim timestamp sebagai Long
                    val ts = snapshot.child("timestamp").getValue(Long::class.java)

                    if (rawStatus != null) {
                        val deviceStatus = DeviceStatus(
                            status = rawStatus,
                            latitude = lat,
                            longitude = lon,
                            timestamp = ts
                        )
                        trySend(deviceStatus)
                    }
                } catch (e: Exception) {
                    Log.e(tag, "❌ Error parsing snapshot for $cleanMac", e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(tag, "❌ Cancelled: ${error.message}")
            }
        }

        database.addValueEventListener(listener)

        awaitClose {
            Log.d(tag, "🛑 Stopped listening to $cleanMac")
            database.removeEventListener(listener)
        }
    }
}
