package com.lifepilot.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Classifies a document/record by its expiry date. Extracted from the object-detail UI so the
 * "expiring within N days" rule (visas, passports, policies) is a testable domain rule, not inline
 * date math in a Composable.
 */
sealed interface ExpiryStatus {
    data object Expired : ExpiryStatus
    data class ExpiringSoon(val daysLeft: Long) : ExpiryStatus
    data object Valid : ExpiryStatus

    companion object {
        /** Returns null when there is no expiry date. */
        fun of(expiry: LocalDate?, today: LocalDate, soonWithinDays: Long = 60): ExpiryStatus? {
            if (expiry == null) return null
            val days = ChronoUnit.DAYS.between(today, expiry)
            return when {
                days < 0 -> Expired
                days <= soonWithinDays -> ExpiringSoon(days)
                else -> Valid
            }
        }
    }
}
