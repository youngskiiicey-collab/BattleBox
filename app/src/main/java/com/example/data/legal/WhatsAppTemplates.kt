package com.example.data.legal

data class MetaWhatsAppTemplate(
    val name: String,
    val triggerEvent: String,
    val category: String = "UTILITY",
    val language: String = "en",
    val exactBodyText: String,
    val variableMapping: String,
    val sampleMessage: String
)

object WhatsAppTemplates {
    val templates: List<MetaWhatsAppTemplate> = listOf(
        MetaWhatsAppTemplate(
            name = "battle_box_expiry_3_days",
            triggerEvent = "Daily 09:00 IST cron when membership expires in 3 days",
            exactBodyText = "Hi {{1}}, your Battle Box {{2}} membership expires on {{3}} (in 3 days). Renew or keep UPI AutoPay active in the Battle Box app to keep your QR pass active without interruption.",
            variableMapping = "{{1}} = Member Name, {{2}} = Plan Name, {{3}} = Expiry Date (IST)",
            sampleMessage = "Hi Aarav, your Battle Box Premium Monthly membership expires on 16 Nov 2026 (in 3 days). Renew or keep UPI AutoPay active in the Battle Box app to keep your QR pass active without interruption."
        ),
        MetaWhatsAppTemplate(
            name = "battle_box_due_today",
            triggerEvent = "Daily 09:00 IST cron when payment / AutoPay debit is due today",
            exactBodyText = "Reminder: {{1}}, your Battle Box {{2}} renewal of INR {{3}} is due today ({{4}}). If you use UPI AutoPay (Google Pay / UPI), please ensure balance and mandate limits are ready.",
            variableMapping = "{{1}} = Member Name, {{2}} = Plan Name, {{3}} = Amount INR, {{4}} = Due Date",
            sampleMessage = "Reminder: Aarav, your Battle Box Premium Monthly renewal of INR 2999 is due today (13 Nov 2026). If you use UPI AutoPay (Google Pay / UPI), please ensure balance and mandate limits are ready."
        ),
        MetaWhatsAppTemplate(
            name = "battle_box_payment_success",
            triggerEvent = "Razorpay payment.captured / invoice.paid verified webhook",
            exactBodyText = "Payment confirmed at Battle Box: We received INR {{1}} for {{2}} (Invoice {{3}}). Your pass is active until {{4}}. Open the Battle Box app anytime to download your GST PDF invoice.",
            variableMapping = "{{1}} = Amount INR, {{2}} = Plan Name, {{3}} = Invoice Number, {{4}} = Valid Until Date",
            sampleMessage = "Payment confirmed at Battle Box: We received INR 2999 for Premium Monthly (Invoice BB-2026-0142). Your pass is active until 13 Dec 2026. Open the Battle Box app anytime to download your GST PDF invoice."
        ),
        MetaWhatsAppTemplate(
            name = "battle_box_payment_failed",
            triggerEvent = "Razorpay payment.failed / subscription.halted verified webhook",
            exactBodyText = "Action needed: {{1}}, your Battle Box payment of INR {{2}} for {{3}} failed ({{4}}). Please retry via UPI or Google Pay in the Battle Box app before your pass expires.",
            variableMapping = "{{1}} = Member Name, {{2}} = Amount INR, {{3}} = Plan Name, {{4}} = Failure Reason",
            sampleMessage = "Action needed: Aarav, your Battle Box payment of INR 2999 for Premium Monthly failed (UPI mandate limit exceeded). Please retry via UPI or Google Pay in the Battle Box app before your pass expires."
        ),
        MetaWhatsAppTemplate(
            name = "battle_box_gym_closed",
            triggerEvent = "Admin posts Closed / Holiday / Special Hours announcement",
            exactBodyText = "Battle Box Alert ({{1}}): {{2}}. Effective timings/notes: {{3}}. Check the Battle Box app banner for live updates before heading in.",
            variableMapping = "{{1}} = Status Type, {{2}} = Announcement Title/Reason, {{3}} = Effective Hours or Date",
            sampleMessage = "Battle Box Alert (CLOSED): Gym closed for Diwali deep cleaning & maintenance. Effective timings/notes: Reopens 14 Nov at 05:30 IST. Check the Battle Box app banner for live updates before heading in."
        )
    )
}
