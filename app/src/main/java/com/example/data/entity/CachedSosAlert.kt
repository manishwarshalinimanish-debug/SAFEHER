package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_sos_alerts")
data class CachedSosAlert(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double,
    val longitude: Double,
    val locationAddress: String,
    val contactsPayload: String // semicolon-separated, e.g. "Name1|Phone1;Name2|Phone2"
)
