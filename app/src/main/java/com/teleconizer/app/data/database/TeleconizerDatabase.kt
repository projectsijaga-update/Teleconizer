package com.teleconizer.app.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.teleconizer.app.data.model.EmergencyContact

@Database(
    entities = [EmergencyContact::class],
    version = 1,
    exportSchema = false
)
abstract class TeleconizerDatabase : RoomDatabase() {
    
    abstract fun emergencyContactDao(): EmergencyContactDao
    
    companion object {
        @Volatile
        private var INSTANCE: TeleconizerDatabase? = null
        
        fun getDatabase(context: Context): TeleconizerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TeleconizerDatabase::class.java,
                    "teleconizer_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

