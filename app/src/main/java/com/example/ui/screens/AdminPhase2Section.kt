package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.legal.WhatsAppTemplates
import com.example.data.model.GymAnnouncement
import com.example.data.model.NotificationDeliveryLog
import com.example.data.model.PaymentRequest

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminPhase2Section(
    announcements: List<GymAnnouncement>,
    paymentRequests: List<PaymentRequest>,
    deliveryLogs: List<NotificationDeliveryLog>,
    isBusy: Boolean,
    onPostAnnouncement: (String, String, String, String, Boolean, Boolean) -> Unit,
    onClearAnnouncement: (String) -> Unit,
    onSeedDefaultPlans: () -> Unit,
    onReconcileSandboxPayment: (PaymentRequest) -> Unit,
    onCopyMessage: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var statusType by remember { mutableStateOf(GymAnnouncement.TYPE_SPECIAL_HOURS) }
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var effectiveHours by remember { mutableStateOf("06:00–11:00 IST") }
    var sendPush by remember { mutableStateOf(true) }
    var sendWhatsappClosure by remember { mutableStateOf(false) }
    var showMetaTemplates by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. Post Live In-App Banner & Push/WhatsApp Broadcast
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "Post Open / Closed / Special Hours Banner", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Updates the live in-app banner for all members and trainers immediately and logs an audit entry.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        GymAnnouncement.TYPE_OPEN to "Open",
                        GymAnnouncement.TYPE_CLOSED to "Closed",
                        GymAnnouncement.TYPE_SPECIAL_HOURS to "Special Hours",
                        GymAnnouncement.TYPE_HOLIDAY to "Holiday"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = statusType == key,
                            onClick = { statusType = key },
                            label = { Text(label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Headline (e.g. Republic Day Hours)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_announcement_title")
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Announcement Message") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_announcement_message")
                )
                OutlinedTextField(
                    value = effectiveHours,
                    onValueChange = { effectiveHours = it },
                    label = { Text("Effective Hours / Dates (IST)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Trigger FCM / Device Push Alert", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = sendPush, onCheckedChange = { sendPush = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Queue WhatsApp Closure Template", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = sendWhatsappClosure, onCheckedChange = { sendWhatsappClosure = it })
                }

                Button(
                    onClick = {
                        onPostAnnouncement(
                            statusType,
                            title.ifBlank { "${statusType.replace("_", " ").uppercase()} Notice" },
                            message.ifBlank { "Please note updated Battle Box floor timings." },
                            effectiveHours,
                            sendPush,
                            sendWhatsappClosure
                        )
                        title = ""
                        message = ""
                    },
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_post_announcement_button")
                ) {
                    Text("Broadcast Banner to Members & Trainers")
                }

                if (announcements.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Text("Active Banners (${announcements.size})", style = MaterialTheme.typography.titleSmall)
                    announcements.forEach { ann ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("[${ann.statusType.uppercase()}] ${ann.title}", style = MaterialTheme.typography.titleSmall)
                                Text(ann.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { onClearAnnouncement(ann.id) }) { Text("Clear") }
                        }
                    }
                }
            }
        }

        // 2. Razorpay Orders, Plans Seeding & Sandbox Webhook Reconciliation
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Plans Catalog & Razorpay Checkout Queue", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Production payment status changes only via HMAC-verified Cloud Functions webhooks. Admins may reconcile pending requests here in sandbox testing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = onSeedDefaultPlans,
                    enabled = !isBusy,
                    modifier = Modifier.testTag("admin_seed_plans_button")
                ) {
                    Text("Seed 6 INR Plans (Basic/Premium)")
                }

                if (paymentRequests.isEmpty()) {
                    Text(
                        text = "No pending member checkout intents.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    paymentRequests.take(5).forEach { req ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${req.memberName} • ${req.planName}", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "₹${req.amountInr} via ${req.paymentMode} (${req.preferredUpiApp})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedButton(
                                    onClick = { onReconcileSandboxPayment(req) },
                                    enabled = !isBusy
                                ) {
                                    Text("Sandbox Settle")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Meta WhatsApp Cloud API Utility Templates & Delivery Logs
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Meta WhatsApp Templates & Delivery Log", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { showMetaTemplates = !showMetaTemplates }) {
                        Text(if (showMetaTemplates) "Hide Templates" else "View 5 Templates")
                    }
                }

                if (showMetaTemplates) {
                    WhatsAppTemplates.templates.forEach { tpl ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${tpl.name} (${tpl.category})",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    TextButton(onClick = {
                                        clipboard.setText(AnnotatedString(tpl.exactBodyText))
                                        onCopyMessage("Copied Meta template ${tpl.name}")
                                    }) {
                                        Text("Copy")
                                    }
                                }
                                Text(tpl.exactBodyText, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = "Variables: ${tpl.variableMapping}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Text("Recent Delivery Status Logs (${deliveryLogs.size})", style = MaterialTheme.typography.titleSmall)
                if (deliveryLogs.isEmpty()) {
                    Text(
                        text = "No notification dispatches logged yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    deliveryLogs.take(6).forEach { log ->
                        Text(
                            text = "• [${log.channel.uppercase()}] ${log.templateName} -> ${log.status.uppercase()} (${log.details})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
