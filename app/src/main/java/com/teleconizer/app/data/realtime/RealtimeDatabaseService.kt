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

    // Menggunakan Server Asia Tenggara
    private val db = FirebaseDatabase.getInstance("https://sijaga-95af3-default-rtdb.asia-southeast1.firebasedatabase.app/")

    // [FITUR BARU] Mendengarkan SELURUH Daftar Perangkat & Statusnya Sekaligus
    // Ini memperbaiki masalah "User Hilang saat Reinstall" karena data langsung diambil dari Cloud
    fun getGlobalDeviceList(): Flow<List<Patient>> = callbackFlow {
        val ref = db.getReference("devices")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val patients = mutableListOf<Patient>()
                
                // Loop semua MAC Address yang ada di database
                for (deviceSnap in snapshot.children) {
                    try {
                        val mac = deviceSnap.key ?: continue
                        
                        // 1. Ambil Info (Nama & Kontak)
                        val infoSnap = deviceSnap.child("info")
                        // Jika tidak ada nama, lewati (berarti bukan user valid/hanya data sampah)
                        val name = infoSnap.child("name").getValue(String::class.java) ?: continue 
                        
                        val typeIndicator = object : GenericTypeIndicator<List<ContactModel>>() {}
                        val contacts = infoSnap.child("contacts").getValue(typeIndicator) ?: emptyList()

                        // 2. Ambil Status (Sensor)
                        // Logika "Safe Parsing" untuk memperbaiki bug Offline
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
                        
                        // 3. Gabungkan menjadi objek Patient
                        // ID kita buat dari hashCode MAC Address agar unik & konsisten
                        patients.add(Patient(
                            id = mac, // Gunakan MAC sebagai ID agar stabil
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

            override fun onCancelled(error: DatabaseError) {
                Log.e("RealtimeDB", "Global List Error: ${error.message}")
            }
        }
        
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // Fungsi Save & Delete tetap sama (hanya update Info)
    fun saveDeviceInfo(macAddress: String, name: String, contacts: List<ContactModel>) {
        val cleanMac = macAddress.replace(":", "").uppercase()
        val path = "devices/$cleanMac/info"
        db.getReference(path).setValue(DeviceInfo(name, contacts))
    }

    fun deleteDeviceInfo(macAddress: String) {
        val cleanMac = macAddress.replace(":", "").uppercase()
        // Hapus INFO saja agar tidak muncul di list, tapi data sensor (history) tetap aman di DB jika perlu
        // Tapi sesuai request terakhir "menghapus user di apk ... data user tersebut (info) ... terhapus"
        // Kita hapus folder 'info'. Tanpa 'info/name', user tidak akan muncul di list getGlobalDeviceList (lihat logika di atas)
        db.getReference("devices/$cleanMac/info").removeValue()
    }
}
