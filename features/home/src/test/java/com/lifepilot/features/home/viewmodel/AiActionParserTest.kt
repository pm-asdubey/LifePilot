package com.lifepilot.features.home.viewmodel

import com.google.common.truth.Truth.assertThat
import com.lifepilot.domain.engine.SchemaEngine
import com.lifepilot.domain.model.ActionItem
import com.lifepilot.domain.model.AiProposal
import com.lifepilot.domain.model.ObjectStatus
import com.lifepilot.domain.model.UpdateMode
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test

class AiActionParserTest {

    private val schemaEngine = mockk<SchemaEngine>(relaxed = true)
    private lateinit var parser: AiActionParser

    @Before
    fun setUp() {
        parser = AiActionParser(schemaEngine)
    }

    // ── Failure / guard cases ────────────────────────────────────────────────

    @Test
    fun `parse plain string OBJECT_CREATION returns null`() {
        // This was the root bug: AI returned bare "OBJECT_CREATION" instead of JSON,
        // causing JSONObject("OBJECT_CREATION") to throw JSONException.
        val result = parser.parseAction("OBJECT_CREATION")
        assertThat(result).isNull()
    }

    @Test
    fun `parse empty string returns null`() {
        val result = parser.parseAction("")
        assertThat(result).isNull()
    }

    @Test
    fun `parse malformed JSON returns null`() {
        val result = parser.parseAction("{actionType: OBJECT_CREATION")
        assertThat(result).isNull()
    }

    @Test
    fun `parse JSON array instead of object returns null`() {
        val result = parser.parseAction("""["OBJECT_CREATION"]""")
        assertThat(result).isNull()
    }

    // ── OBJECT_CREATION ──────────────────────────────────────────────────────

    @Test
    fun `parse OBJECT_CREATION with stray action-type prefix before JSON`() {
        // RCA of scanned docs failing to classify: the model emitted the action type as a bare
        // token before the object ("OBJECT_CREATION {…}") with no "actionType" field, so
        // JSONObject("OBJECT_CREATION …") threw. The parser now isolates the {…} and recovers the
        // action type from the prefix.
        val raw = """
            OBJECT_CREATION
            {"summary": "Found a PAN card", "objectType": "pan_card", "domain": "Identity", "title": "PAN Card", "fields": []}
        """.trimIndent()

        val result = parser.parseAction(raw)

        assertThat(result).isInstanceOf(AiProposal.ObjectCreation::class.java)
        val proposal = result as AiProposal.ObjectCreation
        assertThat(proposal.objectType).isEqualTo("pan_card")
        assertThat(proposal.title).isEqualTo("PAN Card")
    }

    @Test
    fun `parse OBJECT_CREATION wrapped in markdown fences`() {
        val raw = "```json\n{\"actionType\":\"OBJECT_CREATION\",\"objectType\":\"Passport\",\"domain\":\"Identity\",\"title\":\"P\",\"fields\":[]}\n```"

        val result = parser.parseAction(raw)

        assertThat(result).isInstanceOf(AiProposal.ObjectCreation::class.java)
        assertThat((result as AiProposal.ObjectCreation).objectType).isEqualTo("Passport")
    }

    @Test
    fun `parse valid OBJECT_CREATION returns ObjectCreation proposal`() {
        val json = """
            {
              "actionType": "OBJECT_CREATION",
              "summary": "Found a passport",
              "objectType": "Passport",
              "domain": "Identity",
              "title": "My Passport",
              "fields": []
            }
        """.trimIndent()

        val result = parser.parseAction(json)

        assertThat(result).isInstanceOf(AiProposal.ObjectCreation::class.java)
        val proposal = result as AiProposal.ObjectCreation
        assertThat(proposal.objectType).isEqualTo("Passport")
        assertThat(proposal.domain).isEqualTo("Identity")
        assertThat(proposal.title).isEqualTo("My Passport")
        assertThat(proposal.summary).isEqualTo("Found a passport")
    }

