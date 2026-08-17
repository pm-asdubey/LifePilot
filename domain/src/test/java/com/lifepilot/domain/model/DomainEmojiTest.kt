package com.lifepilot.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Guards the deterministic domain → emoji mapping (ADR-002) used to give AI-proposed projects a
 * domain-appropriate emoji instead of a generic marker.
 */
class DomainEmojiTest {

    @Test
    fun `each canonical domain maps to a non-default emoji`() {
        val canonical = listOf(
            "Career", "Education", "Finance", "Health", "Home", "Identity",
            "Legal", "Major Life Events", "People", "Property", "Transport", "Travel",
        )
        canonical.forEach { domain ->
            assertThat(DomainEmoji.forDomain(domain)).isNotEqualTo(DomainEmoji.DEFAULT)
        }
    }

    @Test
    fun `mapping is case insensitive`() {
        assertThat(DomainEmoji.forDomain("career")).isEqualTo(DomainEmoji.forDomain("Career"))
        assertThat(DomainEmoji.forDomain("TRAVEL")).isEqualTo(DomainEmoji.forDomain("Travel"))
    }

    @Test
    fun `null blank and unknown domains fall back to default`() {
        assertThat(DomainEmoji.forDomain(null)).isEqualTo(DomainEmoji.DEFAULT)
        assertThat(DomainEmoji.forDomain("")).isEqualTo(DomainEmoji.DEFAULT)
        assertThat(DomainEmoji.forDomain("   ")).isEqualTo(DomainEmoji.DEFAULT)
        assertThat(DomainEmoji.forDomain("Nonexistent")).isEqualTo(DomainEmoji.DEFAULT)
    }
}
