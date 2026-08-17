package com.lifepilot.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class ExpiryStatusTest {

    private val today = LocalDate.of(2026, 7, 3)

    @Test
    fun `null expiry returns null`() {
        assertThat(ExpiryStatus.of(null, today)).isNull()
    }

    @Test
    fun `past date is Expired`() {
        assertThat(ExpiryStatus.of(today.minusDays(1), today)).isEqualTo(ExpiryStatus.Expired)
    }

    @Test
    fun `within 60 days is ExpiringSoon with days left`() {
        assertThat(ExpiryStatus.of(today.plusDays(30), today))
            .isEqualTo(ExpiryStatus.ExpiringSoon(30))
        // Boundary: exactly the window is still "soon".
        assertThat(ExpiryStatus.of(today.plusDays(60), today))
            .isEqualTo(ExpiryStatus.ExpiringSoon(60))
        // Today counts as 0 days left, still soon.
        assertThat(ExpiryStatus.of(today, today)).isEqualTo(ExpiryStatus.ExpiringSoon(0))
    }

    @Test
    fun `beyond the window is Valid`() {
        assertThat(ExpiryStatus.of(today.plusDays(61), today)).isEqualTo(ExpiryStatus.Valid)
    }
}
