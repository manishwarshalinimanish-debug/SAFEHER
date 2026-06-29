package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import android.net.Uri
import com.example.data.entity.Contact
import com.example.data.entity.Message
import com.example.data.entity.AlertLog
import com.example.sensor.ShakeDetector
import com.example.ui.SafetyViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private val viewModel: SafetyViewModel by viewModels()
    private var sensorManager: SensorManager? = null
    private var shakeDetector: ShakeDetector? = null
    private var accelerometer: Sensor? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        viewModel.setLocationPermissionGranted(fineGranted || coarseGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check current location permissions on startup
        checkLocationPermissions()

        // Register Shake Sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        shakeDetector = ShakeDetector {
            runOnUiThread {
                viewModel.handleShakeTriggered()
            }
        }

        setContent {
            MyApplicationTheme {
                MainSafetyScreen(
                    viewModel = viewModel,
                    onRequestPermissions = {
                        requestPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
            }
        }
    }

    private fun checkLocationPermissions() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.setLocationPermissionGranted(fineGranted || coarseGranted)
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager?.registerListener(shakeDetector, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(shakeDetector)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainSafetyScreen(
    viewModel: SafetyViewModel,
    onRequestPermissions: () -> Unit
) {
    val activeContact by viewModel.selectedContact.collectAsStateWithLifecycle()
    var activeTab by remember { mutableStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (activeContact == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8DEF8)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = "Shield",
                                tint = Color(0xFF21005D),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Guardian",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Advanced Safety Active",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { /* Notifications placeholder */ },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Notifications"
                            )
                        }
                        IconButton(
                            onClick = { /* Account placeholder */ },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = "Account"
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Hide the bottom navigation bar when inside an active messaging chat
            if (activeContact == null) {
                NavigationBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        icon = { Icon(Icons.Filled.Shield, contentDescription = "Shield") },
                        label = { Text("Shield", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    NavigationBarItem(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        icon = { Icon(Icons.Filled.People, contentDescription = "Guardians") },
                        label = { Text("Guardians", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    NavigationBarItem(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        icon = { Icon(Icons.Filled.Forum, contentDescription = "Messenger") },
                        label = { Text("Messenger", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = if (activeContact != null) -1 else activeTab,
                transitionSpec = {
                    if (targetState == -1) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else if (initialState == -1) {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    } else {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    }
                },
                label = "ScreenTransition"
            ) { targetTab ->
                when (targetTab) {
                    -1 -> {
                        // Full immersive chat view
                        ChatScreen(
                            contact = activeContact!!,
                            viewModel = viewModel,
                            onBack = { viewModel.selectContact(null) }
                        )
                    }
                    0 -> {
                        DashboardScreen(
                            viewModel = viewModel,
                            onRequestPermissions = onRequestPermissions
                        )
                    }
                    1 -> {
                        GuardiansScreen(
                            viewModel = viewModel,
                            onChatClick = { contact -> viewModel.selectContact(contact) }
                        )
                    }
                    2 -> {
                        MessengerScreen(
                            viewModel = viewModel,
                            onSelectContact = { contact -> viewModel.selectContact(contact) }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 1: SAFETY DASHBOARD
// ==========================================
@Composable
fun DashboardScreen(
    viewModel: SafetyViewModel,
    onRequestPermissions: () -> Unit
) {
    val systemStatus by viewModel.systemStatus.collectAsStateWithLifecycle()
    val isPermissionGranted by viewModel.isLocationPermissionGranted.collectAsStateWithLifecycle()
    val shakeEnabled by viewModel.shakeEnabled.collectAsStateWithLifecycle()
    val countdown by viewModel.sosCountdown.collectAsStateWithLifecycle()
    val isSirenPlaying by viewModel.isSirenPlaying.collectAsStateWithLifecycle()
    val activeAlert by viewModel.activeAlert.collectAsStateWithLifecycle()
    val isStreamingLocation by viewModel.isStreamingLocation.collectAsStateWithLifecycle()
    val gpsStreamPath by viewModel.gpsStreamPath.collectAsStateWithLifecycle()
    
    val lat by viewModel.latitude.collectAsStateWithLifecycle()
    val lon by viewModel.longitude.collectAsStateWithLifecycle()
    val address by viewModel.locationAddress.collectAsStateWithLifecycle()
    val logs by viewModel.alertLogs.collectAsStateWithLifecycle()

    val hotspots by viewModel.hotspots.collectAsStateWithLifecycle()
    val selectedDestination by viewModel.selectedDestination.collectAsStateWithLifecycle()
    val currentRouteIndex by viewModel.currentRouteIndex.collectAsStateWithLifecycle()
    val isNavigating by viewModel.isNavigating.collectAsStateWithLifecycle()
    val navigationStatus by viewModel.navigationStatus.collectAsStateWithLifecycle()

    val listState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(listState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Glowing Security Hero Header with generated asset
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
        ) {
            Image(
                painter = painterResource(id = com.example.R.drawable.safety_banner),
                contentDescription = "Safety banner background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Beautiful linear overlay for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                            startY = 100f
                        )
                    )
            )

            // Live security badge on the banner
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (activeAlert != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary)
                    )
                    Text(
                        text = if (activeAlert != null) "SOS ALERT ACTIVE" else "SHIELD ACTIVE",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Aegis Real-time Guard Mode",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // System status toast banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (activeAlert != null || countdown != null) 
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) 
                    else MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (activeAlert != null) Icons.Filled.Warning else Icons.Filled.Security,
                    contentDescription = "Status Icon",
                    tint = if (activeAlert != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = systemStatus,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
        val cachedSosAlerts by viewModel.cachedSosAlerts.collectAsStateWithLifecycle()

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("connectivity_simulation_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (!isOnline) 
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) 
                    else MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, if (!isOnline) MaterialTheme.colorScheme.error.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isOnline) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                            contentDescription = "Connection Status",
                            tint = if (isOnline) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                        Column {
                            Text(
                                text = "Connectivity Simulation",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isOnline) "Status: ONLINE (Immediate Alerts)" else "Status: OFFLINE (Cached Alerts)",
                                fontSize = 11.sp,
                                color = if (isOnline) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Switch(
                        checked = isOnline,
                        onCheckedChange = { viewModel.setOnlineStatus(it) },
                        modifier = Modifier.testTag("connectivity_toggle"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.tertiary,
                            checkedTrackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                        )
                    )
                }

                if (cachedSosAlerts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        )
                        Text(
                            text = "${cachedSosAlerts.size} SOS Alerts Cached Locally",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Once connection is toggled ON, these alerts will immediately dispatch to emergency contacts.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        cachedSosAlerts.forEach { alert ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = alert.locationAddress,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Coords: ${String.format(Locale.US, "%.4f", alert.latitude)}, ${String.format(Locale.US, "%.4f", alert.longitude)}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                    contentColor = MaterialTheme.colorScheme.error
                                ) {
                                    Text(
                                        text = "READY",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (activeAlert != null || isStreamingLocation) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_gps_streaming_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GpsFixed,
                                contentDescription = "Streaming Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Live Geolocation Stream",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        // Pulsing online dot
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "StreamPulse")
                            val streamAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.2f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse),
                                label = "StreamAlpha"
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFB3261E).copy(alpha = streamAlpha))
                            )
                            Text(
                                text = "STREAMING (HTTPS/WSS)",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB3261E)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Your GPS coordinates are streaming in real-time using the Geolocation API to your emergency contacts' live dashboard.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Simulated dashboard active viewers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Viewer",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Mom (Primary Contact) is viewing your live dashboard screen.",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "RECENT COORDINATE STREAM TELEMETRY:",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Stream coordinates items
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val displayList = gpsStreamPath.takeLast(4).reversed()
                        if (displayList.isEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Connecting pipeline...",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "PENDING",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            displayList.forEachIndexed { index, item ->
                                val timeStr = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(item.first))
                                val (latitude, longitude) = item.second
                                val packetId = 20400 + (item.first % 1000).toInt()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (index == 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) 
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), 
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (index == 0) Icons.Filled.CheckCircle else Icons.Filled.History,
                                            contentDescription = "Status",
                                            tint = if (index == 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Packet #$packetId: (${String.format(Locale.US, "%.5f", latitude)}, ${String.format(Locale.US, "%.5f", longitude)})",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = if (index == 0) "LIVE" else timeStr,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (index == 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // High Density Style Emergency SOS Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF2B8B5))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (countdown != null) {
                        // Countdown Mode
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "$countdown",
                                color = Color(0xFFB3261E),
                                fontSize = 58.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "TRIGGERING SOS",
                                color = Color(0xFF31111D),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.cancelSosCountdown() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E)),
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("cancel_sos_countdown_button")
                            ) {
                                Text("CANCEL", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (activeAlert != null) {
                        // SOS Active Blaring Mode UI
                        Button(
                            onClick = { viewModel.resolveActiveAlert() },
                            modifier = Modifier
                                .size(110.dp)
                                .testTag("resolve_sos_button"),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E)),
                            border = BorderStroke(4.dp, Color(0xFFF9DEDC)),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.NotificationsActive, contentDescription = "Active Alert", modifier = Modifier.size(24.dp), tint = Color.White)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "RESOLVE",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "SOS ACTIVE",
                                color = Color(0xFF31111D),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Tap Resolve button once you are safe",
                                color = Color(0xFF601410).copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        // Default Standard Ready Mode (SOS Button matching HTML)
                        Button(
                            onClick = { viewModel.triggerSosAlert() },
                            modifier = Modifier
                                .size(110.dp)
                                .testTag("trigger_sos_button"),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E)),
                            border = BorderStroke(4.dp, Color(0xFFF9DEDC)),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "SOS",
                                    color = Color.White,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Emergency SOS",
                                color = Color(0xFF31111D),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "SHAKE OR TAP TO ACTIVATE",
                                color = Color(0xFF601410).copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Shake Setting Switch Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Filled.EdgesensorHigh,
                        contentDescription = "Shake Sensor",
                        tint = if (shakeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column {
                        Text("Shake to Trigger SOS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "Forcefully shake phone to activate 5s countdown dispatch",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = shakeEnabled,
                    onCheckedChange = { viewModel.toggleShakeEnabled() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("shake_sos_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SecureVideoToolCard(viewModel = viewModel)

        Spacer(modifier = Modifier.height(16.dp))

        // Live Tracker GPS Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.GpsFixed, contentDescription = "GPS", tint = MaterialTheme.colorScheme.tertiary)
                        Text("Live Location tracking", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }
                    if (!isPermissionGranted) {
                        Button(
                            onClick = onRequestPermissions,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Grant GPS", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Badge(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)) {
                            Text("GPS ONLINE", color = MaterialTheme.colorScheme.tertiary, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Futuristic Live Pulse Tracker Map (Canvas drawing instead of map library)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
                    val pulseRadius1 by infiniteTransition.animateFloat(
                        initialValue = 10f,
                        targetValue = 260f,
                        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart),
                        label = "Pulse1"
                    )
                    val pulseRadius2 by infiniteTransition.animateFloat(
                        initialValue = 10f,
                        targetValue = 260f,
                        animationSpec = infiniteRepeatable(tween(2500, delayMillis = 1250, easing = LinearEasing), RepeatMode.Restart),
                        label = "Pulse2"
                    )
                    val scannerAngle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Restart),
                        label = "Scanner"
                    )

                    val trackerColor = MaterialTheme.colorScheme.tertiary

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2
                        val cy = size.height / 2

                        // Drawing grids
                        drawLine(Color.White.copy(alpha = 0.05f), Offset(0f, cy), Offset(size.width, cy), strokeWidth = 1f)
                        drawLine(Color.White.copy(alpha = 0.05f), Offset(cx, 0f), Offset(cx, size.height), strokeWidth = 1f)

                        // Outer concentric circles
                        drawCircle(Color.White.copy(alpha = 0.03f), radius = 50f, style = Stroke(1f))
                        drawCircle(Color.White.copy(alpha = 0.03f), radius = 100f, style = Stroke(1f))
                        drawCircle(Color.White.copy(alpha = 0.03f), radius = 150f, style = Stroke(1f))

                        // Radiating Pulses
                        drawCircle(trackerColor.copy(alpha = (1f - (pulseRadius1 / 260f)).coerceIn(0f, 0.4f)), radius = pulseRadius1, style = Stroke(1.5f))
                        drawCircle(trackerColor.copy(alpha = (1f - (pulseRadius2 / 260f)).coerceIn(0f, 0.4f)), radius = pulseRadius2, style = Stroke(1.5f))

                        // Core position pointer
                        drawCircle(trackerColor, radius = 6f)
                        drawCircle(Color.White, radius = 2f)
                        
                        // Sweeping radar beam
                        val radarRadius = size.maxDimension
                        val sweepRad = Math.toRadians(scannerAngle.toDouble())
                        val endX = cx + (radarRadius * Math.cos(sweepRad)).toFloat()
                        val endY = cy + (radarRadius * Math.sin(sweepRad)).toFloat()
                        drawLine(trackerColor.copy(alpha = 0.15f), Offset(cx, cy), Offset(endX, endY), strokeWidth = 2f)
                    }

                    // Floating details
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text("Lat: ${String.format(Locale.US, "%.5f", lat)}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("Lon: ${String.format(Locale.US, "%.5f", lon)}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = address,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Google Maps Tracking Link active for dispatch triggers.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Safest Route Suggestion Hub Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("safest_route_hub_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Directions,
                        contentDescription = "Directions Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Safest Route Advisor",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Powered by crime hotspots and lit street mapping",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Vector Canvas Map
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF121212)) // Dark nighttime map theme
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
                        label = "Pulse"
                    )

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2
                        val cy = size.height / 2
                        val scaleX = size.width / 0.045f
                        val scaleY = size.height / 0.045f

                        // Center projection around coordinates in between SF start and targets
                        val centerLat = 37.7810
                        val centerLng = -122.4140

                        fun project(latitude: Double, longitude: Double): Offset {
                            val x = cx + ((longitude - centerLng) * scaleX).toFloat()
                            val y = cy - ((latitude - centerLat) * scaleY).toFloat()
                            return Offset(x, y)
                        }

                        // 1. Draw base grid/streets for vector map look
                        val streetColor = Color.White.copy(alpha = 0.1f)
                        // Diagonal main corridors (Market St and Mission St)
                        drawLine(streetColor, project(37.7700, -122.4260), project(37.7950, -122.4000), strokeWidth = 3f)
                        drawLine(streetColor, project(37.7650, -122.4220), project(37.7900, -122.3950), strokeWidth = 2f)
                        // Major arterial crossings
                        drawLine(streetColor, project(37.7700, -122.4210), project(37.8000, -122.4210), strokeWidth = 3f) // Van Ness Ave
                        drawLine(streetColor, project(37.7980, -122.4250), project(37.7980, -122.4000), strokeWidth = 2f) // Broadway

                        // 2. Draw Safety Hotspots from DB with glowing radar pulses
                        hotspots.forEach { hotspot ->
                            val pos = project(hotspot.latitude, hotspot.longitude)
                            val riskColor = if (hotspot.riskLevel == "HIGH") Color(0xFFF44336) else Color(0xFFFF9800)
                            
                            // Glowing halo
                            drawCircle(
                                color = riskColor.copy(alpha = 0.15f * (1.2f - pulseScale)),
                                radius = 45f * pulseScale,
                                center = pos
                            )
                            drawCircle(
                                color = riskColor.copy(alpha = 0.25f),
                                radius = 18f,
                                center = pos
                            )
                            drawCircle(
                                color = riskColor,
                                radius = 6f,
                                center = pos
                            )
                        }

                        // 3. Draw Route paths if destination is selected
                        if (selectedDestination != null) {
                            val safestPath = when (selectedDestination) {
                                "Union Square" -> listOf(
                                    project(37.7749, -122.4194),
                                    project(37.7780, -122.4120),
                                    project(37.7840, -122.4080),
                                    project(37.7879, -122.4074)
                                )
                                "Mission District" -> listOf(
                                    project(37.7749, -122.4194),
                                    project(37.7700, -122.4230),
                                    project(37.7610, -122.4210),
                                    project(37.7599, -122.4148)
                                )
                                "Coit Tower" -> listOf(
                                    project(37.7749, -122.4194),
                                    project(37.7820, -122.4050),
                                    project(37.7940, -122.3950),
                                    project(37.8024, -122.4058)
                                )
                                else -> emptyList()
                            }

                            val directPath = when (selectedDestination) {
                                "Union Square" -> listOf(
                                    project(37.7749, -122.4194),
                                    project(37.7833, -122.4167), // Tenderloin hotspot
                                    project(37.7879, -122.4074)
                                )
                                "Mission District" -> listOf(
                                    project(37.7749, -122.4194),
                                    project(37.7650, -122.4190), // Mission hotspot
                                    project(37.7599, -122.4148)
                                )
                                "Coit Tower" -> listOf(
                                    project(37.7749, -122.4194),
                                    project(37.7770, -122.4100), // Soma hotspot
                                    project(37.8024, -122.4058)
                                )
                                else -> emptyList()
                            }

                            // Draw Direct Route in Red dashed line
                            if (directPath.isNotEmpty()) {
                                for (i in 0 until directPath.size - 1) {
                                    drawLine(
                                        color = Color(0xFFFF5252).copy(alpha = if (currentRouteIndex == 1) 0.95f else 0.4f),
                                        start = directPath[i],
                                        end = directPath[i + 1],
                                        strokeWidth = 6f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                                    )
                                }
                            }

                            // Draw Shield Route in Green glowing solid line
                            if (safestPath.isNotEmpty()) {
                                for (i in 0 until safestPath.size - 1) {
                                    // Ambient green outer glow
                                    drawLine(
                                        color = Color(0xFF00E676).copy(alpha = if (currentRouteIndex == 0) 0.35f else 0.15f),
                                        start = safestPath[i],
                                        end = safestPath[i+1],
                                        strokeWidth = 14f
                                    )
                                    // Solid primary green route line
                                    drawLine(
                                        color = Color(0xFF00E676).copy(alpha = if (currentRouteIndex == 0) 0.95f else 0.45f),
                                        start = safestPath[i],
                                        end = safestPath[i + 1],
                                        strokeWidth = 7f
                                    )
                                }
                            }

                            // 4. Draw Destination Marker
                            val destPos = when (selectedDestination) {
                                "Union Square" -> project(37.7879, -122.4074)
                                "Mission District" -> project(37.7599, -122.4148)
                                "Coit Tower" -> project(37.8024, -122.4058)
                                else -> Offset(0f, 0f)
                            }
                            if (destPos != Offset(0f, 0f)) {
                                drawCircle(Color(0xFFE91E63), radius = 9f, center = destPos)
                                drawCircle(Color.White, radius = 4f, center = destPos)
                            }
                        }

                        // 5. Draw YOU Start Point with continuous pulse
                        val youPos = project(lat, lon)
                        drawCircle(Color(0xFF29B6F6).copy(alpha = 0.35f * (1.2f - pulseScale)), radius = 18f * pulseScale, center = youPos)
                        drawCircle(Color(0xFF29B6F6), radius = 7f, center = youPos)
                        drawCircle(Color.White, radius = 2.5f, center = youPos)
                    }

                    // Map overlay labels
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF00E676), CircleShape))
                            Text("Safest Route", color = Color.White, fontSize = 9.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFFFF5252), CircleShape))
                            Text("Fastest (Unsafe)", color = Color.White, fontSize = 9.sp)
                        }
                    }

                    // Display list of safety hazards on the map bounds
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ACTIVE HOTSPOTS: ${hotspots.size}",
                            color = Color(0xFFFFB74D),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Destination Selector dropdown list
                Text(
                    text = "SELECT YOUR DESTINATION:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val destinations = listOf("Union Square", "Mission District", "Coit Tower")
                    destinations.forEach { dest ->
                        val isSelected = selectedDestination == dest
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    viewModel.selectDestination(null)
                                } else {
                                    viewModel.selectDestination(dest)
                                }
                            },
                            label = { Text(dest, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("destination_chip_$dest")
                        )
                    }
                }

                if (selectedDestination != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Route selection tabs
                    Text(
                        text = "ROUTE AUDIT COMPARISON:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Safest Route Card Button
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.selectRoute(0) }
                                .testTag("select_safest_route_button"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (currentRouteIndex == 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                                 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            border = BorderStroke(
                                width = if (currentRouteIndex == 0) 2.dp else 1.dp,
                                color = if (currentRouteIndex == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Shield,
                                        contentDescription = "Safe",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text("Shield Route", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("1.9 mi • 8 mins", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "100% well-lit streets.\nAvoids Tenderloin.",
                                    fontSize = 9.sp,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 11.sp
                                )
                            }
                        }

                        // Direct Route Card Button
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.selectRoute(1) }
                                .testTag("select_direct_route_button"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (currentRouteIndex == 1) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                                 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            border = BorderStroke(
                                width = if (currentRouteIndex == 1) 2.dp else 1.dp,
                                color = if (currentRouteIndex == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = "Warning",
                                        tint = Color(0xFFFF9800),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text("Direct Route", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("1.5 mi • 5 mins", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Crosses high theft\ncrime hotspots.",
                                    fontSize = 9.sp,
                                    color = Color(0xFFE53935),
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Route Safety Status message
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = navigationStatus,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Navigation Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isNavigating) {
                            Button(
                                onClick = { viewModel.startNavigation() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (currentRouteIndex == 0) Color(0xFF4CAF50) else Color(0xFF2196F3)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("start_safety_navigation_button")
                            ) {
                                Icon(Icons.Filled.Navigation, contentDescription = "Nav", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start Shield Navigation", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.stopNavigation() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("stop_safety_navigation_button")
                            ) {
                                Icon(Icons.Filled.Cancel, contentDescription = "Cancel", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cancel Navigation", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(12.dp))

                // Report a custom Hotspot panel
                var showReportForm by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showReportForm = !showReportForm }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddLocationAlt,
                            contentDescription = "Report Hotspot Icon",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Report Local Safety Hotspot",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        imageVector = if (showReportForm) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (showReportForm) {
                    Spacer(modifier = Modifier.height(12.dp))

                    var title by remember { mutableStateOf("") }
                    var desc by remember { mutableStateOf("") }
                    var risk by remember { mutableStateOf("HIGH") }

                    Text(
                        text = "Mark a hazard or unsafe condition at your current location:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Hotspot Title (e.g. Unlit Alleyway)", fontSize = 11.sp) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("report_hotspot_title_field"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Details or incident context", fontSize = 11.sp) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("report_hotspot_desc_field"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Risk Severity:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("LOW", "MEDIUM", "HIGH").forEach { r ->
                                FilterChip(
                                    selected = risk == r,
                                    onClick = { risk = r },
                                    label = { Text(r, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = when(r) {
                                            "HIGH" -> Color(0xFFF44336).copy(alpha = 0.2f)
                                            "MEDIUM" -> Color(0xFFFF9800).copy(alpha = 0.2f)
                                            else -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                        },
                                        selectedLabelColor = when(r) {
                                            "HIGH" -> Color(0xFFD32F2F)
                                            "MEDIUM" -> Color(0xFFF57C00)
                                            else -> Color(0xFF388E3C)
                                        }
                                    ),
                                    modifier = Modifier.testTag("report_risk_chip_$r")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                viewModel.reportHotspot(title, desc, risk, lat, lon)
                                title = ""
                                desc = ""
                                showReportForm = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_report_hotspot_button")
                    ) {
                        Icon(Icons.Filled.PinDrop, contentDescription = "Pin", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Submit & Place Safety Pin", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Activity Logs Panel
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Filled.History, contentDescription = "History", tint = MaterialTheme.colorScheme.primary)
            Text("Emergency Activity Logs", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No SOS triggers logged. System secure.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    logs.take(3).forEach { log ->
                        val timeString = remember(log.timestamp) {
                            SimpleDateFormat("MMM dd, hh:mm:ss a", Locale.getDefault()).format(Date(log.timestamp))
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (log.status == "ACTIVE") "🚨 Emergency Active" else "🛡️ Resolved",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (log.status == "ACTIVE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = timeString,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Track link shared",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 2: EMERGENCY CONTACTS (GUARDIANS)
// ==========================================
@Composable
fun GuardiansScreen(
    viewModel: SafetyViewModel,
    onChatClick: (Contact) -> Unit
) {
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Explanatory Banner Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Filled.PeopleAlt, contentDescription = "Guardians Info", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Column {
                    Text("Emergency Contacts", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("These individuals will be notified instantly when you shake your phone or press the SOS button.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Your Protective Circle", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_guardian_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                    Text("Add Guardian", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.People, contentDescription = "Empty", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No guardians registered.", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Add your trusted contacts to start.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(contacts, key = { it.id }) { contact ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // Monogram Avatar Circle with Gradient
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.secondary
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = contact.name.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = contact.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (contact.isPrimary) {
                                            Badge(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) {
                                                Text("PRIMARY", color = MaterialTheme.colorScheme.primary, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                    Text(contact.phone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = contact.relationship,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { onChatClick(contact) },
                                    modifier = Modifier.testTag("chat_contact_${contact.id}")
                                ) {
                                    Icon(Icons.Filled.Forum, contentDescription = "Chat", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(
                                    onClick = { viewModel.removeContact(contact) },
                                    modifier = Modifier.testTag("remove_contact_${contact.id}")
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Contact Dialog
        if (showAddDialog) {
            AddContactDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, phone, relation, isPrimary ->
                    viewModel.addEmergencyContact(name, phone, relation, isPrimary)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun AddContactDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("Mother") }
    var isPrimary by remember { mutableStateOf(false) }

    val relationOptions = listOf("Mother", "Father", "Sister", "Brother", "Spouse", "Friend", "Official")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add Emergency Guardian",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_contact_name")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_contact_phone")
                )

                // Relationship selection
                Column {
                    Text("Relationship", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        relationOptions.forEach { option ->
                            val selected = relationship == option
                            FilterChip(
                                selected = selected,
                                onClick = { relationship = option },
                                label = { Text(option, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }

                // Primary checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isPrimary,
                        onCheckedChange = { isPrimary = it },
                        modifier = Modifier.testTag("checkbox_contact_primary")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Set as Primary SOS Recipient", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onAdd(name, phone, relationship, isPrimary) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("submit_add_contact_button")
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 3: MESSENGER (INBOX VIEW)
// ==========================================
@Composable
fun MessengerScreen(
    viewModel: SafetyViewModel,
    onSelectContact: (Contact) -> Unit
) {
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val allMessages by viewModel.messages.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.Forum, contentDescription = "Messenger", tint = MaterialTheme.colorScheme.primary)
            Text("Emergency Messenger", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        }
        Text(
            text = "Active encrypted chat logs with your registered emergency contacts.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Register guardians in the 'Guardians' tab to activate messenger chats.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(contacts, key = { it.id }) { contact ->
                    val contactMessages = remember(allMessages, contact.phone) {
                        allMessages.filter { it.contactPhone == contact.phone }
                    }
                    val lastMsg = contactMessages.lastOrNull()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectContact(contact) }
                            .testTag("conversation_row_${contact.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Avatar Monogram
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.surfaceVariant,
                                                MaterialTheme.colorScheme.borderSecondary()
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = contact.name.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 18.sp
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = contact.name,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                    if (lastMsg != null) {
                                        val timeStr = remember(lastMsg.timestamp) {
                                            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(lastMsg.timestamp))
                                        }
                                        Text(timeStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = lastMsg?.content ?: "No messages in history. Start conversation.",
                                    fontSize = 12.sp,
                                    color = if (lastMsg?.isSosAlert == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (lastMsg?.isSosAlert == true) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Extension to avoid compiler missing property on border colors
@Composable
fun ColorScheme.borderSecondary() = this.surfaceVariant

// ==========================================
// FULL IMMERSIVE CHAT VIEW
// ==========================================
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ChatScreen(
    contact: Contact,
    viewModel: SafetyViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val allMessages by viewModel.messages.collectAsStateWithLifecycle()
    val chatMessages = remember(allMessages, contact.phone) {
        allMessages.filter { it.contactPhone == contact.phone }
    }
    
    var inputText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Permission state
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )
    
    var tempChatVideoUri by remember { mutableStateOf<Uri?>(null) }
    var showSendVideoDialog by remember { mutableStateOf<Uri?>(null) }
    
    val videoCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success) {
            tempChatVideoUri?.let { uri ->
                showSendVideoDialog = uri
            }
        } else {
            Toast.makeText(context, "Video recording canceled", Toast.LENGTH_SHORT).show()
        }
    }

    // Automatically scroll to bottom on new message delivery
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            scrollState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    val quickResponses = listOf(
        "I am safe now.",
        "I feel scared, track me!",
        "Just leaving now, walking home.",
        "Please call police if I don't reply."
    )

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Chat screen header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("chat_back_arrow")
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = contact.name.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Column {
                        Text(
                            contact.name,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary))
                            Text("Active Now", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Messages list
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            if (chatMessages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No message history. Write a secure text or send an alert.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(chatMessages, key = { it.id }) { message ->
                        val isSos = message.isSosAlert
                        val isMe = !message.isIncoming

                        val bubbleBg = when {
                            isSos -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            isMe -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surface
                        }

                        val bubbleBorder = if (isSos) 
                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary) 
                            else null

                        val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = alignment
                        ) {
                            Column(
                                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                Card(
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isMe) 16.dp else 2.dp,
                                        bottomEnd = if (isMe) 2.dp else 16.dp
                                    ),
                                    colors = CardDefaults.cardColors(containerColor = bubbleBg),
                                    border = bubbleBorder,
                                    modifier = Modifier.testTag("chat_bubble_${message.id}")
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = message.content,
                                            color = if (isMe && !isSos) Color.White else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp
                                        )
                                        
                                        // Clickable Maps indicator for alert bubbles
                                        if (isSos && message.content.contains("maps")) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Filled.Map, contentDescription = "Run Map", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                                Text("Open GPS Tracker", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                val timeLabel = remember(message.timestamp) {
                                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))
                                }
                                Text(
                                    text = timeLabel,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick responses strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickResponses.forEach { phrase ->
                Card(
                    modifier = Modifier.clickable {
                        viewModel.sendMessageToSelectedContact(phrase)
                        keyboardController?.hide()
                    },
                    shape = RoundedCornerShape(50),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = phrase,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Send Text Bar Area
        Surface(
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        if (permissionsState.allPermissionsGranted) {
                            val newUri = viewModel.createVideoFileUri()
                            tempChatVideoUri = newUri
                            videoCaptureLauncher.launch(newUri)
                        } else {
                            permissionsState.launchMultiplePermissionRequest()
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("chat_video_record_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Videocam,
                        contentDescription = "Record secure video clip",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Send secure message...", fontSize = 13.sp) },
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send,
                        keyboardType = KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessageToSelectedContact(inputText)
                                inputText = ""
                                keyboardController?.hide()
                            }
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessageToSelectedContact(inputText)
                            inputText = ""
                            keyboardController?.hide()
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag("chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (showSendVideoDialog != null) {
            AlertDialog(
                onDismissRequest = { showSendVideoDialog = null },
                title = { Text("Send Emergency Video?", fontWeight = FontWeight.Bold) },
                text = { Text("Would you like to send this emergency secure video recording to ${contact.name}?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showSendVideoDialog?.let { uri ->
                                viewModel.sendVideoToContacts(uri, listOf(contact.phone))
                            }
                            showSendVideoDialog = null
                        }
                    ) {
                        Text("Send Video")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSendVideoDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun VideoPlayerDialog(
    videoUri: Uri,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Emergency Video Preview", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        android.widget.VideoView(ctx).apply {
                            setVideoURI(videoUri)
                            val mediaController = android.widget.MediaController(ctx)
                            mediaController.setAnchorView(this)
                            setMediaController(mediaController)
                            start()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close Preview")
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SecureVideoToolCard(
    viewModel: SafetyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recordedVideoUri by viewModel.recordedVideoUri.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    
    var showPlayerDialog by remember { mutableStateOf(false) }
    var tempVideoUri by remember { mutableStateOf<Uri?>(null) }
    
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )
    
    val videoCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success) {
            tempVideoUri?.let { uri ->
                viewModel.saveRecordedVideo(uri)
            }
        } else {
            Toast.makeText(context, "Video recording canceled", Toast.LENGTH_SHORT).show()
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("secure_video_tool_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Videocam,
                        contentDescription = "Security Camera",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                Column {
                    Text(
                        text = "Secure Video Evidence",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Record real-time video to share with guardians",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Text(
                text = "In an emergency, capture a security clip. It is saved in private app storage and can be instantly dispatched as an alert attachment.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (recordedVideoUri != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f))
                        .border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Security Video Ready",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { showPlayerDialog = true },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play Video",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        IconButton(
                            onClick = {
                                recordedVideoUri?.let { uri ->
                                    viewModel.sendVideoToContacts(uri, contacts.map { it.phone })
                                }
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Share with contacts",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
            
            Button(
                onClick = {
                    if (permissionsState.allPermissionsGranted) {
                        val newUri = viewModel.createVideoFileUri()
                        tempVideoUri = newUri
                        videoCaptureLauncher.launch(newUri)
                    } else {
                        permissionsState.launchMultiplePermissionRequest()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("record_emergency_video_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (recordedVideoUri != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "Camera Icon",
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (recordedVideoUri != null) "Record New Video" else "Record Security Video",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
    
    if (showPlayerDialog) {
        recordedVideoUri?.let { uri ->
            VideoPlayerDialog(videoUri = uri, onDismiss = { showPlayerDialog = false })
        }
    }
}
