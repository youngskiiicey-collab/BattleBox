package com.example.security

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object QrTokenSigner {
    const val ROTATION_INTERVAL_SECONDS = 30L
    private const val PREFIX = "BB1"
    const val KIND_MEMBER = "MEMBER"
    const val KIND_KIOSK = "KIOSK"
    const val KIND_WALL_STATIC = "WALL"

    // Phase 1 Note: When Cloud Functions in asia-south1 are deployed, move signing & verification
    // to Cloud Secret Manager so the HMAC key is never shipped in client binaries.
    private const val PHASE1_SIGNING_SECRET = "battlebox-phase1-qr-hmac-key-rotate-in-prod"

    data class ParsedQrToken(
        val kind: String,
        val subjectId: String,
        val epochSlot: Long,
        val signatureHex: String
    )

    sealed interface VerificationResult {
        data class Valid(val token: ParsedQrToken) : VerificationResult
        data class Invalid(val reason: String) : VerificationResult
    }

    fun currentEpochSlot(nowMillis: Long = System.currentTimeMillis()): Long =
        nowMillis / (ROTATION_INTERVAL_SECONDS * 1000L)

    fun secondsRemainingInSlot(nowMillis: Long = System.currentTimeMillis()): Int {
        val elapsedMs = nowMillis % (ROTATION_INTERVAL_SECONDS * 1000L)
        val remaining = ROTATION_INTERVAL_SECONDS - (elapsedMs / 1000L)
        return remaining.coerceIn(1L, ROTATION_INTERVAL_SECONDS).toInt()
    }

    fun generateMemberToken(uid: String, nowMillis: Long = System.currentTimeMillis()): String {
        val slot = currentEpochSlot(nowMillis)
        val sig = computeSignature(KIND_MEMBER, uid, slot)
        return "$PREFIX|$KIND_MEMBER|$uid|$slot|$sig"
    }

    fun generateKioskToken(gymId: String = "main", nowMillis: Long = System.currentTimeMillis()): String {
        val slot = currentEpochSlot(nowMillis)
        val sig = computeSignature(KIND_KIOSK, gymId, slot)
        return "$PREFIX|$KIND_KIOSK|$gymId|$slot|$sig"
    }

    /**
     * Generates a permanent (non-rotating) signed Battle Box Wall QR token
     * so Admins can print and paste it inside the gym for geofenced member/trainer self check-in.
     */
    fun generatePermanentWallToken(gymId: String = "main"): String {
        val slot = 0L
        val sig = computeSignature(KIND_WALL_STATIC, gymId, slot)
        return "$PREFIX|$KIND_WALL_STATIC|$gymId|$slot|$sig"
    }

    fun verifyToken(
        rawPayload: String,
        expectedKind: String,
        nowMillis: Long = System.currentTimeMillis()
    ): VerificationResult {
        val parts = rawPayload.trim().split("|")
        if (parts.size != 5 || parts[0] != PREFIX) {
            return VerificationResult.Invalid("Invalid Battle Box QR format.")
        }
        val kind = parts[1]
        val subjectId = parts[2]
        val slot = parts[3].toLongOrNull()
            ?: return VerificationResult.Invalid("Invalid QR timestamp slot.")
        val sigHex = parts[4]

        val isAcceptableWallStatic = expectedKind == KIND_KIOSK && kind == KIND_WALL_STATIC
        if (kind != expectedKind && !isAcceptableWallStatic) {
            return VerificationResult.Invalid(
                "Wrong QR type: expected $expectedKind pass, scanned $kind."
            )
        }
        if (subjectId.isBlank()) {
            return VerificationResult.Invalid("Missing QR subject identifier.")
        }

        val expectedSig = computeSignature(kind, subjectId, slot)
        val sigMatches = MessageDigest.isEqual(
            sigHex.lowercase().toByteArray(Charsets.UTF_8),
            expectedSig.lowercase().toByteArray(Charsets.UTF_8)
        )
        if (!sigMatches) {
            return VerificationResult.Invalid("QR signature verification failed (tampered token).")
        }

        if (kind == KIND_WALL_STATIC) {
            if (slot != 0L) {
                return VerificationResult.Invalid("Invalid permanent wall QR slot.")
            }
            return VerificationResult.Valid(
                ParsedQrToken(
                    kind = kind,
                    subjectId = subjectId,
                    epochSlot = slot,
                    signatureHex = sigHex
                )
            )
        }

        val currentSlot = currentEpochSlot(nowMillis)
        val secondsIntoCurrentSlot = (nowMillis % (ROTATION_INTERVAL_SECONDS * 1000L)) / 1000L
        val isFresh = slot == currentSlot || (slot == currentSlot - 1 && secondsIntoCurrentSlot <= 5L)
        if (!isFresh) {
            return VerificationResult.Invalid(
                "Expired QR token (screenshots or stale codes older than 30s are rejected)."
            )
        }

        return VerificationResult.Valid(
            ParsedQrToken(
                kind = kind,
                subjectId = subjectId,
                epochSlot = slot,
                signatureHex = sigHex
            )
        )
    }

    fun renderQrBitmap(payload: String, sizePx: Int = 640): Bitmap {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1
        )
        val bitMatrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun computeSignature(kind: String, subjectId: String, slot: Long): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(PHASE1_SIGNING_SECRET.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val digest = mac.doFinal("$PREFIX|$kind|$subjectId|$slot".toByteArray(Charsets.UTF_8))
        return digest.take(12).joinToString("") { "%02x".format(it) }
    }
}
