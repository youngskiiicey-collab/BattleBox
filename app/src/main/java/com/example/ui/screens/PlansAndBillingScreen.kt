package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.GymPlan
import com.example.data.model.MemberSubscription
import com.example.data.model.NotificationPreference
import com.example.data.model.PaymentRecord
import com.example.data.model.PaymentRequest
import com.example.data.model.UserProfile
import com.example.utils.BattleBoxNotificationHelper
import com.example.utils.InvoicePdfGenerator
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlansAndBillingScreen(
    userProfile: UserProfile,
    plans: List<GymPlan>,
    subscriptions: List<MemberSubscription>,
    paymentRequests: List<PaymentRequest>,
    payments: List<PaymentRecord>,
    notificationPref: NotificationPreference,
    isBusy: Boolean,
    onCreateCheckoutIntent: (GymPlan, String, String) -> Unit,
    onCancelAutopay: (String) -> Unit,
    onSaveNotificationPreference: (NotificationPreference) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCycle by remember { mutableStateOf(GymPlan.CYCLE_MONTHLY) }
    var checkoutPlan by remember { mutableStateOf<GymPlan?>(null) }
    var cancelTargetSub by remember { mutableStateOf<MemberSubscription?>(null) }

    // Notification preferences local edit state synced with Firestore
    var waOptIn by remember(notificationPref) { mutableStateOf(notificationPref.whatsappOptIn) }
    var waPhone by remember(notificationPref) {
        mutableStateOf(notificationPref.whatsappPhone.ifBlank { userProfile.phone.orEmpty() })
    }
    var expiry3d by remember(notificationPref) { mutableStateOf(notificationPref.expiryReminder3d) }
    var dueToday by remember(notificationPref) { mutableStateOf(notificationPref.dueTodayReminder) }
    var payAlerts by remember(notificationPref) { mutableStateOf(notificationPref.paymentAlerts) }
    var closedAlerts by remember(notificationPref) { mutableStateOf(notificationPref.gymClosedAlerts) }
    var preDebitAlerts by remember(notificationPref) { mutableStateOf(notificationPref.preDebitAlerts) }
    var pushEnabled by remember(notificationPref) { mutableStateOf(notificationPref.pushEnabled) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        pushEnabled = granted
        if (!granted) {
            onShowMessage("Notification permission denied. Enable it in Android Settings to receive alerts.")
        } else {
            BattleBoxNotificationHelper.ensureChannels(context)
            onShowMessage("Android Push Notifications enabled.")
        }
    }

    val fmt = remember {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
    }

    val activeSub = subscriptions.firstOrNull {
        it.status == MemberSubscription.STATUS_ACTIVE || it.status == MemberSubscription.STATUS_CANCEL_REQUESTED
    }

    LaunchedEffect(activeSub?.id, activeSub?.preDebitNotified) {
        if (activeSub != null && activeSub.autoRenew && activeSub.nextDebitAt != null) {
            val hoursUntilDebit = (activeSub.nextDebitAt.toDate().time - System.currentTimeMillis()) / 3_600_000L
            if (hoursUntilDebit in 0..24) {
                BattleBoxNotificationHelper.showBillingReminderNotification(
                    context = context,
                    idSeed = "predebit_${activeSub.id}",
                    title = "Battle Box UPI AutoPay Pre-Debit Notice",
                    body = "₹${activeSub.amountInr} for ${activeSub.planName} will be drafted within 24h via ${activeSub.upiHandle}."
                )
            }
        }
    }

    val filteredPlans = remember(plans, selectedCycle) {
        plans.filter { it.billingCycle == selectedCycle }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 760.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Current Plan Status & RBI 24h Pre-Debit Reminder
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Column {
                                Text(text = "MEMBERSHIP & AUTOPAY STATUS", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Current Tier: ${userProfile.planTier.uppercase()} • Access: ${userProfile.membershipStatus.uppercase()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (activeSub != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${activeSub.planName} • ₹${activeSub.amountInr} INR (${activeSub.status.replace("_", " ").uppercase()})",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Mandate: ${activeSub.upiHandle} (${activeSub.razorpaySubscriptionId})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                activeSub.nextDebitAt?.let { nextDebit ->
                                    Text(
                                        text = "Next Scheduled Debit: ${fmt.format(nextDebit.toDate())} IST",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                activeSub.currentPeriodEnd?.let { endTs ->
                                    Text(
                                        text = "Valid Until: ${fmt.format(endTs.toDate())} IST",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // 24-hour Pre-Debit Reminder Card
                                if (activeSub.autoRenew && activeSub.nextDebitAt != null) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Autorenew,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Pre-Debit Reminder (RBI UPI AutoPay): You will receive an advance notice 24h before ₹${activeSub.amountInr} is auto-debited.",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }

                                if (activeSub.autoRenew) {
                                    OutlinedButton(
                                        onClick = { cancelTargetSub = activeSub },
                                        enabled = !isBusy,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.testTag("cancel_autopay_button")
                                    ) {
                                        Text("Manage / Cancel UPI AutoPay")
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No active recurring UPI AutoPay mandate. Select a plan below to pay via Razorpay One-Time Order or set up Google Pay / UPI AutoPay.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. Plans Catalog (Monthly / Quarterly / Annual x Basic / Premium)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "BATTLE BOX MEMBERSHIP PLANS (INR)", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Premium unlocks AI Coach, personalized diet/workout plans & advanced analytics (Phase 3). Prices include 18% GST.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            GymPlan.CYCLE_MONTHLY to "Monthly",
                            GymPlan.CYCLE_QUARTERLY to "Quarterly",
                            GymPlan.CYCLE_ANNUAL to "Annual"
                        ).forEach { (cycle, label) ->
                            FilterChip(
                                selected = selectedCycle == cycle,
                                onClick = { selectedCycle = cycle },
                                label = { Text(label) },
                                modifier = Modifier.testTag("plan_cycle_$cycle")
                            )
                        }
                    }

                    filteredPlans.forEach { plan ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = plan.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${plan.tier.uppercase()} TIER • ${plan.durationDays} days",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "₹${plan.totalAmountInr}",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            text = "Base ₹${plan.baseAmountInr} + 18% GST",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = plan.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = { checkoutPlan = plan },
                                    enabled = !isBusy,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("select_plan_${plan.id}")
                                ) {
                                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "Choose One-Time or UPI AutoPay",
                                        modifier = Modifier.padding(start = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (paymentRequests.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Text(text = "Pending Checkout Intents (${paymentRequests.size})", style = MaterialTheme.typography.titleSmall)
                        paymentRequests.take(3).forEach { req ->
                            Text(
                                text = "• ${req.planName} — ₹${req.amountInr} via ${req.paymentMode} (${req.preferredUpiApp}) • Status: ${req.status.uppercase()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 3. Invoices & Payment History (Webhook-Settled + Native PDF Generator)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(text = "GST PDF INVOICES & PAYMENTS", style = MaterialTheme.typography.titleMedium)
                        }
                        TextButton(
                            onClick = {
                                val sample = PaymentRecord(
                                    id = "sample_preview",
                                    userId = userProfile.userId,
                                    memberName = userProfile.displayName,
                                    memberEmail = userProfile.email,
                                    planId = "premium_monthly",
                                    planName = "Premium Monthly (Sample Preview)",
                                    tier = UserProfile.PLAN_PREMIUM,
                                    baseAmountInr = 2542L,
                                    gstAmountInr = 457L,
                                    totalAmountInr = 2999L,
                                    paymentMode = PaymentRequest.MODE_UPI_AUTOPAY,
                                    razorpayOrderId = "order_sample_preview",
                                    razorpayPaymentId = "pay_sample_preview",
                                    webhookEventId = "evt_sample_preview",
                                    status = "sample_preview_only",
                                    invoiceNumber = "BB-SAMPLE-001"
                                )
                                InvoicePdfGenerator.generateAndOpenInvoice(context, sample)
                                    .onFailure { onShowMessage("No PDF viewer installed or export failed: ${it.message}") }
                            },
                            modifier = Modifier.testTag("preview_sample_pdf_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(" Sample PDF", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Text(
                        text = "Payment status is strictly webhook-controlled (server-side signature verification). Client apps cannot mark invoices paid.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (payments.isEmpty()) {
                        Text(
                            text = "No settled webhook payments recorded yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        payments.forEach { pay ->
                            val dateTxt = pay.createdAt?.toDate()?.let { fmt.format(it) } ?: "Settled"
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${pay.planName} • ₹${pay.totalAmountInr} INR",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = "${pay.invoiceNumber} • ${pay.status.uppercase()} • $dateTxt IST",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            InvoicePdfGenerator.generateAndOpenInvoice(context, pay)
                                                .onFailure { onShowMessage("Unable to open PDF: ${it.localizedMessage}") }
                                        }
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text(" PDF", modifier = Modifier.padding(start = 4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. WhatsApp Cloud API & Push Opt-In Capture
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(text = "WHATSAPP CLOUD & PUSH ALERTS", style = MaterialTheme.typography.titleMedium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Opt in to WhatsApp Utility Alerts", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Expiry in 3 days, due today, payment confirmation/failure & closure notices.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = waOptIn,
                            onCheckedChange = { waOptIn = it },
                            modifier = Modifier.testTag("whatsapp_optin_switch")
                        )
                    }

                    OutlinedTextField(
                        value = waPhone,
                        onValueChange = { waPhone = it },
                        label = { Text("WhatsApp Mobile (+91...)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("whatsapp_phone_input")
                    )

                    PrefSwitchRow("Plan Expiry (3 days prior)", expiry3d) { expiry3d = it }
                    PrefSwitchRow("Due Today & Pre-Debit Reminders", dueToday && preDebitAlerts) {
                        dueToday = it
                        preDebitAlerts = it
                    }
                    PrefSwitchRow("Payment Success / Failed Receipts", payAlerts) { payAlerts = it }
                    PrefSwitchRow("Gym Closed / Holiday Alerts", closedAlerts) { closedAlerts = it }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Device Push Notifications", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Note: Web PWA iOS push requires adding the app to the iOS Home Screen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = pushEnabled,
                            onCheckedChange = { enable ->
                                if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !BattleBoxNotificationHelper.hasNotificationPermission(context)
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    pushEnabled = enable
                                }
                            },
                            modifier = Modifier.testTag("push_optin_switch")
                        )
                    }

                    Button(
                        onClick = {
                            onSaveNotificationPreference(
                                NotificationPreference(
                                    userId = userProfile.userId,
                                    whatsappOptIn = waOptIn,
                                    whatsappPhone = waPhone,
                                    expiryReminder3d = expiry3d,
                                    dueTodayReminder = dueToday,
                                    paymentAlerts = payAlerts,
                                    gymClosedAlerts = closedAlerts,
                                    preDebitAlerts = preDebitAlerts,
                                    pushEnabled = pushEnabled
                                )
                            )
                        },
                        enabled = !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_notification_prefs_button")
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Save Notification Opt-In", modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    // Checkout Mode Dialog (One-Time Razorpay Order vs UPI AutoPay Subscription)
    checkoutPlan?.let { plan ->
        var paymentMode by remember { mutableStateOf(PaymentRequest.MODE_UPI_AUTOPAY) }
        var selectedUpiApp by remember { mutableStateOf("Google Pay") }

        AlertDialog(
            onDismissRequest = { checkoutPlan = null },
            title = { Text("Checkout: ${plan.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Total Payable: ₹${plan.totalAmountInr} INR (includes ₹${plan.totalAmountInr - plan.baseAmountInr} 18% GST)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(text = "Select Payment Rail:", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = paymentMode == PaymentRequest.MODE_UPI_AUTOPAY,
                            onClick = { paymentMode = PaymentRequest.MODE_UPI_AUTOPAY },
                            label = { Text("UPI AutoPay Subscription") }
                        )
                        FilterChip(
                            selected = paymentMode == PaymentRequest.MODE_ONE_TIME,
                            onClick = { paymentMode = PaymentRequest.MODE_ONE_TIME },
                            label = { Text("Razorpay One-Time Order") }
                        )
                    }
                    if (paymentMode == PaymentRequest.MODE_UPI_AUTOPAY) {
                        Text(text = "Preferred UPI App:", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Google Pay", "PhonePe", "Paytm", "BHIM UPI").forEach { app ->
                                FilterChip(
                                    selected = selectedUpiApp == app,
                                    onClick = { selectedUpiApp = app },
                                    label = { Text(app) }
                                )
                            }
                        }
                    }
                    Text(
                        text = "Security Note: Razorpay Key Secret & HMAC webhook signature checks run strictly in Cloud Functions (asia-south1). This creates a server checkout intent without stubbing client payment success.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateCheckoutIntent(
                            plan,
                            paymentMode,
                            if (paymentMode == PaymentRequest.MODE_UPI_AUTOPAY) selectedUpiApp else "Razorpay Checkout"
                        )
                        checkoutPlan = null
                    },
                    modifier = Modifier.testTag("confirm_checkout_intent_button")
                ) {
                    Text("Create Server Intent")
                }
            },
            dismissButton = {
                TextButton(onClick = { checkoutPlan = null }) { Text("Cancel") }
            }
        )
    }

    // Cancel AutoPay Confirmation Dialog
    cancelTargetSub?.let { sub ->
        AlertDialog(
            onDismissRequest = { cancelTargetSub = null },
            title = { Text("Cancel UPI AutoPay?") },
            text = {
                Text(
                    "Cancelling AutoPay for ${sub.planName} stops automatic UPI renewals at the end of your current period. Your pass remains valid until cycle expiry."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelAutopay(sub.id)
                        cancelTargetSub = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Cancellation")
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelTargetSub = null }) { Text("Keep AutoPay") }
            }
        )
    }
}

@Composable
private fun PrefSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
