package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.SafetyDao
import com.example.data.entity.Contact
import com.example.data.entity.Message
import com.example.data.entity.AlertLog
import com.example.data.entity.SafetyHotspot
import com.example.data.entity.CachedSosAlert

@Database(entities = [Contact::class, Message::class, AlertLog::class, SafetyHotspot::class, CachedSosAlert::class], version = 3, exportSchema = false)
abstract class SafetyDatabase : RoomDatabase() {
    abstract fun safetyDao(): SafetyDao

    companion object {
        @Volatile
        private var INSTANCE: SafetyDatabase? = null

        fun getDatabase(context: Context): SafetyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SafetyDatabase::class.java,
                    "safety_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
