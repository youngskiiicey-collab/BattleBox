package com.example.security

import com.example.data.model.GymConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class QrAndGeofenceUnitTest {

    @Test
    fun qrToken_validWithinSlot_passesAndRejectsWrongKindAndTampering() {
        val now = 1_700_000_010_000L
        val memberQr = QrTokenSigner.generateMemberToken("uid_123", now)

        val ok = QrTokenSigner.verifyToken(memberQr, QrTokenSigner.KIND_MEMBER, now + 5_000L)
        assertTrue(ok is QrTokenSigner.VerificationResult.Valid)

        val wrongKind = QrTokenSigner.verifyToken(memberQr, QrTokenSigner.KIND_KIOSK, now + 5_000L)
        assertTrue(wrongKind is QrTokenSigner.VerificationResult.Invalid)

        val tampered = memberQr.dropLast(2) + "ff"
        val tamperedRes = QrTokenSigner.verifyToken(tampered, QrTokenSigner.KIND_MEMBER, now)
        assertTrue(tamperedRes is QrTokenSigner.VerificationResult.Invalid)
    }

    @Test
    fun qrToken_staleScreenshotOlderThanSlot_isRejected() {
        val t0 = 1_700_000_010_000L
        val memberQr = QrTokenSigner.generateMemberToken("uid_123", t0)

        // 45 seconds later -> screenshot expired
        val expiredRes = QrTokenSigner.verifyToken(memberQr, QrTokenSigner.KIND_MEMBER, t0 + 45_000L)
        assertTrue(expiredRes is QrTokenSigner.VerificationResult.Invalid)
    }

    @Test
    fun haversineMeters_computesAccurateDistance() {
        val samePoint = GeofenceUtils.haversineMeters(28.4211, 77.0436, 28.4211, 77.0436)
        assertEquals(0.0, samePoint, 0.01)

        // ~111m per 0.001 deg latitude
        val hundredMeters = GeofenceUtils.haversineMeters(28.4211, 77.0436, 28.4220, 77.0436)
        assertTrue(hundredMeters in 95.0..105.0)
    }

    @Test
    fun evaluateOpenStatusIst_respectsMorningAndEveningWindows() {
        val config = GymConfig(
            morningOpen = "05:30",
            morningClose = "10:00",
            eveningOpen = "16:30",
            eveningClose = "21:30",
            manualStatusOverride = "auto"
        )
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        cal.set(2026, Calendar.NOVEMBER, 12, 7, 0, 0)
        assertTrue(GeofenceUtils.evaluateOpenStatusIst(config, cal.timeInMillis).first)

        cal.set(2026, Calendar.NOVEMBER, 12, 13, 0, 0)
        assertFalse(GeofenceUtils.evaluateOpenStatusIst(config, cal.timeInMillis).first)

        cal.set(2026, Calendar.NOVEMBER, 12, 18, 30, 0)
        assertTrue(GeofenceUtils.evaluateOpenStatusIst(config, cal.timeInMillis).first)
    }
}
