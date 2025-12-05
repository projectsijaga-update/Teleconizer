package com.teleconizer.app.data.realtime

import android.util.Log
import com.google.firebase.database.*
import com.teleconizer.app.data.model.DeviceStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class RealtimeDatabaseService {

    private val database = FirebaseDatabase.getInstance().getReference("lansia/status")
    private val tag = "RealtimeDatabaseService"

    fun getStatusUpdates(): Flow<DeviceStatus?> = callbackFlow {

        Log.d(tag, "✅ Listener registered at path: lansia/status")

        val listener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                // ✅ Jika snapshot NULL, tetap tampilkan log
                Log.d(tag, "📥 onDataChange triggered. Raw snapshot: ${snapshot.value}")

                try {
                    val rawStatus = snapshot.child("status").getValue(String::class.java)
                    val lat = snapshot.child("latitude").getValue(Double::class.java)
                    val lon = snapshot.child("longitude").getValue(Double::class.java)

                    Log.d(tag, "✅ Parsed -> status=$rawStatus | lat=$lat | lon=$lon")

                    val deviceStatus = DeviceStatus(
                        status = rawStatus,
                        latitude = lat,
                        longitude = lon
                    )

                    trySend(deviceStatus)

                } catch (e: Exception) {
                    Log.e(tag, "❌ Error parsing Database snapshot", e)
                    trySend(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(tag, "❌ Listener cancelled: ${error.message}")
                trySend(null)
            }
        }

        // ✅ DAFTARKAN LISTENER
        database.addValueEventListener(listener)

        // ✅ Listener tidak dihapus sampai FLOW closed
        awaitClose {
            Log.d(tag, "🛑 Flow closed -> Listener removed")
            database.removeEventListener(listener)
        }
    }
}