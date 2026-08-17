package com.lifepilot.designsystem.components

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Regression guard for the AI "thinking" indicator's cycling copy. The thinking state (compass
 * avatar + WhatsApp-style dots + rotating words) has regressed before; this locks the rotation
 * behaviour so it can't silently break.
 */
class ThinkingMessagesTest {

    @Test
    fun `there are multiple messages to cycle through`() {
        assertThat(ThinkingMessages.all.size).isAtLeast(3)
        assertThat(ThinkingMessages.all).containsNoDuplicates()
        assertThat(ThinkingMessages.all.none { it.isBlank() }).isTrue()
    }

    @Test
    fun `next advances by one and wraps at the end`() {
        assertThat(ThinkingMessages.next(0)).isEqualTo(1)
        val last = ThinkingMessages.all.size - 1
        assertThat(ThinkingMessages.next(last)).isEqualTo(0)
    }

    @Test
    fun `messageAt is safe for any index including negatives`() {
        assertThat(ThinkingMessages.messageAt(0)).isEqualTo(ThinkingMessages.all[0])
        assertThat(ThinkingMessages.messageAt(ThinkingMessages.all.size)).isEqualTo(ThinkingMessages.all[0])
        assertThat(ThinkingMessages.messageAt(-1)).isEqualTo(ThinkingMessages.all.last())
    }
}
