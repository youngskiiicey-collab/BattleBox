package com.example.data.legal

enum class LegalCategory(val label: String) {
    TERMS("Terms & Conditions"),
    WAIVER("Health Waiver"),
    PRIVACY_DPDP("Privacy & DPDP 2023")
}

data class LegalSection(
    val title: String,
    val body: String,
    val category: LegalCategory
)

object LegalDrafts {
    const val TERMS_VERSION = "1.0-draft"
    const val DRAFT_NOTICE = "DRAFT — NEEDS LEGAL REVIEW BEFORE COMMERCIAL LAUNCH"
    const val DATA_FIDUCIARY_NAME = "Battle Box Fitness & Conditioning (Data Fiduciary)"
    const val GRIEVANCE_OFFICER_CONTACT = "Front Desk / Grievance Desk (privacy@battlebox.in — draft placeholder)"

    val sections: List<LegalSection> = listOf(
        LegalSection(
            title = "1. Membership & Eligibility",
            body = "Battle Box membership is personal, non-transferable, and granted to individuals aged 18 years or older (or minors aged 14–17 with verified guardian consent). Facility access requires an active membership status, completed onboarding, and valid rotating QR check-in. Sharing passes, QR screenshots, or proxy check-ins constitutes grounds for immediate suspension.",
            category = LegalCategory.TERMS
        ),
        LegalSection(
            title = "2. Fees, Billing & Currency (INR)",
            body = "All membership fees, personal training packages, and add-on plans are billed in Indian Rupees (INR) inclusive or exclusive of applicable GST as specified on the invoice. Access is contingent upon timely settlement of dues. Battle Box reserves the right to revise plan tariffs with prior notice prior to renewal.",
            category = LegalCategory.TERMS
        ),
        LegalSection(
            title = "3. Refunds & Cancellation Policy",
            body = "Membership fees once activated are generally non-refundable except in cases of documented medical inability to train, permanent relocation beyond 25 km, or facility closure exceeding 14 consecutive days, evaluated on a pro-rata basis minus administrative charges. Recurring mandates (when enabled) may be cancelled prior to the next billing cycle.",
            category = LegalCategory.TERMS
        ),
        LegalSection(
            title = "4. Membership Freezing / Hold",
            body = "Eligible quarterly and annual members may request a temporary freeze for medical reasons or extended travel (minimum 14 days, maximum 60 days per membership year) by submitting a written request to Front Desk or Admin prior to the absence. Retroactive freezes are not permitted.",
            category = LegalCategory.TERMS
        ),
        LegalSection(
            title = "5. Health Declaration & Liability Waiver",
            body = "Strength training, combat conditioning, heavy bag work, and high-intensity exercise involve inherent physical risks including musculoskeletal strain, falls, or cardiovascular events. By checking in, you declare that you are medically fit to undertake strenuous exercise, have disclosed relevant conditions or consulted a physician where advised, and voluntarily assume all ordinary risks of training at Battle Box. To the fullest extent permitted under Indian law, Battle Box, its owners, and trainers are released from liability for injuries arising from improper equipment use, ignoring safety instructions, or undisclosed pre-existing conditions.",
            category = LegalCategory.WAIVER
        ),
        LegalSection(
            title = "6. CCTV & Facility Surveillance",
            body = "Common training floors, entrances, reception desks, and equipment bays at Battle Box operate 24/7 CCTV video surveillance for member safety, theft prevention, and incident verification. Restrooms and changing rooms are strictly excluded from surveillance. Footage is retained only for security windows and shared solely with authorized personnel or law enforcement when legally required.",
            category = LegalCategory.TERMS
        ),
        LegalSection(
            title = "7. Code of Conduct & Facility Rules",
            body = "Members must wear clean athletic footwear, use a workout towel, re-rack plates and dumbbells after sets, treat equipment and heavy bags with care, and respect fellow athletes and staff. Harassment, intimidation, unsafe spotting, unauthorized commercial coaching, or training under the influence of alcohol/banned substances will result in immediate termination without refund.",
            category = LegalCategory.TERMS
        ),
        LegalSection(
            title = "8. Trainer Terms & Coaching Boundaries",
            body = "Trainers assigned through Battle Box provide programming and form guidance strictly within their scope of fitness certification. Trainers are not licensed physicians, physiotherapists, or medical nutritionists and do not diagnose injuries or prescribe medical treatment. Outside-app private cash solicitation inside the facility is prohibited.",
            category = LegalCategory.TERMS
        ),
        LegalSection(
            title = "9. Privacy Policy — Personal Data Collected & Purpose Limitation",
            body = "$DATA_FIDUCIARY_NAME collects only personal data strictly necessary for facility access and safety: (a) Google account identity details (UID, full name, email) and optional mobile/emergency contact; (b) foreground GPS latitude/longitude and accuracy telemetry captured strictly at the moment of QR check-in to verify presence inside the configured gym geofence (never tracked continuously in the background); (c) check-in/check-out timestamps and 3-hour auto-checkout state; and (d) your health waiver attestation and consent audit log. Battle Box never sells or shares member data with third-party advertisers.",
            category = LegalCategory.PRIVACY_DPDP
        ),
        LegalSection(
            title = "10. Privacy Policy — Data Residency (asia-south1) & Retention",
            body = "All application records, consent receipts, and attendance logs are stored in Google Cloud / Firebase infrastructure pinned to asia-south1 (Mumbai, India). Active profile and attendance history are retained during active membership and erased or anonymized within 30 days of account deletion/withdrawal request, unless short-term retention is required for unresolved safety incidents, immutable security audit trails, or statutory tax/billing compliance.",
            category = LegalCategory.PRIVACY_DPDP
        ),
        LegalSection(
            title = "11. Digital Personal Data Protection (DPDP) Act, 2023 Notice & Rights",
            body = "Pursuant to the Digital Personal Data Protection Act, 2023 (India), you ('Data Principal') grant free, specific, informed, unconditional, and unambiguous consent for Battle Box to process your personal data for the itemized purposes above. Every acceptance writes an immutable server-timestamped consent record in Firestore. You have the right to: (1) access a summary of personal data processed; (2) request correction, completion, or updating of inaccurate data; (3) withdraw consent at any time (with the effect of suspending app-based QR check-in access); (4) request erasure when retention is no longer legally necessary; (5) nominate an individual to exercise your rights in the event of death or incapacity; and (6) file grievances with $GRIEVANCE_OFFICER_CONTACT before escalating to the Data Protection Board of India.",
            category = LegalCategory.PRIVACY_DPDP
        )
    )
}