    @Test
    fun `parse OBJECT_CREATION extracts fields array correctly`() {
        val json = """
            {
              "actionType": "OBJECT_CREATION",
              "summary": "",
              "objectType": "Job",
              "domain": "Career",
              "title": "Software Engineer at Acme",
              "fields": [
                {"fieldId": "company", "displayName": "Company", "value": "Acme Corp", "mode": "set"},
                {"fieldId": "notes", "displayName": "Notes", "value": "Exciting role", "mode": "append"}
              ]
            }
        """.trimIndent()

        val result = parser.parseAction(json) as AiProposal.ObjectCreation

        assertThat(result.fields).hasSize(2)
        assertThat(result.fields[0].fieldId).isEqualTo("company")
        assertThat(result.fields[0].value).isEqualTo("Acme Corp")
        assertThat(result.fields[0].mode).isEqualTo(UpdateMode.SET)
        assertThat(result.fields[1].fieldId).isEqualTo("notes")
        assertThat(result.fields[1].mode).isEqualTo(UpdateMode.APPEND)
    }

    @Test
    fun `parse OBJECT_CREATION field with mode append sets APPEND`() {
        val json = """
            {
              "actionType": "OBJECT_CREATION",
              "objectType": "Record",
              "domain": "General",
              "title": "Test",
              "fields": [{"fieldId": "notes", "value": "text", "mode": "append"}]
            }
        """.trimIndent()

        val result = parser.parseAction(json) as AiProposal.ObjectCreation
        assertThat(result.fields[0].mode).isEqualTo(UpdateMode.APPEND)
    }

    // ── TASK_CREATION ────────────────────────────────────────────────────────

    @Test
    fun `parse TASK_CREATION returns TaskCreation proposal`() {
        val json = """
            {
              "actionType": "TASK_CREATION",
              "summary": "Renew passport",
              "title": "Book appointment at passport office",
              "description": "Government website booking",
              "dueDate": "2026-09-01",
              "goalId": "goal-123"
            }
        """.trimIndent()

        val result = parser.parseAction(json)

        assertThat(result).isInstanceOf(AiProposal.TaskCreation::class.java)
        val proposal = result as AiProposal.TaskCreation
        assertThat(proposal.title).isEqualTo("Book appointment at passport office")
        assertThat(proposal.description).isEqualTo("Government website booking")
        assertThat(proposal.goalId).isEqualTo("goal-123")
        assertThat(proposal.dueDate?.toString()).isEqualTo("2026-09-01")
    }

    @Test
    fun `parse TASK_CREATION with null dueDate parses gracefully`() {
        val json = """
            {"actionType": "TASK_CREATION", "title": "Do something", "dueDate": "null"}
        """.trimIndent()

        val result = parser.parseAction(json) as AiProposal.TaskCreation
        assertThat(result.dueDate).isNull()
    }

    @Test
    fun `parse TASK_CREATION with projectId populates projectId field`() {
        // Regression for ISSUE-086: tasks added to an existing project via AI chat
        // must carry the projectId through to executeProposal so PlanningEngine links them.
        val json = """
            {
              "actionType": "TASK_CREATION",
              "summary": "Add milestone to project",
              "title": "Submit visa application",
              "dueDate": "2026-10-15",
              "projectId": "proj-japan-trip-456"
            }
        """.trimIndent()

        val result = parser.parseAction(json) as AiProposal.TaskCreation

        assertThat(result.title).isEqualTo("Submit visa application")
        assertThat(result.projectId).isEqualTo("proj-japan-trip-456")
    }

    @Test
    fun `parse TASK_CREATION without projectId leaves projectId null`() {
        val json = """
            {"actionType": "TASK_CREATION", "title": "Standalone task"}
        """.trimIndent()

        val result = parser.parseAction(json) as AiProposal.TaskCreation

        assertThat(result.projectId).isNull()
    }

    @Test
    fun `parse TASK_CREATION with explicit null projectId leaves projectId null`() {
        val json = """
            {"actionType": "TASK_CREATION", "title": "Task", "projectId": "null"}
        """.trimIndent()

        val result = parser.parseAction(json) as AiProposal.TaskCreation

        assertThat(result.projectId).isNull()
    }

