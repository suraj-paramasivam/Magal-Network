package org.sosnetwork.app.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import org.sosnetwork.protocol.GeoLocation

object LocationHelper {
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): GeoLocation {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val location = client.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).await() ?: throw IllegalStateException("Location unavailable")

        return GeoLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracy,
            altitudeMeters = location.altitude,
        )
    }
}
