package com.lifepilot.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for AiProposal model invariants.
 * Regression guards for ISSUE-085 (document path null) and ISSUE-086 (task projectId).
 */
class AiProposalTest {

    // ── ISSUE-086: TaskCreation.projectId ────────────────────────────────────

    @Test
    fun `TaskCreation has nullable projectId defaulting to null`() {
        val proposal = AiProposal.TaskCreation(
            proposalId = "p1",
            summary = "Do something",
            title = "My task",
            description = null,
            dueDate = null,
            goalId = null,
            objectId = null,
        )
        assertThat(proposal.projectId).isNull()
    }

    @Test
    fun `TaskCreation carries projectId when set`() {
        val proposal = AiProposal.TaskCreation(
            proposalId = "p1",
            summary = "Add to project",
            title = "My task",
            description = null,
            dueDate = LocalDate.of(2026, 10, 1),
            goalId = null,
            objectId = null,
            projectId = "proj-japan-2026",
        )
        assertThat(proposal.projectId).isEqualTo("proj-japan-2026")
    }

    @Test
    fun `TaskCreation copy preserves projectId`() {
        val original = AiProposal.TaskCreation(
            proposalId = "p1",
            summary = "s",
            title = "T",
            description = null,
            dueDate = null,
            goalId = null,
            objectId = null,
            projectId = "proj-abc",
        )
        val copied = original.copy(title = "Updated title")
        assertThat(copied.projectId).isEqualTo("proj-abc")
    }

    // ── ISSUE-085: ObjectCreation.attachedFilePath must survive copy() ────────

    @Test
    fun `ObjectCreation copy preserves attachedFilePath`() {
        val proposal = AiProposal.ObjectCreation(
            proposalId = "p2",
            summary = "Save passport",
            objectType = "passport",
            domain = "Identity",
            title = "My Passport",
            initialNotes = null,
            attachedFilePath = "/data/user/0/files/passport.pdf",
            attachedFileName = "passport.pdf",
            attachedMimeType = "application/pdf",
        )
        val withTitle = proposal.copy(title = "Indian Passport")
        assertThat(withTitle.attachedFilePath).isEqualTo("/data/user/0/files/passport.pdf")
        assertThat(withTitle.attachedMimeType).isEqualTo("application/pdf")
    }

    @Test
    fun `ObjectCreation copy can override attachedFilePath`() {
        val proposal = AiProposal.ObjectCreation(
            proposalId = "p2",
            summary = "s",
            objectType = "passport",
            domain = "Identity",
            title = "Passport",
            initialNotes = null,
            attachedFilePath = null,
        )
        val withFile = proposal.copy(
            attachedFilePath = "/storage/passport.pdf",
            attachedFileName = "passport.pdf",
            attachedMimeType = "application/pdf",
        )
        assertThat(withFile.attachedFilePath).isEqualTo("/storage/passport.pdf")
        assertThat(proposal.attachedFilePath).isNull()
    }
}
