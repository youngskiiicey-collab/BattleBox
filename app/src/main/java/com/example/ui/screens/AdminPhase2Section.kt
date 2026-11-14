package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.legal.WhatsAppTemplates
import com.example.data.model.GymAnnouncement
import com.example.data.model.GymPlan
import com.example.data.model.MemberSubscription
import com.example.data.model.NotificationDeliveryLog
import com.example.data.model.PaymentRequest
import com.example.data.model.Phase3BillingConfig
import com.example.data.model.UserProfile
import com.example.ui.components.AdminPhase3IntegrationCard
import com.example.ui.components.EditMemberSubscriptionDialog
import com.example.ui.components.EditPaymentRequestDialog
import com.example.ui.components.EditPlanDialog
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminPhase2Section(
    announcements: List<GymAnnouncement>,
    paymentRequests: List<PaymentRequest>,
    deliveryLogs: List<NotificationDeliveryLog>,
    plans: List<GymPlan> = GymPlan.defaultSeedPlans(),
    subscriptions: List<MemberSubscription> = emptyList(),
    allUsers: List<UserProfile> = emptyList(),
    billingConfig: Phase3BillingConfig = Phase3BillingConfig(),
    isBusy: Boolean,
    onPostAnnouncement: (String, String, String, String, Boolean, Boolean) -> Unit,
    onClearAnnouncement: (String) -> Unit,
    onSeedDefaultPlans: () -> Unit,
    onSavePlan: (GymPlan) -> Unit = {},
    onSaveSubscription: (MemberSubscription) -> Unit = {},
    onUpdatePaymentRequest: (PaymentRequest) -> Unit = {},
    onDeletePaymentRequest: (String, String) -> Unit = { _, _ -> },
    onReconcileSandboxPayment: (PaymentRequest) -> Unit,
    onSaveBillingConfig: (Phase3BillingConfig) -> Unit = {},
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

    var editingPlan by remember { mutableStateOf<GymPlan?>(null) }
    var showCreatePlanDialog by remember { mutableStateOf(false) }
    var editingRequest by remember { mutableStateOf<PaymentRequest?>(null) }
    var editingSubscription by remember { mutableStateOf<Pair<MemberSubscription, String>?>(null) }
    var showAssignSubPicker by remember { mutableStateOf(false) }

    val usersById = remember(allUsers) { allUsers.associateBy { it.userId } }
    val dateFmt = remember {
        SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. Post Live In-App Banner & Push/WhatsApp Broadcast (above Editable Membership Plans)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_post_banner_card")
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

                com.example.ui.components.CyberpunkButton(
                    text = "BROADCAST BANNER TO ALL",
                    onClick = {
                        onPostAnnouncement(
                            statusType,
                            title.ifBlank { "${statusType.replace("_", " ").uppercase()} Notice" },
                            message.ifBlank { "Please note updated Battle Box : FFF floor timings." },
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
                        .testTag("admin_post_announcement_button")
                )

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

        // 2. Editable Membership Plans Catalog (Basic/Premium, INR + GST)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_editable_plans_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Editable Membership Plans (${plans.size})", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Edit plan names, INR pricing, GST %, duration days, billing cycles, or toggle visibility for members.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showCreatePlanDialog = true },
                        enabled = !isBusy,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_add_plan_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Add Plan", modifier = Modifier.padding(start = 6.dp))
                    }
                    OutlinedButton(
                        onClick = onSeedDefaultPlans,
                        enabled = !isBusy,
                        modifier = Modifier.testTag("admin_seed_plans_button")
                    ) {
                        Text("Reset 6 Defaults")
                    }
                }

                plans.forEach { plan ->
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
                                Text(
                                    text = "${plan.name} • ₹${plan.totalAmountInr}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${plan.tier.uppercase()} • ${plan.billingCycle} • ${plan.durationDays}d • GST ${plan.gstPercent}% • ${if (plan.active) "ACTIVE" else "HIDDEN"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = { editingPlan = plan },
                                enabled = !isBusy,
                                modifier = Modifier.testTag("edit_plan_${plan.id}")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit plan", modifier = Modifier.size(16.dp))
                                Text("Edit", modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                    }
                }
            }
        }

        // 3. Editable Member Subscriptions & Active Memberships
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_editable_memberships_card")
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Editable Member Memberships (${subscriptions.size})", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Assign, extend validity (+30d), pause/cancel AutoPay, or override member subscription tiers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (allUsers.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showAssignSubPicker = true },
                        enabled = !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_assign_membership_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Assign / Edit Membership for Any User", modifier = Modifier.padding(start = 6.dp))
                    }
                }

                if (subscriptions.isEmpty()) {
                    Text(
                        text = "No member subscriptions recorded yet. Assign one above or settle a pending pay below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    subscriptions.forEach { sub ->
                        val memberLabel = usersById[sub.userId]?.displayName ?: sub.userId.take(10)
                        val validUntil = sub.currentPeriodEnd?.toDate()?.let { dateFmt.format(it) } ?: "No expiry date"
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
                                    Text(
                                        text = "$memberLabel • ${sub.planName}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "₹${sub.amountInr} • ${sub.status.uppercase()} • AutoPay=${if (sub.autoRenew) "ON" else "OFF"} • Until $validUntil",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedButton(
                                    onClick = { editingSubscription = sub to memberLabel },
                                    enabled = !isBusy,
                                    modifier = Modifier.testTag("edit_sub_${sub.id}")
                                ) {
                                    Text("Edit")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Editable Pending Pay Queue + Sandbox Settlement
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_pending_pay_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Editable Pending Pay Queue (${paymentRequests.size})", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Edit INR amount, UPI app, payment mode, or remove requests before reconciling or triggering webhooks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (paymentRequests.isEmpty()) {
                    Text(
                        text = "No pending member checkout intents.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    paymentRequests.take(10).forEach { req ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column {
                                    Text("${req.memberName} • ${req.planName}", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "₹${req.amountInr} via ${req.paymentMode} (${req.preferredUpiApp}) • ${req.status.uppercase()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { editingRequest = req },
                                        enabled = !isBusy,
                                        modifier = Modifier.testTag("edit_pending_pay_${req.id}")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Edit Pay", modifier = Modifier.padding(start = 4.dp))
                                    }
                                    Button(
                                        onClick = { onReconcileSandboxPayment(req) },
                                        enabled = !isBusy,
                                        modifier = Modifier.testTag("settle_pending_pay_${req.id}")
                                    ) {
                                        Text("Sandbox Settle")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Phase 3 Gateway & AutoPay Integration Card
        AdminPhase3IntegrationCard(
            billingConfig = billingConfig,
            isBusy = isBusy,
            onSaveBillingConfig = onSaveBillingConfig
        )

        // 6. Meta WhatsApp Cloud API Utility Templates & Delivery Logs
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

    if (showCreatePlanDialog) {
        EditPlanDialog(
            initialPlan = null,
            onDismiss = { showCreatePlanDialog = false },
            onSave = { created ->
                showCreatePlanDialog = false
                onSavePlan(created)
            }
        )
    }

    editingPlan?.let { targetPlan ->
        EditPlanDialog(
            initialPlan = targetPlan,
            onDismiss = { editingPlan = null },
            onSave = { updated ->
                editingPlan = null
                onSavePlan(updated)
            }
        )
    }

    editingRequest?.let { req ->
        EditPaymentRequestDialog(
            request = req,
            onDismiss = { editingRequest = null },
            onSave = { updated ->
                editingRequest = null
                onUpdatePaymentRequest(updated)
            },
            onDelete = { removed ->
                editingRequest = null
                onDeletePaymentRequest(removed.id, removed.memberName)
            }
        )
    }

    editingSubscription?.let { (sub, memberLabel) ->
        EditMemberSubscriptionDialog(
            subscription = sub,
            memberLabel = memberLabel,
            onDismiss = { editingSubscription = null },
            onSave = { updated ->
                editingSubscription = null
                onSaveSubscription(updated)
            }
        )
    }

    if (showAssignSubPicker) {
        Dialog(
            onDismissRequest = { showAssignSubPicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .widthIn(max = 520.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Select User for Membership Edit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    allUsers.take(25).forEach { user ->
                        val existingSub = subscriptions.firstOrNull { it.userId == user.userId }
                        OutlinedButton(
                            onClick = {
                                showAssignSubPicker = false
                                val draft = existingSub ?: MemberSubscription(
                                    id = "sub_${user.userId.take(10)}_custom",
                                    userId = user.userId,
                                    planId = plans.firstOrNull { it.tier == user.planTier }?.id ?: "basic_monthly",
                                    planName = plans.firstOrNull { it.tier == user.planTier }?.name ?: "Basic Monthly",
                                    tier = user.planTier,
                                    billingCycle = GymPlan.CYCLE_MONTHLY,
                                    paymentMode = PaymentRequest.MODE_UPI_AUTOPAY,
                                    amountInr = plans.firstOrNull { it.tier == user.planTier }?.totalAmountInr ?: 1499L,
                                    status = MemberSubscription.STATUS_ACTIVE,
                                    upiHandle = "Admin Managed",
                                    autoRenew = false,
                                    currentPeriodEnd = Timestamp(Date(System.currentTimeMillis() + 30L * 86_400_000L))
                                )
                                editingSubscription = draft to user.displayName
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${user.displayName} (${user.planTier.uppercase()} • ${user.membershipStatus.uppercase()})")
                        }
                    }
                    TextButton(
                        onClick = { showAssignSubPicker = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