    // ── GOAL_PROPOSAL ────────────────────────────────────────────────────────

    @Test
    fun `parse GOAL_PROPOSAL returns GoalProposal with tasks`() {
        val json = """
            {
              "actionType": "GOAL_PROPOSAL",
              "summary": "Renew passport before expiry",
              "title": "Passport Renewal",
              "description": "Renew Indian passport",
              "estimatedWeeks": 4,
              "suggestedTasks": ["Book appointment", "Gather documents", "Submit application"]
            }
        """.trimIndent()

        val result = parser.parseAction(json)

        assertThat(result).isInstanceOf(AiProposal.GoalProposal::class.java)
        val proposal = result as AiProposal.GoalProposal
        assertThat(proposal.title).isEqualTo("Passport Renewal")
        assertThat(proposal.estimatedWeeks).isEqualTo(4)
        assertThat(proposal.suggestedTasks).containsExactly(
            "Book appointment", "Gather documents", "Submit application"
        ).inOrder()
    }

    // ── PROJECT_CREATION ─────────────────────────────────────────────────────

    @Test
    fun `parse PROJECT_CREATION returns ProjectCreation with title and domain`() {
        val json = """
            {
              "actionType": "PROJECT_CREATION",
              "summary": "Canada trip planning",
              "title": "Canada Trip 2026",
              "description": "Summer vacation",
              "domain": "Travel",
              "linkedObjectIds": []
            }
        """.trimIndent()

        val result = parser.parseAction(json)

        assertThat(result).isInstanceOf(AiProposal.ProjectCreation::class.java)
        val proposal = result as AiProposal.ProjectCreation
        assertThat(proposal.title).isEqualTo("Canada Trip 2026")
        assertThat(proposal.domain).isEqualTo("Travel")
        assertThat(proposal.description).isEqualTo("Summer vacation")
    }

    @Test
    fun `parse PROJECT_CREATION extracts linkedObjectIds`() {
        val json = """
            {
              "actionType": "PROJECT_CREATION",
              "title": "Canada Trip",
              "linkedObjectIds": ["obj-1", "obj-2", "obj-3"]
            }
        """.trimIndent()

        val result = parser.parseAction(json) as AiProposal.ProjectCreation
        assertThat(result.linkedObjectIds).containsExactly("obj-1", "obj-2", "obj-3").inOrder()
    }

    @Test
    fun `parse PROJECT_CREATION with null domain sets domain to null`() {
        val json = """{"actionType": "PROJECT_CREATION", "title": "Side project", "domain": "null"}"""

        val result = parser.parseAction(json) as AiProposal.ProjectCreation
        assertThat(result.domain).isNull()
    }

    // ── STATUS_UPDATE ────────────────────────────────────────────────────────

    @Test
    fun `parse STATUS_UPDATE with no matching objects returns null`() {
        val json = """
            {
              "actionType": "STATUS_UPDATE",
              "objectType": "Passport",
              "newStatus": "ACTIVE"
            }
        """.trimIndent()

        // Empty objectIndex — no match possible
        val result = parser.parseAction(json, objectIndex = emptyMap())
        assertThat(result).isNull()
    }

    @Test
    fun `parse STATUS_UPDATE resolves single matching object`() {
        val objectIndex = mapOf("obj-passport-1" to Pair("My Passport", "Passport"))
        val json = """
            {
              "actionType": "STATUS_UPDATE",
              "summary": "Marking passport as expired",
              "objectType": "Passport",
              "newStatus": "EXPIRED"
            }
        """.trimIndent()

        val result = parser.parseAction(json, objectIndex = objectIndex)

        assertThat(result).isInstanceOf(AiProposal.StatusUpdate::class.java)
        val proposal = result as AiProposal.StatusUpdate
        assertThat(proposal.objectId).isEqualTo("obj-passport-1")
        assertThat(proposal.newStatus).isEqualTo(ObjectStatus.EXPIRED)
    }

