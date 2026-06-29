package com.example.data.repository

import com.example.data.dao.SafetyDao
import com.example.data.entity.Contact
import com.example.data.entity.Message
import com.example.data.entity.AlertLog
import com.example.data.entity.SafetyHotspot
import com.example.data.entity.CachedSosAlert
import kotlinx.coroutines.flow.Flow

class SafetyRepository(private val safetyDao: SafetyDao) {
    val allContacts: Flow<List<Contact>> = safetyDao.getAllContacts()
    val allMessages: Flow<List<Message>> = safetyDao.getAllMessages()
    val allAlertLogs: Flow<List<AlertLog>> = safetyDao.getAllAlertLogs()
    val activeAlert: Flow<AlertLog?> = safetyDao.getActiveAlert()
    val allHotspots: Flow<List<SafetyHotspot>> = safetyDao.getAllHotspots()
    val allCachedSosAlerts: Flow<List<CachedSosAlert>> = safetyDao.getAllCachedSosAlerts()

    fun getMessagesForContact(phone: String): Flow<List<Message>> = safetyDao.getMessagesForContact(phone)

    suspend fun insertContact(contact: Contact): Long = safetyDao.insertContact(contact)

    suspend fun deleteContact(contact: Contact) = safetyDao.deleteContact(contact)

    suspend fun getPrimaryContact(): Contact? = safetyDao.getPrimaryContact()

    suspend fun getContactByPhone(phone: String): Contact? = safetyDao.getContactByPhone(phone)

    suspend fun insertMessage(message: Message): Long = safetyDao.insertMessage(message)

    suspend fun clearAllMessages() = safetyDao.clearAllMessages()

    suspend fun insertAlertLog(alertLog: AlertLog): Long = safetyDao.insertAlertLog(alertLog)

    suspend fun updateAlertLog(alertLog: AlertLog) = safetyDao.updateAlertLog(alertLog)

    suspend fun insertHotspot(hotspot: SafetyHotspot): Long = safetyDao.insertHotspot(hotspot)

    suspend fun deleteHotspot(hotspot: SafetyHotspot) = safetyDao.deleteHotspot(hotspot)

    suspend fun clearAllHotspots() = safetyDao.clearAllHotspots()

    suspend fun insertCachedSosAlert(alert: CachedSosAlert): Long = safetyDao.insertCachedSosAlert(alert)

    suspend fun deleteCachedSosAlert(id: Long) = safetyDao.deleteCachedSosAlert(id)

    suspend fun clearAllCachedSosAlerts() = safetyDao.clearAllCachedSosAlerts()

    suspend fun getCachedSosAlertsList(): List<CachedSosAlert> = safetyDao.getCachedSosAlertsList()
}
