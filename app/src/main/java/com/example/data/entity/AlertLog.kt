package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alert_logs")
data class AlertLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double?,
    val longitude: Double?,
    val locationName: String? = null,
    val status: String = "ACTIVE" // ACTIVE, RESOLVED
)
