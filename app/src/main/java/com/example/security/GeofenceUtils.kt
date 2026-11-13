package com.example.security

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.example.data.model.GymConfig
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Calendar
import java.util.TimeZone
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class DeviceFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double
)

object GeofenceUtils {
    private const val EARTH_RADIUS_M = 6_371_000.0

    fun haversineMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_M * c
    }

    fun evaluateOpenStatusIst(
        config: GymConfig,
        nowMillis: Long = System.currentTimeMillis()
    ): Pair<Boolean, String> {
        return when (config.manualStatusOverride) {
            "open" -> true to "OPEN (MANUAL)"
            "closed" -> false to "CLOSED (MANUAL)"
            else -> {
                val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).apply {
                    timeInMillis = nowMillis
                }
                val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                val morningStart = parseMinutes(config.morningOpen, 5 * 60 + 30)
                val morningEnd = parseMinutes(config.morningClose, 10 * 60)
                val eveningStart = parseMinutes(config.eveningOpen, 16 * 60 + 30)
                val eveningEnd = parseMinutes(config.eveningClose, 21 * 60 + 30)

                val inMorning = nowMinutes in morningStart until morningEnd
                val inEvening = nowMinutes in eveningStart until eveningEnd
                if (inMorning || inEvening) {
                    true to "OPEN • IST"
                } else {
                    false to "CLOSED • IST"
                }
            }
        }
    }

    private fun parseMinutes(hhmm: String, fallback: Int): Int {
        val parts = hhmm.trim().split(":")
        if (parts.size != 2) return fallback
        val h = parts[0].toIntOrNull() ?: return fallback
        val m = parts[1].toIntOrNull() ?: return fallback
        return (h * 60 + m).coerceIn(0, 24 * 60)
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(context: Context): DeviceFix? =
        suspendCancellableCoroutine { cont ->
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            cont.invokeOnCancellation { cts.cancel() }

            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        cont.resume(
                            DeviceFix(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracyMeters = location.accuracy.toDouble()
                            )
                        )
                    } else {
                        client.lastLocation
                            .addOnSuccessListener { last ->
                                if (last != null) {
                                    cont.resume(
                                        DeviceFix(
                                            latitude = last.latitude,
                                            longitude = last.longitude,
                                            accuracyMeters = last.accuracy.toDouble()
                                        )
                                    )
                                } else {
                                    cont.resume(null)
                                }
                            }
                            .addOnFailureListener { cont.resume(null) }
                    }
                }
                .addOnFailureListener {
                    cont.resume(null)
                }
        }
}