    // ── parseStatus ──────────────────────────────────────────────────────────

    @Test
    fun `parseStatus canonical ACTIVE string returns ACTIVE`() {
        assertThat(parser.parseStatus("ACTIVE")).isEqualTo(ObjectStatus.ACTIVE)
    }

    @Test
    fun `parseStatus expired synonym returns EXPIRED`() {
        // "expired" contains "expire" → EXPIRED
        assertThat(parser.parseStatus("expired")).isEqualTo(ObjectStatus.EXPIRED)
        // "expiry" does NOT contain "expire" — falls through to INACTIVE (this is known behaviour)
        assertThat(parser.parseStatus("expiry soon")).isEqualTo(ObjectStatus.INACTIVE)
    }

    @Test
    fun `parseStatus cancel synonym returns INACTIVE`() {
        assertThat(parser.parseStatus("cancelled")).isEqualTo(ObjectStatus.INACTIVE)
    }

    @Test
    fun `parseStatus renewal synonym returns RENEWAL_DUE`() {
        assertThat(parser.parseStatus("needs renewal")).isEqualTo(ObjectStatus.RENEWAL_DUE)
    }

    @Test
    fun `parseStatus unknown string defaults to INACTIVE`() {
        assertThat(parser.parseStatus("something_completely_unknown_xyz")).isEqualTo(ObjectStatus.INACTIVE)
    }

    // ── METADATA_UPDATE fallback ──────────────────────────────────────────────

    @Test
    fun `parse unknown actionType falls through to METADATA_UPDATE`() {
        val objectIndex = mapOf("obj-1" to Pair("My Car", "Vehicle"))
        val json = """
            {
              "actionType": "SOME_FUTURE_TYPE",
              "objectType": "Vehicle",
              "fields": [{"fieldId": "color", "value": "red"}]
            }
        """.trimIndent()

        val result = parser.parseAction(json, objectIndex = objectIndex)

        // Falls through to parseMetadataUpdate since objectType matches and fields present
        assertThat(result).isInstanceOf(AiProposal.MetadataUpdate::class.java)
    }

    @Test
    fun `parse METADATA_UPDATE with no objects returns null`() {
        val json = """
            {
              "actionType": "METADATA_UPDATE",
              "objectType": "Passport",
              "fields": [{"fieldId": "expiry", "value": "2030-01-01"}]
            }
        """.trimIndent()

        val result = parser.parseAction(json, objectIndex = emptyMap())
        assertThat(result).isNull()
    }

    // ── ACTION_PLAN ───────────────────────────────────────────────────────────

    @Test
    fun `parse ACTION_PLAN with CREATE_PROJECT and CREATE_TASK returns ActionPlan`() {
        val json = """
            {
              "actionType": "ACTION_PLAN",
              "planType": "MARRIAGE",
              "summary": "Wedding plan",
              "items": [
                {"type": "CREATE_PROJECT", "itemId": "0", "summary": "Create wedding project", "title": "Ashutosh Wedding 2027", "emoji": "💒", "domain": "Major Life Events", "dependsOn": []},
                {"type": "CREATE_TASK", "itemId": "1", "summary": "Book venue", "title": "Book wedding venue", "dueDate": "2027-01-15", "priority": "HIGH", "projectItemId": "0", "dependsOn": []},
                {"type": "CREATE_TASK", "itemId": "2", "summary": "Register marriage", "title": "Marriage registration", "priority": "HIGH", "projectItemId": "0", "dependsOn": []}
              ]
            }
        """.trimIndent()

        val result = parser.parseAction(json)

        assertThat(result).isInstanceOf(AiProposal.ActionPlan::class.java)
        val plan = (result as AiProposal.ActionPlan).plan
        assertThat(plan.items).hasSize(3)
    }

