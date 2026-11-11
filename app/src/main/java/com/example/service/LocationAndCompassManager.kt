package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import com.example.model.NavigationGuide
import com.example.model.NavigationTarget
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val TAG = "LocationCompassManager"

class LocationAndCompassManager(
    private val context: Context,
    private val scope: CoroutineScope
) : SensorEventListener {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _currentAddress = MutableStateFlow("Locating current position...")
    val currentAddress: StateFlow<String> = _currentAddress.asStateFlow()

    private val _compassAzimuth = MutableStateFlow(0f)
    val compassAzimuth: StateFlow<Float> = _compassAzimuth.asStateFlow()

    private val _navigationGuide = MutableStateFlow<NavigationGuide?>(null)
    val navigationGuide: StateFlow<NavigationGuide?> = _navigationGuide.asStateFlow()

    private var activeDestination: NavigationTarget? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val lastLoc = result.lastLocation ?: return
            _currentLocation.value = lastLoc
            resolveAddress(lastLoc)
            updateNavigationGuide(lastLoc, _compassAzimuth.value)
        }
    }

    init {
        rotationSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        try {
            val request = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                4000L
            ).setMinUpdateIntervalMillis(2000L)
                .setMinUpdateDistanceMeters(2f)
                .build()

            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )

            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    _currentLocation.value = loc
                    resolveAddress(loc)
                    updateNavigationGuide(loc, _compassAzimuth.value)
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission missing", e)
            _currentAddress.value = "Location permission needed for navigation."
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start location updates", e)
        }
    }

    fun stopLocationUpdates() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            sensorManager?.unregisterListener(this)
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping location updates", e)
        }
    }

    fun setDestination(target: NavigationTarget?) {
        activeDestination = target
        _currentLocation.value?.let { loc ->
            updateNavigationGuide(loc, _compassAzimuth.value)
        }
    }

    private fun resolveAddress(loc: Location) {
        scope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(loc.latitude, loc.longitude, 1) { addresses ->
                        val addr = addresses.firstOrNull()?.toReadableString()
                        if (!addr.isNullOrBlank()) {
                            _currentAddress.value = addr
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val list = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                    val addr = list?.firstOrNull()?.toReadableString()
                    if (!addr.isNullOrBlank()) {
                        _currentAddress.value = addr
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Geocoder lookup failed", e)
            }
        }
    }

    private fun Address.toReadableString(): String {
        val street = thoroughfare ?: featureName ?: ""
        val subThoroughfare = subThoroughfare ?: ""
        val locality = locality ?: subAdminArea ?: ""
        return listOf(subThoroughfare, street, locality)
            .filter { it.isNotBlank() }
            .joinToString(", ")
            .ifBlank { "Coordinates: ${latitude.toString().take(7)}, ${longitude.toString().take(7)}" }
    }

    private fun updateNavigationGuide(currentLoc: Location, currentAzimuth: Float) {
        val dest = activeDestination ?: run {
            _navigationGuide.value = null
            return
        }

        val destLoc = Location("destination").apply {
            latitude = dest.latitude
            longitude = dest.longitude
        }

        val distanceMeters = currentLoc.distanceTo(destLoc)
        val bearingToDest = currentLoc.bearingTo(destLoc) // -180 to +180

        // Relative angle between current heading and destination
        var relativeAngle = (bearingToDest - currentAzimuth) % 360f
        if (relativeAngle < -180f) relativeAngle += 360f
        if (relativeAngle > 180f) relativeAngle -= 360f

        val (directionText, spoken) = when {
            distanceMeters <= 8f -> {
                "Arrived" to "You have reached your destination: ${dest.name}."
            }
            relativeAngle in -25.0..25.0 -> {
                "Straight ahead" to "Walk straight ahead for ${distanceMeters.roundToInt()} meters to ${dest.name}."
            }
            relativeAngle in 25.0..75.0 -> {
                "Turn slight right" to "Turn slightly right toward 1 o'clock. ${distanceMeters.roundToInt()} meters remaining."
            }
            relativeAngle in 75.0..115.0 -> {
                "Turn right" to "Turn right toward 3 o'clock. ${distanceMeters.roundToInt()} meters remaining."
            }
            relativeAngle in 115.0..170.0 -> {
                "Turn sharp right" to "Turn sharp right. Destination is ${distanceMeters.roundToInt()} meters behind your right."
            }
            relativeAngle in -75.0..-25.0 -> {
                "Turn slight left" to "Turn slightly left toward 11 o'clock. ${distanceMeters.roundToInt()} meters remaining."
            }
            relativeAngle in -115.0..-75.0 -> {
                "Turn left" to "Turn left toward 9 o'clock. ${distanceMeters.roundToInt()} meters remaining."
            }
            else -> {
                "Turn around" to "Destination is behind you. Turn around. ${distanceMeters.roundToInt()} meters away."
            }
        }

        _navigationGuide.value = NavigationGuide(
            destination = dest,
            distanceMeters = distanceMeters,
            bearingDegrees = bearingToDest,
            relativeDirection = directionText,
            spokenInstruction = spoken
        )
    }

    suspend fun searchAddress(query: String): List<NavigationTarget> = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val list = geocoder.getFromLocationName(query, 5) ?: emptyList()
            list.mapIndexed { index, addr ->
                NavigationTarget(
                    id = "search_$index",
                    name = addr.featureName ?: query,
                    category = "Search Result",
                    latitude = addr.latitude,
                    longitude = addr.longitude,
                    address = addr.toReadableString()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search geocode failed", e)
            emptyList()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            var azimuthDegrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
            if (azimuthDegrees < 0) azimuthDegrees += 360f
            _compassAzimuth.value = azimuthDegrees

            _currentLocation.value?.let { loc ->
                if (activeDestination != null) {
                    updateNavigationGuide(loc, azimuthDegrees)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
