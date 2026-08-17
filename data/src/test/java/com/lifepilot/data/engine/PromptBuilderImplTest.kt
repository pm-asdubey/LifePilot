package com.lifepilot.data.engine

import com.google.common.truth.Truth.assertThat
import com.lifepilot.domain.model.AiObjectContext
import com.lifepilot.domain.model.DomainLifeState
import com.lifepilot.domain.model.MetadataEntry
import com.lifepilot.domain.model.MetadataFieldType
import com.lifepilot.domain.model.MetadataSource
import com.lifepilot.domain.model.ObjectSnapshot
import com.lifepilot.domain.model.ObjectStatus
import com.lifepilot.domain.model.Project
import com.lifepilot.domain.model.ProjectStatus
import com.lifepilot.domain.model.RetrievalContext
import com.lifepilot.domain.model.VerificationStatus
import org.junit.Test
import java.time.Instant

class PromptBuilderImplTest {

    private val builder = PromptBuilderImpl()

    @Test
    fun `build redacts sensitive metadata values before returning prompt`() {
        val sensitiveValue = "1234 5678 9012" // matches Aadhaar-like pattern
        val panValue = "ABCDE1234F"
        val snapshot = ObjectSnapshot(
            objectId = "obj-1",
            title = "My Passport",
            objectType = "Passport",
            domain = "Identity",
            status = ObjectStatus.ACTIVE,
            metadata = listOf(
                MetadataEntry(
                    metadataId = "meta-1",
                    objectId = "obj-1",
                    fieldId = "aadhaar_number",
                    fieldType = MetadataFieldType.TEXT,
                    value = sensitiveValue,
                    version = 1,
                    confidence = null,
                    source = MetadataSource.USER,
                    verificationStatus = VerificationStatus.VERIFIED,
                    updatedAt = Instant.now(),
                ),
                MetadataEntry(
                    metadataId = "meta-2",
                    objectId = "obj-1",
                    fieldId = "pan_number",
                    fieldType = MetadataFieldType.TEXT,
                    value = panValue,
                    version = 1,
                    confidence = null,
                    source = MetadataSource.USER,
                    verificationStatus = VerificationStatus.VERIFIED,
                    updatedAt = Instant.now(),
                ),
            ),
            aiContext = null,
            pendingTaskCount = 0,
            documentCount = 0,
        )

        val context = RetrievalContext(
            profileId = "profile-1",
            profileName = "Test Profile",
            domainLifeStates = emptyMap(),
            relevantSnapshots = listOf(snapshot),
            totalObjectCount = 1,
            allObjectIndex = emptyMap(),
            allObjectDomainIndex = emptyMap(),
            allObjectMetadata = emptyMap(),
            pendingTasks = emptyList(),
            upcomingReminders = emptyList(),
        )

        val prompt = builder.build(context, "What is my Aadhaar number?")

        assertThat(prompt).doesNotContain(sensitiveValue)
        assertThat(prompt).doesNotContain(panValue)
        assertThat(prompt).contains("aadhaar_number = [REDACTED]")
        assertThat(prompt).contains("pan_number = [REDACTED]")
        // Non-sensitive structure should remain intact.
        assertThat(prompt).contains("My Passport")
        assertThat(prompt).contains("Passport")
    }

    @Test
    fun `build includes PART_BREAK instruction in system prompt`() {
        val context = emptyContext()
        val prompt = builder.build(context, "Hello")
        assertThat(prompt).contains("[PART_BREAK]")
    }

    @Test
    fun `build includes active project title when projects present`() {
        val project = Project(
            projectId = "proj-1",
            profileId = "profile-1",
            title = "Canada Trip 2026",
            description = "Summer vacation planning",
            domain = "Travel",
            status = ProjectStatus.ACTIVE,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        val context = emptyContext().copy(activeProjects = listOf(project))

        val prompt = builder.build(context, "What should I do next?")

        assertThat(prompt).contains("Canada Trip 2026")
        assertThat(prompt).contains("Active projects")
    }

    @Test
    fun `build with empty activeProjects does not include projects section`() {
        val context = emptyContext().copy(activeProjects = emptyList())
        val prompt = builder.build(context, "Hello")
        assertThat(prompt).doesNotContain("Active projects")
    }

    @Test
    fun `build preserves domain life state structure`() {
        val state = DomainLifeState(
            profileId = "profile-1",
            domain = "Identity",
            currentSituation = "Passport expires in six months.",
            currentPriorities = listOf("Renew passport"),
            knownRisks = emptyList(),
            openQuestions = emptyList(),
            recommendations = emptyList(),
            recentChanges = emptyList(),
            lastUpdated = Instant.now(),
            version = 1,
        )

        val context = RetrievalContext(
            profileId = "profile-1",
            profileName = "Test Profile",
            domainLifeStates = mapOf("Identity" to state),
            relevantSnapshots = emptyList(),
            totalObjectCount = 0,
            allObjectIndex = emptyMap(),
            allObjectDomainIndex = emptyMap(),
            allObjectMetadata = emptyMap(),
            pendingTasks = emptyList(),
            upcomingReminders = emptyList(),
        )

        val prompt = builder.build(context, "How am I doing?")

        assertThat(prompt).contains("[Identity]")
        assertThat(prompt).contains("Passport expires in six months.")
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun emptyContext() = RetrievalContext(
        profileId = "profile-1",
        profileName = "Test Profile",
        domainLifeStates = emptyMap(),
        relevantSnapshots = emptyList(),
        totalObjectCount = 0,
        allObjectIndex = emptyMap(),
        allObjectDomainIndex = emptyMap(),
        allObjectMetadata = emptyMap(),
        pendingTasks = emptyList(),
        upcomingReminders = emptyList(),
        activeProjects = emptyList(),
    )
}
