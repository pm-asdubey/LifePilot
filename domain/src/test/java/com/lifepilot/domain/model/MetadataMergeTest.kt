package com.lifepilot.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MetadataMergeTest {

    @Test
    fun `APPEND concatenates onto existing value on a new line`() {
        assertThat(MetadataMerge.merge(UpdateMode.APPEND, "old", "new")).isEqualTo("old\nnew")
    }

    @Test
    fun `APPEND with no existing value returns the incoming value`() {
        assertThat(MetadataMerge.merge(UpdateMode.APPEND, null, "new")).isEqualTo("new")
        assertThat(MetadataMerge.merge(UpdateMode.APPEND, "", "new")).isEqualTo("new")
        assertThat(MetadataMerge.merge(UpdateMode.APPEND, "   ", "new")).isEqualTo("new")
    }

    @Test
    fun `SET always returns the incoming value`() {
        assertThat(MetadataMerge.merge(UpdateMode.SET, "old", "new")).isEqualTo("new")
        assertThat(MetadataMerge.merge(UpdateMode.SET, null, "new")).isEqualTo("new")
    }
}
