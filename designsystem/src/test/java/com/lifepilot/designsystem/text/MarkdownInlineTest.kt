package com.lifepilot.designsystem.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MarkdownInlineTest {

    @Test
    fun `plain text is a single unstyled run`() {
        val runs = MarkdownInline.parse("just some text")
        assertThat(runs).hasSize(1)
        assertThat(runs[0]).isEqualTo(MarkdownRun("just some text"))
    }

    @Test
    fun `double-asterisk becomes a bold run`() {
        val runs = MarkdownInline.parse("Book the **venue** now")
        assertThat(runs).containsExactly(
            MarkdownRun("Book the "),
            MarkdownRun("venue", bold = true),
            MarkdownRun(" now"),
        ).inOrder()
    }

    @Test
    fun `single-asterisk becomes an italic run`() {
        val runs = MarkdownInline.parse("this is *important*")
        assertThat(runs).containsExactly(
            MarkdownRun("this is "),
            MarkdownRun("important", italic = true),
        ).inOrder()
    }

    @Test
    fun `heading line renders bold and drops the hashes`() {
        val runs = MarkdownInline.parse("## Wedding plan")
        assertThat(runs).containsExactly(MarkdownRun("Wedding plan", bold = true))
    }

    @Test
    fun `newlines are preserved as their own runs`() {
        val runs = MarkdownInline.parse("1. First\n2. Second")
        assertThat(runs.map { it.text }).containsExactly("1. First", "\n", "2. Second").inOrder()
    }

    @Test
    fun `backticks are stripped`() {
        val runs = MarkdownInline.parse("run `gradlew test`")
        assertThat(runs.single().text).isEqualTo("run gradlew test")
    }
}
