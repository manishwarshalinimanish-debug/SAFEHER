package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactPhone: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isIncoming: Boolean,
    val isSosAlert: Boolean = false
)
