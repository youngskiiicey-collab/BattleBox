package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.AttendanceRecord
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class MonthlyAttendanceSummary(
    val year: Int,
    val monthOneBased: Int,
    val monthLabel: String,
    val fileName: String,
    val totalRecords: Int,
    val uniqueUsers: Int,
    val activeCount: Int,
    val checkedOutCount: Int,
    val autoCheckedOutCount: Int,
    val kioskQrCount: Int,
    val frontDeskCount: Int,
    val trainerScanCount: Int,
    val csvContent: String,
    val records: List<AttendanceRecord>
)

object AttendanceCsvExporter {

    private val istZone: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    private const val CSV_HEADER =
        "attendance_id,member_name,user_id,member_role,check_in_mode,status,check_in_ist,check_out_ist,duration_minutes,distance_meters,gps_accuracy_meters,latitude,longitude,verified_by_uid,qr_epoch_slot"

    fun currentYearMonthIst(referenceTimeMs: Long = System.currentTimeMillis()): Pair<Int, Int> {
        val cal = Calendar.getInstance(istZone).apply { timeInMillis = referenceTimeMs }
        return cal.get(Calendar.YEAR) to (cal.get(Calendar.MONTH) + 1)
    }

    fun shiftYearMonth(year: Int, monthOneBased: Int, deltaMonths: Int): Pair<Int, Int> {
        val cal = Calendar.getInstance(istZone).apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, (monthOneBased - 1).coerceIn(0, 11))
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, deltaMonths)
        }
        return cal.get(Calendar.YEAR) to (cal.get(Calendar.MONTH) + 1)
    }

    fun recentMonthOptions(
        count: Int = 6,
        referenceTimeMs: Long = System.currentTimeMillis()
    ): List<Pair<Int, Int>> {
        val (curYear, curMonth) = currentYearMonthIst(referenceTimeMs)
        return (0 until count).map { offset -> shiftYearMonth(curYear, curMonth, -offset) }
    }

    fun monthDateRangeIst(year: Int, monthOneBased: Int): Pair<Date, Date> {
        val normalizedMonth = monthOneBased.coerceIn(1, 12) - 1
        val startCal = Calendar.getInstance(istZone).apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, normalizedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = (startCal.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }
        return startCal.time to endCal.time
    }

    fun formatMonthLabel(year: Int, monthOneBased: Int): String {
        val cal = Calendar.getInstance(istZone).apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthOneBased.coerceIn(1, 12) - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val fmt = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).apply { timeZone = istZone }
        return fmt.format(cal.time)
    }

    fun formatShortMonthLabel(year: Int, monthOneBased: Int): String {
        val cal = Calendar.getInstance(istZone).apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthOneBased.coerceIn(1, 12) - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val fmt = SimpleDateFormat("MMM yyyy", Locale.ENGLISH).apply { timeZone = istZone }
        return fmt.format(cal.time)
    }

    fun suggestedFileName(year: Int, monthOneBased: Int): String {
        val mm = monthOneBased.coerceIn(1, 12).toString().padStart(2, '0')
        return "battlebox_attendance_${year}_$mm.csv"
    }

    fun buildSummaryAndCsv(
        year: Int,
        monthOneBased: Int,
        records: List<AttendanceRecord>
    ): MonthlyAttendanceSummary {
        val sorted = records.sortedBy { it.checkInAt?.toDate()?.time ?: 0L }
        return MonthlyAttendanceSummary(
            year = year,
            monthOneBased = monthOneBased,
            monthLabel = formatMonthLabel(year, monthOneBased),
            fileName = suggestedFileName(year, monthOneBased),
            totalRecords = sorted.size,
            uniqueUsers = sorted.map { it.userId }.filter { it.isNotBlank() }.distinct().size,
            activeCount = sorted.count { it.status == AttendanceRecord.STATUS_CHECKED_IN },
            checkedOutCount = sorted.count { it.status == AttendanceRecord.STATUS_CHECKED_OUT },
            autoCheckedOutCount = sorted.count { it.status == AttendanceRecord.STATUS_AUTO_CHECKED_OUT },
            kioskQrCount = sorted.count { it.mode == AttendanceRecord.MODE_KIOSK_QR },
            frontDeskCount = sorted.count { it.mode == AttendanceRecord.MODE_FRONT_DESK },
            trainerScanCount = sorted.count { it.mode == AttendanceRecord.MODE_TRAINER_SCAN },
            csvContent = toCsv(sorted),
            records = sorted
        )
    }

    fun toCsv(records: List<AttendanceRecord>): String {
        val istFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'IST'", Locale.ENGLISH).apply {
            timeZone = istZone
        }
        val sb = StringBuilder()
        sb.appendLine(CSV_HEADER)
        for (rec in records) {
            val checkInMs = rec.checkInAt?.toDate()?.time
            val checkOutMs = rec.checkOutAt?.toDate()?.time
            val checkInStr = checkInMs?.let { istFmt.format(Date(it)) }.orEmpty()
            val checkOutStr = checkOutMs?.let { istFmt.format(Date(it)) }.orEmpty()
            val durationMin = if (checkInMs != null && checkOutMs != null && checkOutMs >= checkInMs) {
                TimeUnit.MILLISECONDS.toMinutes(checkOutMs - checkInMs).toString()
            } else if (rec.status == AttendanceRecord.STATUS_CHECKED_IN) {
                "active"
            } else {
                ""
            }

            val row = listOf(
                escapeCsvCell(rec.id),
                escapeCsvCell(rec.memberName),
                escapeCsvCell(rec.userId),
                escapeCsvCell(rec.memberRole),
                escapeCsvCell(rec.mode),
                escapeCsvCell(rec.status),
                escapeCsvCell(checkInStr),
                escapeCsvCell(checkOutStr),
                escapeCsvCell(durationMin),
                String.format(Locale.US, "%.2f", rec.distanceMeters),
                String.format(Locale.US, "%.2f", rec.gpsAccuracyMeters),
                String.format(Locale.US, "%.6f", rec.latitude),
                String.format(Locale.US, "%.6f", rec.longitude),
                escapeCsvCell(rec.verifiedByUid),
                rec.qrEpochSlot.toString()
            )
            sb.appendLine(row.joinToString(","))
        }
        return sb.toString()
    }

    fun escapeCsvCell(raw: String): String {
        val formulaSafe = if (
            raw.isNotEmpty() &&
            (raw[0] == '=' || raw[0] == '+' || raw[0] == '-' || raw[0] == '@' || raw[0] == '\t' || raw[0] == '\r') &&
            raw.toDoubleOrNull() == null
        ) {
            "'$raw"
        } else {
            raw
        }
        val needsQuotes = formulaSafe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuotes) {
            "\"${formulaSafe.replace("\"", "\"\"")}\""
        } else {
            formulaSafe
        }
    }

    fun writeCsvToUri(context: Context, uri: Uri, csvContent: String): Result<Unit> = runCatching {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(csvContent.toByteArray(Charsets.UTF_8))
            stream.flush()
        } ?: throw IllegalStateException("Unable to open destination file stream.")
        Unit
    }

    fun shareCsvViaIntent(context: Context, fileName: String, csvContent: String): Result<Unit> = runCatching {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(csvContent, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Battle Box Attendance Export: $fileName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Export Monthly Attendance CSV").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
