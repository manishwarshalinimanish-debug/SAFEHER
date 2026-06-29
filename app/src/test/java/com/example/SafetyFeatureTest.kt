package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ui.SafetyViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SafetyFeatureTest {

    private lateinit var app: Application
    private lateinit var viewModel: SafetyViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        viewModel = SafetyViewModel(app)
    }

    // Helper to wait until a condition is met in our state flows
    private fun <T> awaitState(
        flowGetter: () -> T,
        timeoutMs: Long = 3000,
        condition: (T) -> Boolean
    ): T {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val currentValue = flowGetter()
            if (condition(currentValue)) {
                return currentValue
            }
            Thread.sleep(100)
        }
        return flowGetter()
    }

    @Test
    fun testSelectDestinationAndRoute() {
        // Initially, no destination is selected
        assertNull(viewModel.selectedDestination.value)

        // Select Union Square
        viewModel.selectDestination("Union Square")
        assertEquals("Union Square", viewModel.selectedDestination.value)

        // Default route is Safest (index 0)
        assertEquals(0, viewModel.currentRouteIndex.value)

        // Select Direct Route (Index 1)
        viewModel.selectRoute(1)
        assertEquals(1, viewModel.currentRouteIndex.value)

        // Select Shield Route (Index 0)
        viewModel.selectRoute(0)
        assertEquals(0, viewModel.currentRouteIndex.value)

        // Deselect destination
        viewModel.selectDestination(null)
        assertNull(viewModel.selectedDestination.value)
    }

    @Test
    fun testOnlineStatusToggle() {
        org.junit.Assert.assertTrue(viewModel.isOnline.value)
        viewModel.setOnlineStatus(false)
        org.junit.Assert.assertFalse(viewModel.isOnline.value)
        viewModel.setOnlineStatus(true)
        org.junit.Assert.assertTrue(viewModel.isOnline.value)
    }

    @Test
    fun testOfflineCachingRepositoryDirect() = kotlinx.coroutines.runBlocking {
        val database = com.example.data.database.SafetyDatabase.getDatabase(app)
        val dao = database.safetyDao()
        
        // Ensure empty cache initially
        dao.clearAllCachedSosAlerts()
        
        val cached = com.example.data.entity.CachedSosAlert(
            latitude = 37.7749,
            longitude = -122.4194,
            locationAddress = "Test Area",
            contactsPayload = "Mom|12345"
        )
        
        dao.insertCachedSosAlert(cached)
        
        val cachedList = dao.getCachedSosAlertsList()
        org.junit.Assert.assertEquals(1, cachedList.size)
        org.junit.Assert.assertEquals("Test Area", cachedList.first().locationAddress)
        
        dao.clearAllCachedSosAlerts()
        org.junit.Assert.assertEquals(0, dao.getCachedSosAlertsList().size)
    }
}
