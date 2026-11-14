package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.AttendanceRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object MonthlyAttendancePdfGenerator {

    private val istZone: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    fun suggestedPdfFileName(year: Int, monthOneBased: Int): String {
        val mm = monthOneBased.coerceIn(1, 12).toString().padStart(2, '0')
        return "battlebox_fff_attendance_${year}_$mm.pdf"
    }

    data class PdfRowCells(
        val name: String,
        val mode: String,
        val status: String,
        val checkInStr: String,
        val durStr: String,
        val geo: String
    )

    fun paginateRecords(
        records: List<AttendanceRecord>,
        rowsPerFirstPage: Int = 23,
        rowsPerNextPage: Int = 31
    ): List<List<AttendanceRecord>> {
        if (records.isEmpty()) return listOf(emptyList())
        val chunks = mutableListOf<List<AttendanceRecord>>()
        chunks.add(records.take(rowsPerFirstPage))
        var idx = rowsPerFirstPage
        while (idx < records.size) {
            chunks.add(records.subList(idx, (idx + rowsPerNextPage).coerceAtMost(records.size)))
            idx += rowsPerNextPage
        }
        return chunks
    }

    fun formatSummaryLine1(summary: MonthlyAttendanceSummary): String =
        "Sessions: ${summary.totalRecords}   |   Unique Members: ${summary.uniqueUsers}   |   Trainer Scans: ${summary.trainerScanCount}   |   Wall/Kiosk QR: ${summary.kioskQrCount}"

    fun formatSummaryLine2(summary: MonthlyAttendanceSummary): String =
        "Active Now: ${summary.activeCount}   |   Checked Out: ${summary.checkedOutCount}   |   Geofence Auto-Out: ${summary.autoCheckedOutCount}"

    fun formatRowCells(rec: AttendanceRecord): PdfRowCells {
        val istFmt = SimpleDateFormat("dd MMM HH:mm", Locale.ENGLISH).apply { timeZone = istZone }
        val checkInMs = rec.checkInAt?.toDate()?.time
        val checkOutMs = rec.checkOutAt?.toDate()?.time
        val checkInStr = checkInMs?.let { istFmt.format(Date(it)) } ?: "--"
        val durStr = if (checkInMs != null && checkOutMs != null && checkOutMs >= checkInMs) {
            "${TimeUnit.MILLISECONDS.toMinutes(checkOutMs - checkInMs)}m"
        } else if (rec.status == AttendanceRecord.STATUS_CHECKED_IN) {
            "Active"
        } else {
            "--"
        }
        return PdfRowCells(
            name = rec.memberName.ifBlank { rec.userId }.take(21),
            mode = rec.mode.replace("_", " ").take(11),
            status = rec.status.replace("_", " ").take(14),
            checkInStr = checkInStr,
            durStr = durStr,
            geo = "${rec.distanceMeters.toInt()}m / ±${rec.gpsAccuracyMeters.toInt()}m"
        )
    }

    fun generatePdfBytes(summary: MonthlyAttendanceSummary): ByteArray {
        val pdf = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        val headerBg = Paint().apply { color = Color.rgb(11, 16, 22) }
        val cyanLine = Paint().apply { color = Color.rgb(0, 200, 220) }
        val greenAccent = Paint().apply { color = Color.rgb(68, 184, 82) }
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint().apply {
            color = Color.rgb(242, 182, 67)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val metaPaint = Paint().apply {
            color = Color.LTGRAY
            textSize = 9.5f
        }
        val sectionPaint = Paint().apply {
            color = Color.rgb(18, 92, 34)
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerRowBg = Paint().apply { color = Color.rgb(228, 236, 240) }
        val rowAltBg = Paint().apply { color = Color.rgb(246, 249, 250) }
        val colHeaderPaint = Paint().apply {
            color = Color.rgb(17, 20, 23)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val cellPaint = Paint().apply {
            color = Color.rgb(28, 34, 42)
            textSize = 9f
        }
        val footerPaint = Paint().apply {
            color = Color.rgb(95, 108, 120)
            textSize = 8.5f
        }
        val dividerPaint = Paint().apply {
            color = Color.rgb(210, 218, 224)
            strokeWidth = 0.8f
        }

        val chunks = paginateRecords(summary.records)

        val totalPages = chunks.size
        chunks.forEachIndexed { pageIndex, pageRecords ->
            val pageNumber = pageIndex + 1
            val page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            val canvas = page.canvas

            var y: Float
            if (pageNumber == 1) {
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 96f, headerBg)
                canvas.drawRect(0f, 96f, pageWidth.toFloat(), 100f, greenAccent)
                canvas.drawRect(0f, 100f, pageWidth.toFloat(), 102f, cyanLine)

                canvas.drawText("BATTLE BOX : FFF", 32f, 38f, titlePaint)
                canvas.drawText("FIGHT FOR FITNESS • MONTHLY ATTENDANCE REPORT", 32f, 56f, subtitlePaint)
                canvas.drawText("Period: ${summary.monthLabel} (IST)  •  Dudhnoi, Assam", 32f, 76f, metaPaint)
                canvas.drawText("Region: asia-south1", 445f, 38f, metaPaint)
                canvas.drawText("Page $pageNumber of $totalPages", 445f, 56f, metaPaint)

                y = 126f
                canvas.drawText("MONTHLY SUMMARY", 32f, y, sectionPaint)
                y += 18f
                canvas.drawText(formatSummaryLine1(summary), 32f, y, cellPaint)
                y += 16f
                canvas.drawText(formatSummaryLine2(summary), 32f, y, cellPaint)
                y += 16f
            } else {
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 48f, headerBg)
                canvas.drawRect(0f, 48f, pageWidth.toFloat(), 51f, cyanLine)
                canvas.drawText("BATTLE BOX : FFF • ${summary.monthLabel} Attendance (cont.)", 32f, 30f, metaPaint)
                canvas.drawText("Page $pageNumber of $totalPages", 485f, 30f, metaPaint)
                y = 74f
            }

            // Table header
            canvas.drawRect(28f, y, 567f, y + 20f, headerRowBg)
            canvas.drawText("Member", 34f, y + 13.5f, colHeaderPaint)
            canvas.drawText("Mode", 170f, y + 13.5f, colHeaderPaint)
            canvas.drawText("Status", 242f, y + 13.5f, colHeaderPaint)
            canvas.drawText("Check-In (IST)", 328f, y + 13.5f, colHeaderPaint)
            canvas.drawText("Dur", 436f, y + 13.5f, colHeaderPaint)
            canvas.drawText("Dist / GPS", 485f, y + 13.5f, colHeaderPaint)
            y += 22f

            if (pageRecords.isEmpty()) {
                canvas.drawText("No attendance logs recorded for ${summary.monthLabel}.", 34f, y + 18f, cellPaint)
            } else {
                pageRecords.forEachIndexed { rowIdx, rec ->
                    if (rowIdx % 2 == 1) {
                        canvas.drawRect(28f, y - 2f, 567f, y + 18f, rowAltBg)
                    }
                    val row = formatRowCells(rec)
                    canvas.drawText(row.name, 34f, y + 12f, cellPaint)
                    canvas.drawText(row.mode, 170f, y + 12f, cellPaint)
                    canvas.drawText(row.status, 242f, y + 12f, cellPaint)
                    canvas.drawText(row.checkInStr, 328f, y + 12f, cellPaint)
                    canvas.drawText(row.durStr, 436f, y + 12f, cellPaint)
                    canvas.drawText(row.geo, 485f, y + 12f, cellPaint)

                    y += 20f
                }
            }

            // Footer
            canvas.drawLine(28f, 804f, 567f, 804f, dividerPaint)
            canvas.drawText("Battle Box : FFF • Dudhnoi, Assam • App created by DocCoder.exe", 32f, 820f, footerPaint)
            canvas.drawText("Page $pageNumber/$totalPages", 520f, 820f, footerPaint)

            pdf.finishPage(page)
        }

        val out = java.io.ByteArrayOutputStream()
        pdf.writeTo(out)
        pdf.close()
        return out.toByteArray()
    }

    fun writePdfToUri(context: Context, uri: Uri, summary: MonthlyAttendanceSummary): Result<Unit> = runCatching {
        val bytes = generatePdfBytes(summary)
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(bytes)
            stream.flush()
        } ?: throw IllegalStateException("Unable to open PDF destination stream.")
        Unit
    }

    fun shareOrOpenPdf(
        context: Context,
        summary: MonthlyAttendanceSummary,
        viewOnly: Boolean = false
    ): Result<File> = runCatching {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = suggestedPdfFileName(summary.year, summary.monthOneBased)
        val file = File(dir, fileName)
        FileOutputStream(file).use { it.write(generatePdfBytes(summary)) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = if (viewOnly) {
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Battle Box : FFF Attendance PDF — ${summary.monthLabel}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        val chooserTitle = if (viewOnly) "Open Monthly Attendance PDF" else "Share Monthly Attendance PDF"
        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        file
    }
}
