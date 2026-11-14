package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.GymConfig
import com.example.security.QrTokenSigner
import java.io.File
import java.io.FileOutputStream

object WallQrPosterPdfGenerator {

    fun generateAndOpenPoster(context: Context, gymConfig: GymConfig): Result<File> = runCatching {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "BattleBox_Wall_QR_Poster.pdf")
        val permanentPayload = QrTokenSigner.generatePermanentWallToken("main")
        val qrBitmap = QrTokenSigner.renderQrBitmap(permanentPayload, 880)

        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 portrait 72dpi
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        val darkPaint = Paint().apply { color = Color.rgb(17, 20, 23) }
        val greenPaint = Paint().apply { color = Color.rgb(46, 139, 87) }
        val borderPaint = Paint().apply {
            color = Color.rgb(46, 139, 87)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val goldPaint = Paint().apply {
            color = Color.rgb(229, 169, 60)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val headingPaint = Paint().apply {
            color = Color.rgb(17, 20, 23)
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(45, 52, 60)
            textSize = 12f
            textAlign = Paint.Align.CENTER
        }
        val badgePaint = Paint().apply {
            color = Color.rgb(27, 110, 40)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val footerPaint = Paint().apply {
            color = Color.rgb(100, 112, 125)
            textSize = 10f
            textAlign = Paint.Align.LEFT
        }

        // Top dark header banner
        canvas.drawRect(0f, 0f, 595f, 128f, darkPaint)
        canvas.drawRect(0f, 128f, 595f, 136f, greenPaint)
        canvas.drawText(gymConfig.gymName.uppercase(), 297.5f, 60f, titlePaint)
        canvas.drawText("DUDHNOI, ASSAM • PERMANENT SELF CHECK-IN WALL QR", 297.5f, 90f, goldPaint)

        // Instruction title
        canvas.drawText("SCAN TO CHECK IN", 297.5f, 186f, headingPaint)
        canvas.drawText("NO FRONT DESK WAIT • PERMANENT PRINTED GYM POSTER", 297.5f, 210f, badgePaint)

        // Framed Permanent QR Bitmap
        val qrSize = 310f
        val left = (595f - qrSize) / 2f
        val top = 238f
        val destRect = Rect(left.toInt(), top.toInt(), (left + qrSize).toInt(), (top + qrSize).toInt())
        canvas.drawRect(left - 16f, top - 16f, left + qrSize + 16f, top + qrSize + 16f, borderPaint)
        canvas.drawBitmap(qrBitmap, null, destRect, null)

        // Instructions & Geofence rules
        var y = 602f
        canvas.drawText("1. Open Battle Box app -> Pass & Check-In -> Tap 'Scan Wall QR'.", 297.5f, y, bodyPaint)
        y += 24f
        canvas.drawText(
            "2. Stand inside ${gymConfig.geofenceRadiusM.toInt()}m gym geofence (GPS accuracy <= ${gymConfig.maxGpsAccuracyM.toInt()}m).",
            297.5f,
            y,
            bodyPaint
        )
        y += 24f
        canvas.drawText("3. Trainers & Admin receive live check-in alerts automatically.", 297.5f, y, bodyPaint)
        y += 28f
        canvas.drawText(
            "IST Timings: ${gymConfig.morningOpen}–${gymConfig.morningClose} & ${gymConfig.eveningOpen}–${gymConfig.eveningClose}",
            297.5f,
            y,
            badgePaint
        )
        y += 28f
        canvas.drawText("Token Type: Permanent Wall Pass ($permanentPayload)", 297.5f, y, footerPaint.apply { textAlign = Paint.Align.CENTER })

        // Bottom-left attribution
        footerPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("App created by DocCoder.exe", 36f, 812f, footerPaint)

        pdf.finishPage(page)
        FileOutputStream(file).use { out -> pdf.writeTo(out) }
        pdf.close()

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(viewIntent, "Print or Share Battle Box Wall QR Poster").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)

        file
    }
}
