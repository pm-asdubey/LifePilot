package com.lifepilot.domain.security

/**
 * Canonical registry of metadata field IDs and patterns that may carry personally
 * identifiable or otherwise sensitive information.
 *
 * Used by logging and outbound-prompt scrubbers to prevent sensitive values from
 * leaving the device unintentionally.
 */
object SensitiveFieldRegistry {

    /**
     * Field IDs whose values should be redacted when they appear in logs or prompts.
     * Keep this sorted alphabetically and append new fields as schemas evolve.
     */
    val sensitiveFieldIds: Set<String> = setOf(
        "aadhaar_number",
        "account_number",
        "bank_account_number",
        "card_number",
        "credit_card_number",
        "cvv",
        "dob",
        "date_of_birth",
        "dl_number",
        "driving_licence_number",
        "email",
        "email_address",
        "employee_id",
        "ifsc_code",
        "income",
        "mobile_number",
        "passport_number",
        "password",
        "pan_number",
        "phone_number",
        "pin",
        "pin_code",
        "policy_number",
        "salary",
        "sin_number",
        "social_security_number",
        "tax_id",
        "upi_id",
        "vehicle_registration_number",
        "vin",
    )

    /**
     * Regex patterns that match common sensitive values regardless of field ID.
     * These catch values that may leak outside of structured metadata logs.
     */
    private val sensitiveValuePatterns: List<Regex> = listOf(
        // 12-digit Aadhaar (with or without spaces)
        """\b\d{4}\s?\d{4}\s?\d{4}\b""".toRegex(),
        // PAN: 5 upper, 4 digits, 1 upper
        """\b[A-Z]{5}\d{4}[A-Z]\b""".toRegex(),
        // 16-digit card/account numbers (with or without spaces/hyphens)
        """\b(?:\d[ -]*?){13,16}\b""".toRegex(),
    )

    /**
     * Regex that matches a sensitive field key followed by a value separator.
     * Group 1 captures the key; group 2 captures the value portion we want to redact.
     */
    private val structuredSensitivePattern: Regex by lazy {
        val fieldGroup = sensitiveFieldIds.joinToString("|") { Regex.escape(it) }
        """(?i)($fieldGroup)\s*[:=]\s*([\"']?[^\"'\s,;]+)""".toRegex()
    }

    /**
     * Replace any known sensitive field values and standalone sensitive patterns
     * with `[REDACTED]`.
     */
    fun scrub(message: String): String {
        if (message.isBlank()) return message

        var scrubbed = structuredSensitivePattern.replace(message, "$1 = [REDACTED]")

        sensitiveValuePatterns.forEach { pattern ->
            scrubbed = pattern.replace(scrubbed, "[REDACTED]")
        }

        return scrubbed
    }

    /**
     * Returns true if [fieldId] is a known sensitive field.
     */
    fun isSensitive(fieldId: String): Boolean =
        sensitiveFieldIds.any { it.equals(fieldId, ignoreCase = true) }
}
