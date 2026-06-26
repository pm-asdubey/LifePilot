package com.lifepilot.domain.model

import com.lifepilot.domain.model.schema.ReminderRule
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderRuleTest {

    @Test
    fun `effectiveTitle returns title when set`() {
        val rule = ReminderRule(
            ruleId = "r1",
            triggerField = "expiry_date",
            offsetDays = -30,
            title = "Expiry Warning",
            displayName = "Expiry",
        )
        assertEquals("Expiry Warning", rule.effectiveTitle)
    }

    @Test
    fun `effectiveTitle falls back to displayName when title is blank`() {
        val rule = ReminderRule(
            ruleId = "r1",
            triggerField = "expiry_date",
            offsetDays = -30,
            title = "",
            displayName = "Expiry Display",
        )
        assertEquals("Expiry Display", rule.effectiveTitle)
    }

    @Test
    fun `effectiveTitle returns empty when both blank`() {
        val rule = ReminderRule(
            ruleId = "r1",
            triggerField = "expiry_date",
            offsetDays = -30,
        )
        assertEquals("", rule.effectiveTitle)
    }
}
