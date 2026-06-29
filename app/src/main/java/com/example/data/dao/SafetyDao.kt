package com.example.data.dao

import androidx.room.*
import com.example.data.entity.Contact
import com.example.data.entity.Message
import com.example.data.entity.AlertLog
import com.example.data.entity.SafetyHotspot
import com.example.data.entity.CachedSosAlert
import kotlinx.coroutines.flow.Flow

@Dao
interface SafetyDao {
    // --- Contacts ---
    @Query("SELECT * FROM emergency_contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<Contact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact): Long

    @Delete
    suspend fun deleteContact(contact: Contact)

    @Query("SELECT * FROM emergency_contacts WHERE isPrimary = 1 LIMIT 1")
    suspend fun getPrimaryContact(): Contact?

    @Query("SELECT * FROM emergency_contacts WHERE phone = :phone LIMIT 1")
    suspend fun getContactByPhone(phone: String): Contact?

    // --- Messages ---
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE contactPhone = :phone ORDER BY timestamp ASC")
    fun getMessagesForContact(phone: String): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message): Long

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()

    // --- Alert Logs ---
    @Query("SELECT * FROM alert_logs ORDER BY timestamp DESC")
    fun getAllAlertLogs(): Flow<List<AlertLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlertLog(alertLog: AlertLog): Long

    @Update
    suspend fun updateAlertLog(alertLog: AlertLog)

    @Query("SELECT * FROM alert_logs WHERE status = 'ACTIVE' ORDER BY timestamp DESC LIMIT 1")
    fun getActiveAlert(): Flow<AlertLog?>

    // --- Safety Hotspots ---
    @Query("SELECT * FROM safety_hotspots ORDER BY reportTime DESC")
    fun getAllHotspots(): Flow<List<SafetyHotspot>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHotspot(hotspot: SafetyHotspot): Long

    @Delete
    suspend fun deleteHotspot(hotspot: SafetyHotspot)

    @Query("DELETE FROM safety_hotspots")
    suspend fun clearAllHotspots()

    // --- Cached SOS Alerts ---
    @Query("SELECT * FROM cached_sos_alerts ORDER BY timestamp DESC")
    fun getAllCachedSosAlerts(): Flow<List<CachedSosAlert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedSosAlert(alert: CachedSosAlert): Long

    @Query("DELETE FROM cached_sos_alerts WHERE id = :id")
    suspend fun deleteCachedSosAlert(id: Long)

    @Query("DELETE FROM cached_sos_alerts")
    suspend fun clearAllCachedSosAlerts()

    @Query("SELECT * FROM cached_sos_alerts")
    suspend fun getCachedSosAlertsList(): List<CachedSosAlert>
}
