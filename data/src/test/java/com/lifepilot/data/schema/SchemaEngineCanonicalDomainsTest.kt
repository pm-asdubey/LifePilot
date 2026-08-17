package com.lifepilot.data.schema

import android.content.Context
import android.content.res.AssetManager
import com.google.common.truth.Truth.assertThat
import com.lifepilot.domain.engine.SchemaEngine
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test

/**
 * Regression tests for canonical domain list in SchemaEngineImpl.
 *
 * CRITICAL INVARIANT: getAllDomains() must return ALL known life domains even when
 * no schemas have been loaded yet (e.g., fresh install, assets not yet parsed).
 *
 * Root cause of regression (ISSUE-48): SchemaEngineImpl.getAllDomains() previously
 * derived domains only from _registeredSchemas. If schemas hadn't loaded yet, or if
 * a schema used a different domain name (e.g., "Employment" instead of "Career"), the
 * full canonical list would not appear in the Library domain filter chips.
 *
 * Fix: a hardcoded canonicalDomains baseline is unioned with schema-derived domains.
 */
class SchemaEngineCanonicalDomainsTest {

    private lateinit var engine: SchemaEngine

    private val expectedCanonicalDomains = listOf(
        "Career", "Education", "Finance", "Health", "Home",
        "Identity", "Legal", "Major Life Events", "People",
        "Property", "Transport", "Travel",
    )

    @Before
    fun setUp() {
        // Context that returns an empty schema directory — simulates cold start / no schemas.
        val assetManager = mockk<AssetManager> {
            every { list("schemas") } returns emptyArray()
        }
        val context = mockk<Context> {
            every { assets } returns assetManager
        }
        engine = SchemaEngineImpl(context)
    }

    @Test
    fun `getAllDomains returns canonical list even with no schemas loaded`() {
        val domains = engine.getAllDomains()
        assertThat(domains).containsAtLeastElementsIn(expectedCanonicalDomains)
    }

    @Test
    fun `getAllDomains does not contain Employment — Interview schema maps to Career`() {
        // "Employment" was the old domain name. ISSUE-48 fix renamed it to "Career" in
        // Interview.json. The canonical list must never include "Employment".
        val domains = engine.getAllDomains()
        assertThat(domains).doesNotContain("Employment")
    }

    @Test
    fun `getAllDomains list is sorted alphabetically`() {
        val domains = engine.getAllDomains()
        assertThat(domains).isInOrder()
    }

    @Test
    fun `getAllDomains result is deduplicated`() {
        val domains = engine.getAllDomains()
        assertThat(domains).containsNoDuplicates()
    }

    @Test
    fun `Career is in canonical list`() {
        assertThat(engine.getAllDomains()).contains("Career")
    }

    @Test
    fun `Major Life Events is in canonical list`() {
        // Multi-word domain name — common source of typos
        assertThat(engine.getAllDomains()).contains("Major Life Events")
    }
}
