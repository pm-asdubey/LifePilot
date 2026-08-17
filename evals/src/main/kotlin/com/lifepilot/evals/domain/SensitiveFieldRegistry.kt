package com.lifepilot.evals.domain

// Local copy of SensitiveFieldRegistry — avoids Android transitive dependency.
// Keep in sync with domain/src/main/java/com/lifepilot/domain/security/SensitiveFieldRegistry.kt

object SensitiveFieldRegistry {

    val sensitiveFieldIds: Set<String> = setOf(
        "aadhaar_number", "account_number", "bank_account_number", "card_number",
        "credit_card_number", "cvv", "dob", "date_of_birth", "dl_number",
        "driving_licence_number", "email", "email_address", "employee_id",
        "ifsc_code", "income", "mobile_number", "passport_number", "password",
        "pan_number", "phone_number", "pin", "pin_code", "policy_number",
        "salary", "sin_number", "social_security_number", "tax_id", "upi_id",
        "vehicle_registration_number", "vin",
    )

    private val sensitiveValuePatterns: List<Regex> = listOf(
        """\b\d{4}\s?\d{4}\s?\d{4}\b""".toRegex(),
        """\b[A-Z]{5}\d{4}[A-Z]\b""".toRegex(),
        """\b(?:\d[ -]*?){13,16}\b""".toRegex(),
    )

    private val structuredSensitivePattern: Regex by lazy {
        val fieldGroup = sensitiveFieldIds.joinToString("|") { Regex.escape(it) }
        """(?i)($fieldGroup)\s*[:=]\s*([\"']?[^\"'\s,;]+)""".toRegex()
    }

    fun scrub(message: String): String {
        if (message.isBlank()) return message
        var scrubbed = structuredSensitivePattern.replace(message, "$1 = [REDACTED]")
        sensitiveValuePatterns.forEach { scrubbed = it.replace(scrubbed, "[REDACTED]") }
        return scrubbed
    }

    fun isSensitive(fieldId: String): Boolean =
        sensitiveFieldIds.any { it.equals(fieldId, ignoreCase = true) }
}
