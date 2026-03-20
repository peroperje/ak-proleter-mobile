package com.akproleter.mobile.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class LocationResult(
    val lat: Float?,
    val lon: Float?,
    val locationText: String?
)

@Singleton
class AppLocationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val geocoder = Geocoder(context, Locale.getDefault())

    companion object {
        private const val TAG = "AppLocationManager"
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): LocationResult? = withContext(Dispatchers.IO) {
        try {
            val location = suspendCancellableCoroutine<Location?> { continuation ->
                val cancellationTokenSource = CancellationTokenSource()
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { loc ->
                    if (continuation.isActive) {
                        continuation.resume(loc)
                    }
                }.addOnFailureListener { e ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }
                
                continuation.invokeOnCancellation {
                    cancellationTokenSource.cancel()
                }
            }

            if (location == null) {
                Log.w(TAG, "Location is null (GPS might be off or unavailable)")
                return@withContext null
            }

            val lat = location.latitude.toFloat()
            val lon = location.longitude.toFloat()

            var locationText: String? = null
            try {
                // Geocoder requires internet connection. This will fail gracefully if offline.
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    locationText = buildString {
                        if (address.thoroughfare != null) append(address.thoroughfare)
                        if (address.subThoroughfare != null) append(" ${address.subThoroughfare}")
                        if (address.locality != null) {
                            if (isNotEmpty()) append(", ")
                            append(address.locality)
                        }
                    }.takeIf { it.isNotBlank() } ?: address.getAddressLine(0)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Geocoder failed to get text location (offline?): ${e.message}")
            }

            return@withContext LocationResult(lat, lon, locationText)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get location coordinates", e)
            return@withContext null
        }
    }
}
