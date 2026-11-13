package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.PaymentRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object InvoicePdfGenerator {

    fun generateAndOpenInvoice(context: Context, record: PaymentRecord): Result<File> = runCatching {
        val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val safeInvoiceNo = record.invoiceNumber.ifBlank { record.id }.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(dir, "BattleBox_Invoice_$safeInvoiceNo.pdf")

        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 72dpi
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        val headerBg = Paint().apply { color = Color.rgb(17, 20, 23) }
        val greenAccent = Paint().apply { color = Color.rgb(68, 184, 82) }
        val goldPaint = Paint().apply {
            color = Color.rgb(242, 182, 67)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subHeaderPaint = Paint().apply {
            color = Color.LTGRAY
            textSize = 11f
        }
        val sectionPaint = Paint().apply {
            color = Color.rgb(27, 110, 40)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(30, 35, 42)
            textSize = 11.5f
        }
        val boldBodyPaint = Paint().apply {
            color = Color.rgb(17, 20, 23)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val mutedPaint = Paint().apply {
            color = Color.rgb(100, 112, 125)
            textSize = 10f
        }
        val linePaint = Paint().apply {
            color = Color.rgb(210, 218, 224)
            strokeWidth = 1f
        }

        // Top Dark Brand Header
        canvas.drawRect(0f, 0f, 595f, 118f, headerBg)
        canvas.drawRect(0f, 118f, 595f, 124f, greenAccent)
        canvas.drawText("BATTLE BOX", 36f, 52f, titlePaint)
        canvas.drawText("STRENGTH & COMBAT CONDITIONING", 36f, 72f, goldPaint)
        canvas.drawText("Tax Invoice / Membership Receipt (INR)", 36f, 96f, subHeaderPaint)
        canvas.drawText("Data Region: asia-south1 (Mumbai)", 370f, 52f, subHeaderPaint)
        canvas.drawText("Currency: INR (GST Inclusive)", 370f, 72f, subHeaderPaint)

        val fmt = SimpleDateFormat("dd MMM yyyy, hh:mm a z", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
        val issuedDate = fmt.format(record.createdAt?.toDate() ?: Date())

        var y = 160f
        canvas.drawText("INVOICE DETAILS", 36f, y, sectionPaint)
        y += 22f
        canvas.drawText("Invoice No:", 36f, y, boldBodyPaint)
        canvas.drawText(record.invoiceNumber.ifBlank { record.id }, 165f, y, bodyPaint)
        canvas.drawText("Issued Date:", 330f, y, boldBodyPaint)
        canvas.drawText(issuedDate, 415f, y, bodyPaint)

        y += 20f
        canvas.drawText("Status:", 36f, y, boldBodyPaint)
        canvas.drawText(record.status.uppercase(Locale.ENGLISH), 165f, y, bodyPaint)
        canvas.drawText("Payment Mode:", 330f, y, boldBodyPaint)
        canvas.drawText(record.paymentMode.replace("_", " ").uppercase(Locale.ENGLISH), 430f, y, bodyPaint)

        y += 24f
        canvas.drawLine(36f, y, 559f, y, linePaint)
        y += 26f

        canvas.drawText("BILLED TO (MEMBER)", 36f, y, sectionPaint)
        y += 22f
        canvas.drawText("Member Name:", 36f, y, boldBodyPaint)
        canvas.drawText(record.memberName.ifBlank { "Battle Box Member" }, 165f, y, bodyPaint)
        y += 18f
        canvas.drawText("Email / Account:", 36f, y, boldBodyPaint)
        canvas.drawText(record.memberEmail.ifBlank { record.userId }, 165f, y, bodyPaint)
        y += 18f
        canvas.drawText("Plan Tier:", 36f, y, boldBodyPaint)
        canvas.drawText("${record.planName} (${record.tier.uppercase(Locale.ENGLISH)})", 165f, y, bodyPaint)

        y += 28f
        canvas.drawLine(36f, y, 559f, y, linePaint)
        y += 28f

        canvas.drawText("LINE ITEM & GST BREAKDOWN (INR)", 36f, y, sectionPaint)
        y += 24f
        canvas.drawText("Description", 36f, y, boldBodyPaint)
        canvas.drawText("Amount (INR)", 475f, y, boldBodyPaint)
        y += 12f
        canvas.drawLine(36f, y, 559f, y, linePaint)

        val cgst = record.gstAmountInr / 2L
        val sgst = record.gstAmountInr - cgst

        y += 24f
        canvas.drawText("Battle Box Membership — ${record.planName}", 36f, y, bodyPaint)
        canvas.drawText("₹${record.baseAmountInr}.00", 485f, y, bodyPaint)

        y += 20f
        canvas.drawText("CGST (9%)", 36f, y, bodyPaint)
        canvas.drawText("₹$cgst.00", 485f, y, bodyPaint)

        y += 20f
        canvas.drawText("SGST (9%)", 36f, y, bodyPaint)
        canvas.drawText("₹$sgst.00", 485f, y, bodyPaint)

        y += 16f
        canvas.drawLine(36f, y, 559f, y, linePaint)
        y += 24f
        canvas.drawText("TOTAL PAID", 36f, y, boldBodyPaint)
        canvas.drawText("₹${record.totalAmountInr}.00 INR", 475f, y, boldBodyPaint)

        y += 42f
        canvas.drawText("RAZORPAY & WEBHOOK VERIFICATION AUDIT", 36f, y, sectionPaint)
        y += 20f
        canvas.drawText("Order / Sub Ref: ${record.razorpayOrderId.ifBlank { "N/A" }}", 36f, y, bodyPaint)
        y += 18f
        canvas.drawText("Payment ID: ${record.razorpayPaymentId.ifBlank { "N/A" }}", 36f, y, bodyPaint)
        y += 18f
        canvas.drawText("Webhook Event ID: ${record.webhookEventId.ifBlank { "N/A" }}", 36f, y, bodyPaint)

        y += 48f
        canvas.drawLine(36f, y, 559f, y, linePaint)
        y += 20f
        canvas.drawText("Computer-generated GST invoice • Payment status set strictly via verified webhook.", 36f, y, mutedPaint)
        y += 16f
        canvas.drawText("DPDP Act 2023 Compliant Storage: asia-south1 (Mumbai) • Battle Box", 36f, y, mutedPaint)

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
        val chooser = Intent.createChooser(viewIntent, "Open or Share Battle Box PDF Invoice").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)

        file
    }
}
