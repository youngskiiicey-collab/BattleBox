package com.example.data.ai

import com.example.data.model.GymConfig
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

class BattleBoxConciergeRepository {
    private var activeChat: Chat? = null
    private var lastConfigKey: String = ""

    private fun buildModel(gymConfig: GymConfig) =
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    You are the official Battle Box Gym Concierge for ${gymConfig.gymName}.
                    Speak with disciplined, motivating, helpful precision.
                    Known Battle Box Facility Details (always share accurately):
                    - Google Maps Location Link: ${gymConfig.mapsUrl}
                    - Coordinates: ${gymConfig.latitude}, ${gymConfig.longitude}
                    - Check-In Geofence Radius: ${gymConfig.geofenceRadiusM.toInt()} meters (max GPS accuracy: ${gymConfig.maxGpsAccuracyM.toInt()} meters)
                    - Timezone: ${gymConfig.timezone} | Currency: ${gymConfig.currency}
                    - Operating Hours (IST): Morning ${gymConfig.morningOpen}–${gymConfig.morningClose}, Evening ${gymConfig.eveningOpen}–${gymConfig.eveningClose}
                    - Auto Check-Out Window: ${gymConfig.autoCheckoutHours} hours
                    - Data Residency: ${gymConfig.dataRegion} under India DPDP Act 2023
                    - Attendance Rules: Member & Kiosk QR codes rotate every 30 seconds so screenshots do not work. Both Front Desk scan and Self Kiosk scan verify active membership, no duplicate check-in, and geofence proximity.
                    - Safety Guardrail: Never diagnose medical conditions or prescribe injury treatment; advise consulting a qualified physician or physiotherapist for pain or injury.
                    - Phase Boundary: Payments/Razorpay and full personalized AI Workout/Diet coaching are scheduled for upcoming Phase 2 & Phase 3 releases; in Phase 1 assist with onboarding, timings, directions, QR check-in, and DPDP policy clarity.
                    """.trimIndent()
                )
            }
        )

    fun sendMessageStream(userMessage: String, gymConfig: GymConfig): Flow<String> = flow {
        val configKey = "${gymConfig.gymName}|${gymConfig.morningOpen}|${gymConfig.eveningClose}|${gymConfig.geofenceRadiusM}"
        if (activeChat == null || lastConfigKey != configKey) {
            val model = buildModel(gymConfig)
            activeChat = model.startChat()
            lastConfigKey = configKey
        }
        val chat = checkNotNull(activeChat)
        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            chat.sendMessageStream(userMessage).collect { chunk ->
                val part = chunk.text.orEmpty()
                if (part.isNotEmpty()) {
                    accumulated.append(part)
                    emit(accumulated.toString())
                }
            }
        }
    }

    fun resetChat() {
        activeChat = null
    }
}
