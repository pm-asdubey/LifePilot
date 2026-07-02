package com.lifepilot.domain.security

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SensitiveFieldRegistryTest {

    @Test
    fun `scrub replaces structured sensitive field values`() {
        val input = "passport_number = X1234567, aadhaar_number = 1234 5678 9012"
        val output = SensitiveFieldRegistry.scrub(input)

        assertThat(output).doesNotContain("X1234567")
        assertThat(output).doesNotContain("1234 5678 9012")
        assertThat(output).contains("passport_number = [REDACTED]")
        assertThat(output).contains("aadhaar_number = [REDACTED]")
    }

    @Test
    fun `scrub masks standalone Aadhaar numbers`() {
        val input = "My Aadhaar is 123456789012 and PAN is ABCDE1234F"
        val output = SensitiveFieldRegistry.scrub(input)

        assertThat(output).doesNotContain("123456789012")
        assertThat(output).doesNotContain("ABCDE1234F")
        assertThat(output).contains("[REDACTED]")
    }

    @Test
    fun `scrub leaves non sensitive text unchanged`() {
        val input = "The quick brown fox jumps over the lazy dog."
        val output = SensitiveFieldRegistry.scrub(input)

        assertThat(output).isEqualTo(input)
    }

    @Test
    fun `isSensitive returns true for known sensitive fields`() {
        assertThat(SensitiveFieldRegistry.isSensitive("passport_number")).isTrue()
        assertThat(SensitiveFieldRegistry.isSensitive("PAN_NUMBER")).isTrue()
    }

    @Test
    fun `isSensitive returns false for unknown fields`() {
        assertThat(SensitiveFieldRegistry.isSensitive("favorite_color")).isFalse()
    }
}
