package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Phase3BillingConfig

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminPhase3IntegrationCard(
    billingConfig: Phase3BillingConfig,
    isBusy: Boolean,
    onSaveBillingConfig: (Phase3BillingConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var provider by remember(billingConfig.gatewayProvider) { mutableStateOf(billingConfig.gatewayProvider) }
    var envMode by remember(billingConfig.environmentMode) { mutableStateOf(billingConfig.environmentMode) }
    var keyId by remember(billingConfig.razorpayKeyId) { mutableStateOf(billingConfig.razorpayKeyId) }
    var supportedApps by remember(billingConfig.supportedUpiApps) { mutableStateOf(billingConfig.supportedUpiApps) }
    var preDebitStr by remember(billingConfig.preDebitLeadHours) { mutableStateOf(billingConfig.preDebitLeadHours.toString()) }
    var graceStr by remember(billingConfig.gracePeriodDays) { mutableStateOf(billingConfig.gracePeriodDays.toString()) }
    var webhookUrl by remember(billingConfig.webhookEndpointUrl) { mutableStateOf(billingConfig.webhookEndpointUrl) }
    var autopayEnabled by remember(billingConfig.upiAutoPayMandateEnabled) { mutableStateOf(billingConfig.upiAutoPayMandateEnabled) }
    var autoRetry by remember(billingConfig.autoRetryFailedDebits) { mutableStateOf(billingConfig.autoRetryFailedDebits) }
    var futureFinance by remember(billingConfig.futureFinanceModulesEnabled) { mutableStateOf(billingConfig.futureFinanceModulesEnabled) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_phase3_integration_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Hub, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Column {
                    Text(
                        text = "Phase 3 • Future Payment & AutoPay Integration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Configure future live UPI AutoPay gateway, RBI pre-debit window & Cloud Functions webhook endpoint (asia-south1).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text("Gateway Provider", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Phase3BillingConfig.GATEWAY_RAZORPAY to "Razorpay",
                    Phase3BillingConfig.GATEWAY_CASHFREE to "Cashfree PG",
                    Phase3BillingConfig.GATEWAY_PHONEPE to "PhonePe PG"
                ).forEach { (id, label) ->
                    FilterChip(
                        selected = provider == id,
                        onClick = { provider = id },
                        label = { Text(label) }
                    )
                }
            }

            Text("Environment Mode", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = envMode == Phase3BillingConfig.ENV_SANDBOX,
                    onClick = { envMode = Phase3BillingConfig.ENV_SANDBOX },
                    label = { Text("SANDBOX TESTING") }
                )
                FilterChip(
                    selected = envMode == Phase3BillingConfig.ENV_PRODUCTION_READY,
                    onClick = { envMode = Phase3BillingConfig.ENV_PRODUCTION_READY },
                    label = { Text("PRODUCTION READY") }
                )
            }

            OutlinedTextField(
                value = keyId,
                onValueChange = { keyId = it },
                label = { Text("Public Gateway Key ID (rzp_test_... / rzp_live_...)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("phase3_key_id_input")
            )

            OutlinedTextField(
                value = webhookUrl,
                onValueChange = { webhookUrl = it },
                label = { Text("Cloud Functions Webhook Endpoint (asia-south1)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = supportedApps,
                onValueChange = { supportedApps = it },
                label = { Text("Enabled UPI AutoPay Apps") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = preDebitStr,
                    onValueChange = { preDebitStr = it },
                    label = { Text("RBI Pre-Debit (hrs)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = graceStr,
                    onValueChange = { graceStr = it },
                    label = { Text("Grace Period (days)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Phase3SwitchRow("Enable Future UPI AutoPay Mandates", autopayEnabled) { autopayEnabled = it }
            Phase3SwitchRow("Auto-Retry Failed Recurring Debits", autoRetry) { autoRetry = it }
            Phase3SwitchRow("Enable Phase 3 Finance & Ledger Readiness", futureFinance) { futureFinance = it }

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Security Rule: Only the public Key ID is stored in Firestore. Razorpay Key Secret & HMAC Webhook Secret stay strictly in Google Cloud Secret Manager for asia-south1 Functions.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Button(
                onClick = {
                    onSaveBillingConfig(
                        billingConfig.copy(
                            gatewayProvider = provider,
                            environmentMode = envMode,
                            razorpayKeyId = keyId.trim(),
                            supportedUpiApps = supportedApps.ifBlank { "Google Pay, PhonePe, Paytm, BHIM" },
                            preDebitLeadHours = (preDebitStr.toLongOrNull() ?: 24L).coerceIn(1L, 168L),
                            gracePeriodDays = (graceStr.toLongOrNull() ?: 3L).coerceIn(0L, 30L),
                            webhookEndpointUrl = webhookUrl.trim(),
                            upiAutoPayMandateEnabled = autopayEnabled,
                            autoRetryFailedDebits = autoRetry,
                            futureFinanceModulesEnabled = futureFinance
                        )
                    )
                },
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("admin_save_phase3_billing_button")
            ) {
                Text("Save Phase 3 Gateway & AutoPay Config")
            }
        }
    }
}

@Composable
fun MemberPhase3AutopayBanner(
    billingConfig: Phase3BillingConfig,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .testTag("member_phase3_autopay_banner")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Autorenew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Phase 3 AutoPay • ${billingConfig.gatewayProvider.replace("_", " ").uppercase()} (${billingConfig.environmentMode.replace("_", " ").uppercase()})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "Mandates: ${if (billingConfig.upiAutoPayMandateEnabled) "Enabled" else "Paused"} • UPI: ${billingConfig.supportedUpiApps}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "RBI pre-debit alert ${billingConfig.preDebitLeadHours}h before renewal • ${billingConfig.gracePeriodDays}-day renewal grace • Auto-retry: ${if (billingConfig.autoRetryFailedDebits) "ON" else "OFF"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Phase3SwitchRow(
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
