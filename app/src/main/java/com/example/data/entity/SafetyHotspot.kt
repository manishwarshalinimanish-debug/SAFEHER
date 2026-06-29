package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "safety_hotspots")
data class SafetyHotspot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val description: String,
    val riskLevel: String, // "HIGH", "MEDIUM", "LOW"
    val reportTime: Long = System.currentTimeMillis()
)
