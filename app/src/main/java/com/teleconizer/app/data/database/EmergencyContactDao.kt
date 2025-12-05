package com.teleconizer.app.data.database

import androidx.room.*
import com.teleconizer.app.data.model.EmergencyContact
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyContactDao {
    
    @Query("SELECT * FROM emergency_contact ORDER BY id DESC LIMIT 1")
    fun getLatestContact(): Flow<EmergencyContact?>
    
    @Query("SELECT * FROM emergency_contact ORDER BY id DESC")
    fun getAllContacts(): Flow<List<EmergencyContact>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: EmergencyContact): Long
    
    @Update
    suspend fun updateContact(contact: EmergencyContact): Int
    
    @Delete
    suspend fun deleteContact(contact: EmergencyContact): Int
    
    @Query("DELETE FROM emergency_contact")
    suspend fun deleteAllContacts(): Int
}

