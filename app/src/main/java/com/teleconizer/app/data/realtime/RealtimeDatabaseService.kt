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

    // [PERBAIKAN BUG OFFLINE]
    // Kita wajib menuliskan URL lengkap karena database Anda berada di Asia (bukan US default)
    private val db = FirebaseDatabase.getInstance("https://sijaga-95af3-default-rtdb.asia-southeast1.firebasedatabase.app/")

    // 1. Mendengarkan Status (Sensor & Lokasi)
    fun getStatusUpdates(macAddress: String): Flow<DeviceStatus?> = callbackFlow {
        val cleanMac = macAddress.replace(":", "").uppercase()
        val path = "devices/$cleanMac/status"
        val ref = db.getReference(path)

        Log.d("RealtimeDB", "Mendengarkan path: $path di Server Asia")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    // Menggunakan 'safe call' (?.) dan 'elvis operator' (?:) untuk mencegah crash jika data kosong
                    val status = snapshot.child("status").getValue(String::class.java)
                    
                    // Jika status null, berarti data belum masuk/path salah
                    if (status == null) {
                        trySend(null) // Kirim null agar UI tetap "Loading/Offline"
                        return
                    }

                    val lat = snapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                    val lon = snapshot.child("longitude").getValue(Double::class.java) ?: 0.0
                    val ts = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L

                    trySend(DeviceStatus(lat, lon, status, ts))
                    
                } catch (e: Exception) { 
                    Log.e("RealtimeDB", "Gagal parsing data: ${e.message}")
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("RealtimeDB", "Database Error: ${error.message}")
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // 2. Mendengarkan Info User (Nama & Kontak)
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

    // 3. Simpan Info User (Nama & Kontak)
    fun saveDeviceInfo(macAddress: String, name: String, contacts: List<ContactModel>) {
        val cleanMac = macAddress.replace(":", "").uppercase()
        val path = "devices/$cleanMac/info"
        // Hanya update node 'info', tidak mengganggu 'status'
        db.getReference(path).setValue(DeviceInfo(name, contacts))
    }

    // [PERBAIKAN FITUR HAPUS]
    // Hanya menghapus data INFO (Nama & Kontak) dari Firebase
    // Data STATUS (Sensor/Lokasi) dari ESP32 TIDAK AKAN DIHAPUS
    fun deleteDeviceInfo(macAddress: String) {
        val cleanMac = macAddress.replace(":", "").uppercase()
        // Target penghapusan spesifik ke folder 'info'
        db.getReference("devices/$cleanMac/info").removeValue()
    }
}
