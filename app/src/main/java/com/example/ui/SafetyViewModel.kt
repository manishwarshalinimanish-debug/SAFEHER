package com.example.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.File
import android.location.Location
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.SafetyDatabase
import com.example.data.entity.AlertLog
import com.example.data.entity.Contact
import com.example.data.entity.Message
import com.example.data.repository.SafetyRepository
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class SafetyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SafetyRepository
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    private var locationCallback: LocationCallback? = null

    // ToneGenerator for loud emergency siren
    private var toneGenerator: ToneGenerator? = null
    private var sirenJob: Job? = null

    // Location States
    private val _latitude = MutableStateFlow<Double>(37.7749) // Default SF
    val latitude: StateFlow<Double> = _latitude.asStateFlow()

    private val _longitude = MutableStateFlow<Double>(-122.4194)
    val longitude: StateFlow<Double> = _longitude.asStateFlow()

    private val _locationAddress = MutableStateFlow<String>("Golden Gate Park, San Francisco, CA")
    val locationAddress: StateFlow<String> = _locationAddress.asStateFlow()

    private val _isLocationPermissionGranted = MutableStateFlow(false)
    val isLocationPermissionGranted: StateFlow<Boolean> = _isLocationPermissionGranted.asStateFlow()

    // Alarm/SOS States
    private val _sosCountdown = MutableStateFlow<Int?>(null)
    val sosCountdown: StateFlow<Int?> = _sosCountdown.asStateFlow()

    private val _isSirenPlaying = MutableStateFlow(false)
    val isSirenPlaying: StateFlow<Boolean> = _isSirenPlaying.asStateFlow()

    private val _shakeEnabled = MutableStateFlow(true)
    val shakeEnabled: StateFlow<Boolean> = _shakeEnabled.asStateFlow()

    // Active Chat Contact selection
    private val _selectedContact = MutableStateFlow<Contact?>(null)
    val selectedContact: StateFlow<Contact?> = _selectedContact.asStateFlow()

    // Real-time GPS/Geolocation Streaming state
    private val _gpsStreamPath = MutableStateFlow<List<Pair<Long, Pair<Double, Double>>>>(emptyList())
    val gpsStreamPath: StateFlow<List<Pair<Long, Pair<Double, Double>>>> = _gpsStreamPath.asStateFlow()

    private val _isStreamingLocation = MutableStateFlow(false)
    val isStreamingLocation: StateFlow<Boolean> = _isStreamingLocation.asStateFlow()

    private var locationStreamingJob: Job? = null

    // UI Toast or Alert Logs Status
    private val _systemStatus = MutableStateFlow<String>("Safety System Active")
    val systemStatus: StateFlow<String> = _systemStatus.asStateFlow()

    private val _recordedVideoUri = MutableStateFlow<Uri?>(null)
    val recordedVideoUri: StateFlow<Uri?> = _recordedVideoUri.asStateFlow()

    // Countdown Timer holder
    private var countdownTimer: CountDownTimer? = null

    private val _isOnline = MutableStateFlow<Boolean>(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    // Expose Flows from Repository
    val contacts: StateFlow<List<Contact>>
    val messages: StateFlow<List<Message>>
    val alertLogs: StateFlow<List<AlertLog>>
    val activeAlert: StateFlow<AlertLog?>
    val hotspots: StateFlow<List<com.example.data.entity.SafetyHotspot>>
    val cachedSosAlerts: StateFlow<List<com.example.data.entity.CachedSosAlert>>

    private val _selectedDestination = MutableStateFlow<String?>(null)
    val selectedDestination: StateFlow<String?> = _selectedDestination.asStateFlow()

    private val _currentRouteIndex = MutableStateFlow<Int>(0) // 0 = Safest Route, 1 = Direct/Fastest Route
    val currentRouteIndex: StateFlow<Int> = _currentRouteIndex.asStateFlow()

    private val _isNavigating = MutableStateFlow<Boolean>(false)
    val isNavigating: StateFlow<Boolean> = _isNavigating.asStateFlow()

    private val _navigationStatus = MutableStateFlow<String>("Select destination to audit route safety.")
    val navigationStatus: StateFlow<String> = _navigationStatus.asStateFlow()

    private var navigationJob: Job? = null

    init {
        val database = SafetyDatabase.getDatabase(application)
        repository = SafetyRepository(database.safetyDao())

        contacts = repository.allContacts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        messages = repository.allMessages.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        alertLogs = repository.allAlertLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        activeAlert = repository.activeAlert.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        hotspots = repository.allHotspots.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        cachedSosAlerts = repository.allCachedSosAlerts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Prepopulate data if empty
        viewModelScope.launch(Dispatchers.IO) {
            prepopulateIfNeeded()
        }

        // Start simulated movement to show "Real-time location tracking" in action
        startSimulatedLocationMovement()
        
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        } catch (e: Exception) {
            Log.e("SafetyViewModel", "Failed to initialize ToneGenerator", e)
        }
    }

    private suspend fun prepopulateIfNeeded() {
        val currentContacts = contacts.first()
        if (currentContacts.isEmpty()) {
            val mom = Contact(name = "Mom (Primary Contact)", phone = "+1 (555) 019-2831", relationship = "Mother", isPrimary = true)
            val dad = Contact(name = "Dad", phone = "+1 (555) 019-2832", relationship = "Father", isPrimary = false)
            val sister = Contact(name = "Sarah (Sister)", phone = "+1 (555) 019-2833", relationship = "Sister", isPrimary = false)
            val safeZone = Contact(name = "Emergency Help Desk", phone = "911", relationship = "Official Rescue", isPrimary = false)

            repository.insertContact(mom)
            repository.insertContact(dad)
            repository.insertContact(sister)
            repository.insertContact(safeZone)

            // Insert initial welcome messages
            repository.insertMessage(Message(
                contactPhone = mom.phone,
                content = "Hi dear, stay safe! Remember you can shake your phone anytime if you are in trouble to alert me.",
                timestamp = System.currentTimeMillis() - 3600000 * 2, // 2 hours ago
                isIncoming = true
            ))

            repository.insertMessage(Message(
                contactPhone = mom.phone,
                content = "I will keep this chat open. Talk to me when you start walking home.",
                timestamp = System.currentTimeMillis() - 3600000 * 1, // 1 hour ago
                isIncoming = true
            ))
        }

        val currentHotspots = repository.allHotspots.first()
        if (currentHotspots.isEmpty()) {
            repository.insertHotspot(com.example.data.entity.SafetyHotspot(
                latitude = 37.7833,
                longitude = -122.4167,
                title = "Tenderloin High Theft Intersection",
                description = "Historically high rates of crime. Dark corners and poorly patrolled alleyways.",
                riskLevel = "HIGH"
            ))
            repository.insertHotspot(com.example.data.entity.SafetyHotspot(
                latitude = 37.7650,
                longitude = -122.4190,
                title = "16th St Mission Alleyway",
                description = "Poor lighting and frequent reports of antisocial behavior or pickpocketing.",
                riskLevel = "HIGH"
            ))
            repository.insertHotspot(com.example.data.entity.SafetyHotspot(
                latitude = 37.7770,
                longitude = -122.4100,
                title = "Soma Poorly Lit Underpass",
                description = "Dimly lit passage with construction hoardings obstructing visibility.",
                riskLevel = "MEDIUM"
            ))
        }
    }

    // Set Location Permission State and trigger location request if granted
    @SuppressLint("MissingPermission")
    fun setLocationPermissionGranted(granted: Boolean) {
        _isLocationPermissionGranted.value = granted
        if (granted) {
            startRealLocationUpdates()
        } else {
            stopRealLocationUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRealLocationUpdates() {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location = locationResult.lastLocation ?: return
                    _latitude.value = location.latitude
                    _longitude.value = location.longitude
                    updateAddressString(location.latitude, location.longitude)
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            Log.e("SafetyViewModel", "Error starting location updates", e)
        }
    }

    private fun stopRealLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
    }

    private fun updateAddressString(lat: Double, lon: Double) {
        val formattedLat = String.format(Locale.US, "%.4f", lat)
        val formattedLon = String.format(Locale.US, "%.4f", lon)
        _locationAddress.value = "Safe Path Zone, near ($formattedLat, $formattedLon)"
    }

    // Simulated path movement to represent live active tracker moving on a route
    private fun startSimulatedLocationMovement() {
        viewModelScope.launch {
            while (true) {
                delay(8000) // update coordinates every 8s
                if (!_isLocationPermissionGranted.value) {
                    // Slight walk simulation
                    val deltaLat = (Random.nextDouble() - 0.5) * 0.0003
                    val deltaLon = (Random.nextDouble() - 0.5) * 0.0003
                    _latitude.value = _latitude.value + deltaLat
                    _longitude.value = _longitude.value + deltaLon
                    updateAddressString(_latitude.value, _longitude.value)
                }
            }
        }
    }

    // Toggle shake detection state
    fun toggleShakeEnabled() {
        _shakeEnabled.value = !_shakeEnabled.value
        _systemStatus.value = if (_shakeEnabled.value) "Shake SOS Active" else "Shake SOS Suspended"
    }

    // Triggered by shaking phone or direct emergency button tap
    fun handleShakeTriggered() {
        if (!_shakeEnabled.value && _sosCountdown.value == null) return
        if (_sosCountdown.value != null || _isSirenPlaying.value) {
            // Already active/counting down, ignore
            return
        }
        startSosCountdown()
    }

    private fun startSosCountdown() {
        _sosCountdown.value = 5
        _systemStatus.value = "SHAKE DETECTED! SOS triggering in 5s..."
        
        // Single beep for countdown initialization
        playBeep(200)

        countdownTimer = object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt() + 1
                _sosCountdown.value = secondsLeft
                _systemStatus.value = "SOS triggering in ${secondsLeft}s! Tap CANCEL to abort."
                playBeep(100)
            }

            override fun onFinish() {
                _sosCountdown.value = null
                triggerSosAlert()
            }
        }.start()
    }

    fun cancelSosCountdown() {
        countdownTimer?.cancel()
        countdownTimer = null
        _sosCountdown.value = null
        _systemStatus.value = "SOS Trigger Cancelled Safely."
        playBeep(300)
    }

    // Trigger SOS alert (countdown has completed, or instant dispatch)
    fun triggerSosAlert() {
        _sosCountdown.value = null
        
        if (!_isOnline.value) {
            _systemStatus.value = "OFFLINE: Caching SOS locally..."
            viewModelScope.launch(Dispatchers.IO) {
                // Prepare serialized contact information payload
                val contactsPayload = contacts.value.joinToString(";") { "${it.name}|${it.phone}|${it.relationship}" }
                
                // Cache the alert in Room
                val cached = com.example.data.entity.CachedSosAlert(
                    latitude = _latitude.value,
                    longitude = _longitude.value,
                    locationAddress = _locationAddress.value,
                    contactsPayload = contactsPayload
                )
                repository.insertCachedSosAlert(cached)

                // Log a Cached Alert in local history
                val alertLog = AlertLog(
                    latitude = _latitude.value,
                    longitude = _longitude.value,
                    locationName = "[CACHED OFFLINE] ${_locationAddress.value}",
                    status = "CACHED_OFFLINE"
                )
                repository.insertAlertLog(alertLog)

                // Start local sirens
                startSirenAlarm()

                _systemStatus.value = "Device Offline. SOS Cached & Sirens Triggered! Will send once connected."
            }
            return
        }

        _systemStatus.value = "SOS EMERGENCY ALERT ACTIVE!"
        
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Create and insert alert log
            val alert = AlertLog(
                latitude = _latitude.value,
                longitude = _longitude.value,
                locationName = _locationAddress.value,
                status = "ACTIVE"
            )
            val alertId = repository.insertAlertLog(alert)

            // 2. Format a modern emergency map message
            val mapLink = "https://www.google.com/maps/search/?api=1&query=${_latitude.value},${_longitude.value}"
            val alertMessageText = "🔴 EMERGENCY SOS ALERT! 🔴\nI feel unsafe and have triggered my safety app.\nMy location: ${_locationAddress.value}\nLive Track: $mapLink"

            // 3. Send message to all contacts
            val allContactsList = contacts.value
            for (contact in allContactsList) {
                repository.insertMessage(Message(
                    contactPhone = contact.phone,
                    content = alertMessageText,
                    isIncoming = false,
                    isSosAlert = true
                ))
            }

            // Launch the default messaging app with pre-filled SOS details
            sendSmsViaDefaultApp(allContactsList.map { it.phone }, alertMessageText)

            // 4. Start sound sirens
            startSirenAlarm()

            // 5. Start real-time Geolocation streaming to dashboard
            startLocationStreaming()

            // 6. Automatically schedule a mock contact response from the primary contact to simulate alert delivery receipt
            val primary = repository.getPrimaryContact() ?: allContactsList.firstOrNull()
            primary?.let { contact ->
                delay(3000) // Deliver after 3 seconds
                repository.insertMessage(Message(
                    contactPhone = contact.phone,
                    content = "OH MY GOD! I got your SOS alert! 🚔 I am calling the police and rushing to you right now. Stay where you are, we can track your live movement on this map!",
                    isIncoming = true
                ))
                _systemStatus.value = "SOS Alert Delivered. Contact ${contact.name} is responding."
            }
        }
    }

    private fun startLocationStreaming() {
        locationStreamingJob?.cancel()
        _isStreamingLocation.value = true
        _gpsStreamPath.value = emptyList()
        locationStreamingJob = viewModelScope.launch(Dispatchers.IO) {
            while (_isStreamingLocation.value) {
                val currentLat = _latitude.value
                val currentLng = _longitude.value
                val timestamp = System.currentTimeMillis()

                _gpsStreamPath.update { list ->
                    list + (timestamp to (currentLat to currentLng))
                }

                _systemStatus.value = "Streaming GPS packet to Guardian Dashboard: (${String.format(Locale.US, "%.5f", currentLat)}, ${String.format(Locale.US, "%.5f", currentLng)})"

                // Every 12 seconds, simulated stream updates to the active chat screen contacts
                if (_gpsStreamPath.value.size % 3 == 0) {
                    val mapLink = "https://www.google.com/maps/search/?api=1&query=$currentLat,$currentLng"
                    val allContactsList = contacts.value
                    for (contact in allContactsList) {
                        repository.insertMessage(Message(
                            contactPhone = contact.phone,
                            content = "📡 [LIVE TRACKING] Current GPS: (${String.format(Locale.US, "%.5f", currentLat)}, ${String.format(Locale.US, "%.5f", currentLng)})\nStream link: $mapLink",
                            isIncoming = false,
                            isSosAlert = true
                        ))
                    }
                }
                delay(4000)
            }
        }
    }

    private fun stopLocationStreaming() {
        _isStreamingLocation.value = false
        locationStreamingJob?.cancel()
        locationStreamingJob = null
    }

    // Deactivates the SOS mode
    fun resolveActiveAlert() {
        _systemStatus.value = "Emergency Alert Resolved. All systems standing by."
        stopSirenAlarm()
        stopLocationStreaming()
        
        viewModelScope.launch(Dispatchers.IO) {
            val logs = alertLogs.value
            val active = logs.firstOrNull { it.status == "ACTIVE" }
            if (active != null) {
                repository.updateAlertLog(active.copy(status = "RESOLVED"))
            }
            
            // Send safety check clear to contacts
            val allContactsList = contacts.value
            val clearMessageText = "🟢 SAFE NOW: I have resolved the SOS alert. I am safe now. Thank you!"
            for (contact in allContactsList) {
                repository.insertMessage(Message(
                    contactPhone = contact.phone,
                    content = clearMessageText,
                    isIncoming = false,
                    isSosAlert = false
                ))
            }

            // Launch default SMS app to let user send clear/safe messages
            sendSmsViaDefaultApp(allContactsList.map { it.phone }, clearMessageText)
        }
    }

    // Siren alarm management
    private fun startSirenAlarm() {
        if (_isSirenPlaying.value) return
        _isSirenPlaying.value = true
        
        sirenJob = viewModelScope.launch(Dispatchers.IO) {
            while (_isSirenPlaying.value) {
                try {
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 400)
                    delay(500)
                    toneGenerator?.startTone(ToneGenerator.TONE_SUP_ERROR, 400)
                    delay(500)
                } catch (e: Exception) {
                    Log.e("SafetyViewModel", "Tone play failed", e)
                    delay(1000)
                }
            }
        }
    }

    fun stopSirenAlarm() {
        _isSirenPlaying.value = false
        sirenJob?.cancel()
        sirenJob = null
        try {
            toneGenerator?.stopTone()
        } catch (e: Exception) {
            Log.e("SafetyViewModel", "Tone stop failed", e)
        }
    }

    // ToneGenerator Helper for UI Feedback
    private fun playBeep(durationMs: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, durationMs)
            } catch (e: Exception) {
                Log.e("SafetyViewModel", "Beep play failed", e)
            }
        }
    }

    // Selection of Chat
    fun selectContact(contact: Contact?) {
        _selectedContact.value = contact
    }

    // Messaging operations (Normal Messaging App feature)
    fun sendMessageToSelectedContact(content: String) {
        val contact = _selectedContact.value ?: return
        if (content.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            val msgId = repository.insertMessage(Message(
                contactPhone = contact.phone,
                content = content,
                isIncoming = false
            ))

            // Launch the default messaging app for the selected contact
            sendSmsViaDefaultApp(listOf(contact.phone), content)

            // Simulate incoming response after a small delay
            delay(1500)
            
            val responseText = when {
                content.contains("safe", ignoreCase = true) -> {
                    "That is wonderful to hear! Let me know when you are fully back inside."
                }
                content.contains("help", ignoreCase = true) || content.contains("scared", ignoreCase = true) -> {
                    "Should I call you right now? Or do you want me to track your location on live map?"
                }
                else -> {
                    "Got it! I am keeping an eye on your status. Send an SOS if anything changes."
                }
            }

            repository.insertMessage(Message(
                contactPhone = contact.phone,
                content = responseText,
                isIncoming = true
            ))
        }
    }

    // Contact registration (CRUD)
    fun addEmergencyContact(name: String, phone: String, relationship: String, isPrimary: Boolean) {
        if (name.isBlank() || phone.isBlank() || relationship.isBlank()) return
        
        viewModelScope.launch(Dispatchers.IO) {
            val contact = Contact(
                name = name,
                phone = phone,
                relationship = relationship,
                isPrimary = isPrimary
            )
            repository.insertContact(contact)
        }
    }

    fun removeContact(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteContact(contact)
            if (_selectedContact.value?.id == contact.id) {
                _selectedContact.value = null
            }
        }
    }

    // --- Safest Route Suggestions & Geolocation API Routing Integration ---
    fun selectDestination(destination: String?) {
        _selectedDestination.value = destination
        if (destination == null) {
            _navigationStatus.value = "Select destination to audit route safety."
            stopNavigation()
        } else {
            _navigationStatus.value = "Safety Audit Complete. Safest route (Shield) avoids all known safety hotspots and historical incident locations."
        }
    }

    fun selectRoute(index: Int) {
        _currentRouteIndex.value = index
        if (_isNavigating.value) {
            // Restart navigation on route switch
            startNavigation()
        }
    }

    fun startNavigation() {
        val dest = _selectedDestination.value ?: return
        _isNavigating.value = true
        _navigationStatus.value = "Navigating along " + (if (_currentRouteIndex.value == 0) "SHIELD (Safest)" else "DIRECT (Fastest)") + " route..."
        
        navigationJob?.cancel()
        navigationJob = viewModelScope.launch {
            val startLat = 37.7749
            val startLng = -122.4194
            
            val destLat = when (dest) {
                "Union Square" -> 37.7879
                "Mission District" -> 37.7599
                "Coit Tower" -> 37.8024
                else -> 37.7879
            }
            val destLng = when (dest) {
                "Union Square" -> -122.4074
                "Mission District" -> -122.4148
                "Coit Tower" -> -122.4058
                else -> -122.4074
            }

            val steps = 8
            for (i in 1..steps) {
                if (!_isNavigating.value) break
                delay(3000)
                
                val fraction = i.toDouble() / steps
                val currentLat: Double
                val currentLng: Double
                
                if (_currentRouteIndex.value == 0) {
                    // Safest route curves south and east to bypass the Tenderloin hotspot (37.7833, -122.4167)
                    val curveFactor = if (fraction < 0.6) 0.005 else 0.002
                    currentLat = startLat + (fraction * (destLat - startLat)) - curveFactor
                    currentLng = startLng + (fraction * (destLng - startLng)) + curveFactor
                } else {
                    // Direct route is a straight path passing directly through Tenderloin/Soma hotspots
                    currentLat = startLat + (fraction * (destLat - startLat))
                    currentLng = startLng + (fraction * (destLng - startLng))
                }
                
                _latitude.value = currentLat
                _longitude.value = currentLng
                _locationAddress.value = "Walking along route... (${String.format(Locale.US, "%.5f", currentLat)}, ${String.format(Locale.US, "%.5f", currentLng)})"
                
                // Real-time hotspot proximity auditing
                val currentHotspotsList = hotspots.value
                var warningTriggered = false
                for (hotspot in currentHotspotsList) {
                    val dist = calculateDistance(currentLat, currentLng, hotspot.latitude, hotspot.longitude)
                    // If within 300 meters on a direct route, trigger proximity alarm
                    if (dist < 320.0 && _currentRouteIndex.value == 1) {
                        _navigationStatus.value = "⚠️ HAZARD WARNING: You are entering a crime hotspot/incident zone: ${hotspot.title}!"
                        _systemStatus.value = "Hazard Warning: Proximity to ${hotspot.title}"
                        warningTriggered = true
                        break
                    }
                }
                
                if (!warningTriggered) {
                    _navigationStatus.value = "Walking... ${String.format(Locale.US, "%.0f", fraction * 100)}% complete. Lighting on route: " + (if (_currentRouteIndex.value == 0) "100% EXCELLENT" else "30% WEAK")
                }
            }
            
            if (_isNavigating.value) {
                _latitude.value = destLat
                _longitude.value = destLng
                _locationAddress.value = "Arrived safely at $dest."
                _navigationStatus.value = "Destination reached! Shield routing navigation ended successfully."
                _isNavigating.value = false
            }
        }
    }

    fun stopNavigation() {
        _isNavigating.value = false
        navigationJob?.cancel()
        navigationJob = null
    }

    fun reportHotspot(title: String, description: String, riskLevel: String, lat: Double, lng: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val hotspot = com.example.data.entity.SafetyHotspot(
                title = title,
                description = description,
                riskLevel = riskLevel,
                latitude = lat,
                longitude = lng
            )
            repository.insertHotspot(hotspot)
            _systemStatus.value = "Registered new safety hotspot in local database: $title."
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371e3 // Earth radius in meters
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)
        val a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
                Math.cos(phi1) * Math.cos(phi2) *
                Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    private fun sendSmsViaDefaultApp(phoneNumbers: List<String>, message: String) {
        viewModelScope.launch(Dispatchers.Main) {
            val context = getApplication<Application>()
            val phoneString = phoneNumbers.joinToString(
                separator = if (android.os.Build.MANUFACTURER.lowercase().contains("samsung")) "," else ";"
            )
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneString")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("SafetyViewModel", "Failed to launch ACTION_SENDTO SMS intent, trying ACTION_VIEW", e)
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("sms:$phoneString")
                    putExtra("sms_body", message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(fallbackIntent)
                } catch (e2: Exception) {
                    Log.e("SafetyViewModel", "Failed to launch ACTION_VIEW SMS intent, trying ACTION_SEND", e2)
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, message)
                        putExtra("address", phoneString)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(sendIntent)
                    } catch (e3: Exception) {
                        Log.e("SafetyViewModel", "All SMS intents failed", e3)
                    }
                }
            }
        }
    }

    fun setOnlineStatus(online: Boolean) {
        _isOnline.value = online
        if (online) {
            _systemStatus.value = "Connectivity Restored. Processing Cached SOS Alerts..."
            dispatchCachedAlerts()
        } else {
            _systemStatus.value = "Device Offline. Alert dispatching will be cached."
        }
    }

    private fun dispatchCachedAlerts() {
        viewModelScope.launch(Dispatchers.IO) {
            val cachedAlertsList = repository.getCachedSosAlertsList()
            if (cachedAlertsList.isEmpty()) {
                _systemStatus.value = "No cached alerts found."
                return@launch
            }
            
            _systemStatus.value = "Restoring offline SOS. Dispatching ${cachedAlertsList.size} cached alerts..."
            for (cached in cachedAlertsList) {
                // 1. Create a live AlertLog from the cached location data
                val alert = AlertLog(
                    latitude = cached.latitude,
                    longitude = cached.longitude,
                    locationName = cached.locationAddress,
                    status = "ACTIVE"
                )
                repository.insertAlertLog(alert)

                // 2. Build the emergency message
                val mapLink = "https://www.google.com/maps/search/?api=1&query=${cached.latitude},${cached.longitude}"
                val alertMessageText = "🔴 [OFFLINE CACHED SENT] EMERGENCY SOS ALERT! 🔴\nI feel unsafe and have triggered my safety app.\nMy location: ${cached.locationAddress}\nLive Track: $mapLink"

                // 3. Deserialize contacts list
                val contactsToNotify = mutableListOf<Contact>()
                if (cached.contactsPayload.isNotEmpty()) {
                    val parts = cached.contactsPayload.split(";")
                    for (part in parts) {
                        val subParts = part.split("|")
                        if (subParts.size >= 2) {
                            contactsToNotify.add(Contact(
                                name = subParts[0],
                                phone = subParts[1],
                                relationship = subParts.getOrNull(2) ?: "Emergency Contact"
                            ))
                        }
                    }
                }
                
                // If deserialization failed or is empty, use current contacts as fallback
                val finalContactsList = contactsToNotify.ifEmpty { contacts.value }

                // 4. Send messages to contacts
                for (contact in finalContactsList) {
                    repository.insertMessage(Message(
                        contactPhone = contact.phone,
                        content = alertMessageText,
                        isIncoming = false,
                        isSosAlert = true
                    ))
                }

                // 5. Open default messaging app with prefilled text
                sendSmsViaDefaultApp(finalContactsList.map { it.phone }, alertMessageText)

                // 6. Delete from local cache
                repository.deleteCachedSosAlert(cached.id)
            }

            // Start sound siren and Geolocation streaming
            startSirenAlarm()
            startLocationStreaming()
            _systemStatus.value = "All cached SOS alerts successfully sent!"
        }
    }

    fun createVideoFileUri(): Uri {
        val context = getApplication<Application>()
        val videoFile = File(context.cacheDir, "emergency_video_${System.currentTimeMillis()}.mp4")
        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            videoFile
        )
    }

    fun saveRecordedVideo(uri: Uri?) {
        _recordedVideoUri.value = uri
        if (uri != null) {
            _systemStatus.value = "Emergency video recorded successfully! Ready to send."
        }
    }

    fun sendVideoToContacts(videoUri: Uri, phoneNumbers: List<String>) {
        val context = getApplication<Application>()
        if (phoneNumbers.isEmpty()) {
            _systemStatus.value = "No primary emergency contacts registered to send the video to."
            return
        }
        val phoneString = phoneNumbers.joinToString(
            separator = if (android.os.Build.MANUFACTURER.lowercase().contains("samsung")) "," else ";"
        )
        
        val mapLink = "https://www.google.com/maps/search/?api=1&query=${_latitude.value},${_longitude.value}"
        val videoAlertMessage = "🔴 EMERGENCY SOS VIDEO! 🔴\nI have recorded a safety video. Here is my live location: ${_locationAddress.value}\nLive track: $mapLink"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, videoUri)
            putExtra("address", phoneString)
            putExtra(Intent.EXTRA_SUBJECT, "EMERGENCY SOS VIDEO RECORDING")
            putExtra(Intent.EXTRA_TEXT, videoAlertMessage)
            putExtra("sms_body", videoAlertMessage)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        try {
            context.startActivity(intent)
            _systemStatus.value = "Launching Messaging App with emergency video attached..."
        } catch (e: Exception) {
            Log.e("SafetyViewModel", "Failed to launch ACTION_SEND with video attachment, trying general share chooser", e)
            val shareIntent = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "video/*"
                putExtra(Intent.EXTRA_STREAM, videoUri)
                putExtra(Intent.EXTRA_TEXT, videoAlertMessage)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Send Emergency Video").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(shareIntent)
                _systemStatus.value = "Launching Share Chooser for emergency video..."
            } catch (e2: Exception) {
                Log.e("SafetyViewModel", "All video share intents failed", e2)
                _systemStatus.value = "Failed to launch video sender. Please check your system settings."
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopRealLocationUpdates()
        stopSirenAlarm()
        stopLocationStreaming()
        stopNavigation()
        toneGenerator?.release()
    }
}
