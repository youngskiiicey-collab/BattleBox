package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.GymPlan
import com.example.data.model.MemberSubscription
import com.example.data.model.PaymentRequest
import com.example.data.model.UserProfile
import com.google.firebase.Timestamp
import java.util.Date

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditPlanDialog(
    initialPlan: GymPlan?,
    onDismiss: () -> Unit,
    onSave: (GymPlan) -> Unit
) {
    val isNew = initialPlan == null || initialPlan.id.isBlank()
    var name by remember(initialPlan) { mutableStateOf(initialPlan?.name ?: "") }
    var tier by remember(initialPlan) { mutableStateOf(initialPlan?.tier ?: UserProfile.PLAN_BASIC) }
    var cycle by remember(initialPlan) { mutableStateOf(initialPlan?.billingCycle ?: GymPlan.CYCLE_MONTHLY) }
    var baseStr by remember(initialPlan) { mutableStateOf((initialPlan?.baseAmountInr ?: 1270L).toString()) }
    var gstStr by remember(initialPlan) { mutableStateOf((initialPlan?.gstPercent ?: 18L).toString()) }
    var totalStr by remember(initialPlan) { mutableStateOf((initialPlan?.totalAmountInr ?: 1499L).toString()) }
    var durationStr by remember(initialPlan) { mutableStateOf((initialPlan?.durationDays ?: 30L).toString()) }
    var description by remember(initialPlan) { mutableStateOf(initialPlan?.description ?: "") }
    var active by remember(initialPlan) { mutableStateOf(initialPlan?.active ?: true) }
    var supportsOneTime by remember(initialPlan) { mutableStateOf(initialPlan?.supportsOneTime ?: true) }
    var supportsAutopay by remember(initialPlan) { mutableStateOf(initialPlan?.supportsUpiAutopay ?: true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isNew) "Create Membership Plan" else "Edit ${initialPlan?.name}",
                    style = MaterialTheme.typography.headlineSmall
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Plan Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_plan_name_input")
                    )

                    Text("Tier", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(UserProfile.PLAN_BASIC, UserProfile.PLAN_PREMIUM).forEach { t ->
                            FilterChip(
                                selected = tier == t,
                                onClick = { tier = t },
                                label = { Text(t.uppercase()) }
                            )
                        }
                    }

                    Text("Billing Cycle", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(GymPlan.CYCLE_MONTHLY, GymPlan.CYCLE_QUARTERLY, GymPlan.CYCLE_ANNUAL).forEach { c ->
                            FilterChip(
                                selected = cycle == c,
                                onClick = { cycle = c },
                                label = { Text(c.uppercase()) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = baseStr,
                            onValueChange = { v ->
                                baseStr = v
                                val b = v.toLongOrNull()
                                val g = gstStr.toLongOrNull() ?: 18L
                                if (b != null) totalStr = (b + (b * g / 100L)).toString()
                            },
                            label = { Text("Base ₹") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = gstStr,
                            onValueChange = { v ->
                                gstStr = v
                                val b = baseStr.toLongOrNull()
                                val g = v.toLongOrNull() ?: 18L
                                if (b != null) totalStr = (b + (b * g / 100L)).toString()
                            },
                            label = { Text("GST %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = totalStr,
                            onValueChange = { totalStr = it },
                            label = { Text("Total ₹ (INR)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edit_plan_total_input")
                        )
                        OutlinedTextField(
                            value = durationStr,
                            onValueChange = { durationStr = it },
                            label = { Text("Days") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Plan Benefits / Description") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    DialogSwitchRow("Plan Visible & Active", active) { active = it }
                    DialogSwitchRow("Allow One-Time Payment", supportsOneTime) { supportsOneTime = it }
                    DialogSwitchRow("Allow UPI AutoPay", supportsAutopay) { supportsAutopay = it }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = {
                            val baseAmt = (baseStr.toLongOrNull() ?: 0L).coerceAtLeast(0L)
                            val gstPct = (gstStr.toLongOrNull() ?: 18L).coerceIn(0L, 50L)
                            val totalAmt = (totalStr.toLongOrNull() ?: (baseAmt + baseAmt * gstPct / 100L)).coerceAtLeast(0L)
                            onSave(
                                GymPlan(
                                    id = initialPlan?.id.orEmpty(),
                                    name = name.ifBlank { "${tier.uppercase()} ${cycle.uppercase()}" },
                                    tier = tier,
                                    billingCycle = cycle,
                                    baseAmountInr = baseAmt,
                                    gstPercent = gstPct,
                                    totalAmountInr = totalAmt,
                                    durationDays = (durationStr.toLongOrNull() ?: 30L).coerceIn(1L, 1095L),
                                    supportsOneTime = supportsOneTime,
                                    supportsUpiAutopay = supportsAutopay,
                                    description = description.trim(),
                                    active = active
                                )
                            )
                        },
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .testTag("save_plan_dialog_button")
                    ) {
                        Text("Save Plan")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditPaymentRequestDialog(
    request: PaymentRequest,
    onDismiss: () -> Unit,
    onSave: (PaymentRequest) -> Unit,
    onDelete: (PaymentRequest) -> Unit
) {
    var planName by remember(request.id) { mutableStateOf(request.planName) }
    var amountStr by remember(request.id) { mutableStateOf(request.amountInr.toString()) }
    var paymentMode by remember(request.id) { mutableStateOf(request.paymentMode) }
    var upiApp by remember(request.id) { mutableStateOf(request.preferredUpiApp) }
    var status by remember(request.id) { mutableStateOf(request.status) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Edit Pending Pay • ${request.memberName}",
                    style = MaterialTheme.typography.headlineSmall
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = planName,
                        onValueChange = { planName = it },
                        label = { Text("Plan Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Amount (INR ₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_pending_pay_amount_input")
                    )

                    Text("Payment Mode", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = paymentMode == PaymentRequest.MODE_ONE_TIME,
                            onClick = { paymentMode = PaymentRequest.MODE_ONE_TIME },
                            label = { Text("One-Time Order") }
                        )
                        FilterChip(
                            selected = paymentMode == PaymentRequest.MODE_UPI_AUTOPAY,
                            onClick = { paymentMode = PaymentRequest.MODE_UPI_AUTOPAY },
                            label = { Text("UPI AutoPay") }
                        )
                    }

                    Text("Queue Status", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = status == PaymentRequest.STATUS_CREATED,
                            onClick = { status = PaymentRequest.STATUS_CREATED },
                            label = { Text("CREATED") }
                        )
                        FilterChip(
                            selected = status == PaymentRequest.STATUS_AWAITING_BACKEND,
                            onClick = { status = PaymentRequest.STATUS_AWAITING_BACKEND },
                            label = { Text("AWAITING BACKEND") }
                        )
                    }

                    OutlinedTextField(
                        value = upiApp,
                        onValueChange = { upiApp = it },
                        label = { Text("UPI App / Collection Method") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = { onDelete(request) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_pending_pay_button")
                    ) {
                        Text("Remove Pending Request")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = {
                            val amount = (amountStr.toLongOrNull() ?: request.amountInr).coerceIn(1L, 500_000L)
                            onSave(
                                request.copy(
                                    planName = planName.ifBlank { request.planName },
                                    amountInr = amount,
                                    paymentMode = paymentMode,
                                    preferredUpiApp = upiApp.ifBlank { request.preferredUpiApp },
                                    status = status
                                )
                            )
                        },
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .testTag("save_pending_pay_button")
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditMemberSubscriptionDialog(
    subscription: MemberSubscription,
    memberLabel: String,
    onDismiss: () -> Unit,
    onSave: (MemberSubscription) -> Unit
) {
    var planName by remember(subscription.id) { mutableStateOf(subscription.planName) }
    var tier by remember(subscription.id) { mutableStateOf(subscription.tier) }
    var cycle by remember(subscription.id) { mutableStateOf(subscription.billingCycle) }
    var amountStr by remember(subscription.id) { mutableStateOf(subscription.amountInr.toString()) }
    var upiHandle by remember(subscription.id) { mutableStateOf(subscription.upiHandle) }
    var status by remember(subscription.id) { mutableStateOf(subscription.status) }
    var autoRenew by remember(subscription.id) { mutableStateOf(subscription.autoRenew) }
    var periodEnd by remember(subscription.id) { mutableStateOf(subscription.currentPeriodEnd) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Edit Membership • $memberLabel",
                    style = MaterialTheme.typography.headlineSmall
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = planName,
                        onValueChange = { planName = it },
                        label = { Text("Membership Plan Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            label = { Text("Amount ₹ (INR)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = upiHandle,
                            onValueChange = { upiHandle = it },
                            label = { Text("UPI / Mandate Note") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("Tier", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(UserProfile.PLAN_BASIC, UserProfile.PLAN_PREMIUM).forEach { t ->
                            FilterChip(selected = tier == t, onClick = { tier = t }, label = { Text(t.uppercase()) })
                        }
                    }

                    Text("Billing Cycle", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(GymPlan.CYCLE_MONTHLY, GymPlan.CYCLE_QUARTERLY, GymPlan.CYCLE_ANNUAL).forEach { c ->
                            FilterChip(selected = cycle == c, onClick = { cycle = c }, label = { Text(c.uppercase()) })
                        }
                    }

                    Text("Subscription Status", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            MemberSubscription.STATUS_ACTIVE,
                            MemberSubscription.STATUS_PENDING_AUTH,
                            MemberSubscription.STATUS_CANCEL_REQUESTED,
                            MemberSubscription.STATUS_CANCELLED,
                            MemberSubscription.STATUS_EXPIRED
                        ).forEach { s ->
                            FilterChip(
                                selected = status == s,
                                onClick = { status = s },
                                label = { Text(s.replace("_", " ").uppercase()) }
                            )
                        }
                    }

                    DialogSwitchRow("Auto-Renew / UPI AutoPay", autoRenew) { autoRenew = it }

                    OutlinedButton(
                        onClick = {
                            val baseTime = maxOf(
                                System.currentTimeMillis(),
                                periodEnd?.toDate()?.time ?: System.currentTimeMillis()
                            )
                            periodEnd = Timestamp(Date(baseTime + 30L * 86_400_000L))
                            status = MemberSubscription.STATUS_ACTIVE
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Extend Validity +30 Days & Activate")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = {
                            onSave(
                                subscription.copy(
                                    planName = planName.ifBlank { subscription.planName },
                                    tier = tier,
                                    billingCycle = cycle,
                                    amountInr = (amountStr.toLongOrNull() ?: subscription.amountInr).coerceAtLeast(0L),
                                    upiHandle = upiHandle,
                                    status = status,
                                    autoRenew = autoRenew,
                                    currentPeriodEnd = periodEnd
                                )
                            )
                        },
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .testTag("save_subscription_dialog_button")
                    ) {
                        Text("Save Membership")
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogSwitchRow(
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