    @Test
    fun `parse ACTION_PLAN CREATE_PROJECT item is parsed correctly`() {
        val json = """
            {
              "actionType": "ACTION_PLAN",
              "planType": "MARRIAGE",
              "summary": "Wedding plan",
              "items": [
                {"type": "CREATE_PROJECT", "itemId": "0", "title": "Ashutosh Wedding 2027", "emoji": "💒", "domain": "Major Life Events", "dependsOn": []}
              ]
            }
        """.trimIndent()

        val result = (parser.parseAction(json) as AiProposal.ActionPlan).plan
        val project = result.items.filterIsInstance<ActionItem.CreateProject>().first()

        assertThat(project.title).isEqualTo("Ashutosh Wedding 2027")
        assertThat(project.emoji).isEqualTo("💒")
        assertThat(project.domain).isEqualTo("Major Life Events")
    }

    @Test
    fun `parse ACTION_PLAN CREATE_TASK links to project via projectItemId`() {
        val json = """
            {
              "actionType": "ACTION_PLAN",
              "planType": "MARRIAGE",
              "summary": "Wedding plan",
              "items": [
                {"type": "CREATE_PROJECT", "itemId": "0", "title": "Wedding Project", "emoji": "💒", "dependsOn": []},
                {"type": "CREATE_TASK", "itemId": "1", "title": "Book venue", "priority": "HIGH", "projectItemId": "0", "dependsOn": []}
              ]
            }
        """.trimIndent()

        val result = (parser.parseAction(json) as AiProposal.ActionPlan).plan
        val task = result.items.filterIsInstance<ActionItem.CreateTask>().first()

        assertThat(task.projectItemId).isEqualTo("0")
    }

    @Test
    fun `parse ACTION_PLAN with empty items returns null`() {
        // Guard against parseActionPlan returning null when all items fail to parse.
        val json = """
            {
              "actionType": "ACTION_PLAN",
              "planType": "CUSTOM",
              "summary": "Empty plan",
              "items": []
            }
        """.trimIndent()

        val result = parser.parseAction(json)
        assertThat(result).isNull()
    }

    @Test
    fun `parse ACTION_PLAN unknown planType defaults to CUSTOM`() {
        val json = """
            {
              "actionType": "ACTION_PLAN",
              "planType": "SOMETHING_FUTURE",
              "summary": "Future plan",
              "items": [
                {"type": "CREATE_PROJECT", "itemId": "0", "title": "Future Project", "emoji": "🔮", "dependsOn": []}
              ]
            }
        """.trimIndent()

        val result = (parser.parseAction(json) as AiProposal.ActionPlan).plan
        assertThat(result.type).isEqualTo(com.lifepilot.domain.model.ActionPlanType.CUSTOM)
    }

    @Test
    fun `parse ACTION_PLAN inline single-line JSON returns ActionPlan`() {
        // Regression guard: when the AI returns a single-line ACTION_PLAN block (as shown
        // in the prompt examples), the _italic_ markdown rule in stripMarkdown() was
        // matching from LIFEPILOT_ACTION's underscore to ACTION_PLAN's underscore on the same
        // line, corrupting the opening tag to [LIFEPILOTACTION] and causing the regex to fail.
        // Fix: action blocks are extracted from raw response BEFORE stripMarkdown() is applied.
        // This test verifies the parser itself handles the JSON correctly.
        val singleLineJson = """{"actionType":"ACTION_PLAN","planType":"MARRIAGE","summary":"Wedding plan","items":[{"type":"CREATE_PROJECT","itemId":"0","title":"Wedding","emoji":"💒","dependsOn":[]}]}"""

        val result = parser.parseAction(singleLineJson)
        assertThat(result).isInstanceOf(AiProposal.ActionPlan::class.java)
    }

    // ── proposalId uniqueness ─────────────────────────────────────────────────

    @Test
    fun `each parse call generates a unique proposalId`() {
        val json = """
            {"actionType": "OBJECT_CREATION", "objectType": "Record", "domain": "General", "title": "T", "fields": []}
        """.trimIndent()

        val first = (parser.parseAction(json) as AiProposal.ObjectCreation).proposalId
        val second = (parser.parseAction(json) as AiProposal.ObjectCreation).proposalId
        assertThat(first).isNotEqualTo(second)
    }
}
