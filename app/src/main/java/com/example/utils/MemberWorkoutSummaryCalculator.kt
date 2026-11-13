package com.example.utils

import com.example.data.model.AttendanceRecord
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class MemberWorkoutSummary(
    val totalSessions: Int = 0,
    val thisMonthSessions: Int = 0,
    val last7DaysSessions: Int = 0,
    val currentStreakDays: Int = 0,
    val completedSessions: Int = 0,
    val totalWorkoutMinutes: Long = 0L,
    val avgSessionMinutes: Long = 0L,
    val preferredWindow: String = "Not enough data",
    val kioskQrCount: Int = 0,
    val trainerScanCount: Int = 0,
    val frontDeskCount: Int = 0,
    val lastCheckInAtMs: Long? = null
) {
    val formattedTotalDuration: String
        get() {
            val hours = totalWorkoutMinutes / 60L
            val mins = totalWorkoutMinutes % 60L
            return when {
                totalWorkoutMinutes <= 0L -> "0m"
                hours > 0L -> "${hours}h ${mins}m"
                else -> "${mins}m"
            }
        }
}

object MemberWorkoutSummaryCalculator {

    private val istZone: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    fun summarize(
        records: List<AttendanceRecord>,
        referenceTimeMs: Long = System.currentTimeMillis()
    ): MemberWorkoutSummary {
        if (records.isEmpty()) return MemberWorkoutSummary()

        val refCal = Calendar.getInstance(istZone).apply { timeInMillis = referenceTimeMs }
        val curYear = refCal.get(Calendar.YEAR)
        val curMonth = refCal.get(Calendar.MONTH)
        val sevenDaysAgoMs = referenceTimeMs - TimeUnit.DAYS.toMillis(7)

        var thisMonthCount = 0
        var last7DaysCount = 0
        var completedCount = 0
        var totalMins = 0L
        var morningCount = 0
        var eveningCount = 0
        var lastCheckInMs: Long? = null
        val uniqueIstDayKeys = mutableSetOf<Long>()

        for (rec in records) {
            val checkInMs = rec.checkInAt?.toDate()?.time
            if (checkInMs != null) {
                if (lastCheckInMs == null || checkInMs > lastCheckInMs) {
                    lastCheckInMs = checkInMs
                }
                val cal = Calendar.getInstance(istZone).apply { timeInMillis = checkInMs }
                if (cal.get(Calendar.YEAR) == curYear && cal.get(Calendar.MONTH) == curMonth) {
                    thisMonthCount++
                }
                if (checkInMs >= sevenDaysAgoMs) {
                    last7DaysCount++
                }
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                if (hour < 13) morningCount++ else eveningCount++

                uniqueIstDayKeys += istDayNumber(cal)
            }

            val checkOutMs = rec.checkOutAt?.toDate()?.time
            if (checkInMs != null && checkOutMs != null && checkOutMs >= checkInMs) {
                completedCount++
                totalMins += TimeUnit.MILLISECONDS.toMinutes(checkOutMs - checkInMs)
            }
        }

        val todayKey = istDayNumber(refCal)
        var streak = 0
        var probeKey = when {
            uniqueIstDayKeys.contains(todayKey) -> todayKey
            uniqueIstDayKeys.contains(todayKey - 1L) -> todayKey - 1L
            else -> Long.MIN_VALUE
        }
        while (probeKey != Long.MIN_VALUE && uniqueIstDayKeys.contains(probeKey)) {
            streak++
            probeKey--
        }

        val avgMins = if (completedCount > 0) totalMins / completedCount else 0L
        val preferred = when {
            morningCount == 0 && eveningCount == 0 -> "Not enough data"
            morningCount > eveningCount -> String.format(Locale.ENGLISH, "Morning Batch (%d)", morningCount)
            eveningCount > morningCount -> String.format(Locale.ENGLISH, "Evening Batch (%d)", eveningCount)
            else -> "Balanced AM / PM"
        }

        return MemberWorkoutSummary(
            totalSessions = records.size,
            thisMonthSessions = thisMonthCount,
            last7DaysSessions = last7DaysCount,
            currentStreakDays = streak,
            completedSessions = completedCount,
            totalWorkoutMinutes = totalMins,
            avgSessionMinutes = avgMins,
            preferredWindow = preferred,
            kioskQrCount = records.count { it.mode == AttendanceRecord.MODE_KIOSK_QR },
            trainerScanCount = records.count { it.mode == AttendanceRecord.MODE_TRAINER_SCAN },
            frontDeskCount = records.count { it.mode == AttendanceRecord.MODE_FRONT_DESK },
            lastCheckInAtMs = lastCheckInMs
        )
    }

    private fun istDayNumber(cal: Calendar): Long {
        val normalized = (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return TimeUnit.MILLISECONDS.toDays(normalized.timeInMillis + istZone.rawOffset)
    }
}
