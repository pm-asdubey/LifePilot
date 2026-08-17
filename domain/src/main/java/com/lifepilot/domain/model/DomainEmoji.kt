package com.lifepilot.domain.model

/**
 * Deterministic domain → emoji mapping used when an AI-proposed Project does not carry its own
 * emoji (ADR-002 "Emoji Assignment"). Keeps AI-created projects visually consistent with their
 * life domain instead of falling back to a generic marker.
 */
object DomainEmoji {

    /** Fallback used for unknown domains or manual projects with no chosen emoji. */
    const val DEFAULT = "🎯"

    private val byDomain = mapOf(
        "Career" to "💼",
        "Education" to "🎓",
        "Finance" to "💰",
        "Health" to "🩺",
        "Home" to "🏠",
        "Identity" to "🪪",
        "Legal" to "⚖️",
        "Major Life Events" to "🎉",
        "People" to "👥",
        "Property" to "🏡",
        "Transport" to "🚗",
        "Travel" to "✈️",
    )

    /** Returns the emoji for [domain], or [DEFAULT] when the domain is null/blank/unknown. */
    fun forDomain(domain: String?): String {
        if (domain.isNullOrBlank()) return DEFAULT
        // Case-insensitive match so "career" and "Career" both resolve.
        return byDomain[domain]
            ?: byDomain.entries.firstOrNull { it.key.equals(domain, ignoreCase = true) }?.value
            ?: DEFAULT
    }
}
