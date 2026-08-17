package com.lifepilot.data.engine

import com.google.common.truth.Truth.assertThat
import com.lifepilot.data.repository.PreferenceManager
import com.lifepilot.domain.engine.DomainLifeStateEngine
import com.lifepilot.domain.engine.PlanningEngine
import com.lifepilot.domain.model.ActionItem
import com.lifepilot.domain.model.ActionPlan
import com.lifepilot.domain.model.ActionPlanType
import com.lifepilot.domain.model.LifeObject
import com.lifepilot.domain.model.ObjectStatus
import com.lifepilot.domain.model.Project
import com.lifepilot.domain.model.ProjectStatus
import com.lifepilot.domain.model.Task
import com.lifepilot.domain.model.TaskPriority
import com.lifepilot.domain.model.TaskSource
import com.lifepilot.domain.model.TaskStatus
import com.lifepilot.domain.repository.MetadataRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.ProjectRepository
import com.lifepilot.domain.usecase.UpdateObjectStatusUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for [ActionPlanExecutorImpl].
 *
 * CRITICAL INVARIANT documented here:
 * The executor must NEVER silently skip CREATE_PROJECT or CREATE_TASK items.
 * Every item in the plan that is enabled and checked must produce exactly one repository call.
 * This invariant is asserted by [execute_createProject_and_createTask_bothRepositoryCallsMade].
 * Removing that test opens a regression path where AI-approved plans are partially discarded
 * without any error surfacing in the UI.
 */
class ActionPlanExecutorImplTest {

    private val preferenceManager = mockk<PreferenceManager>(relaxed = true)
    private val planningEngine = mockk<PlanningEngine>(relaxed = true)
    private val objectRepository = mockk<ObjectRepository>(relaxed = true)
    private val metadataRepository = mockk<MetadataRepository>(relaxed = true)
    private val projectRepository = mockk<ProjectRepository>(relaxed = true)
    private val updateObjectStatusUseCase = mockk<UpdateObjectStatusUseCase>(relaxed = true)
    private val domainLifeStateEngine = mockk<DomainLifeStateEngine>(relaxed = true)
    private val lifeStateEngine = mockk<com.lifepilot.domain.engine.LifeStateEngine>(relaxed = true)

    private lateinit var executor: ActionPlanExecutorImpl

    private val testProfileId = "profile-1"

    private val testProject = Project(
        projectId = "project-1",
        profileId = testProfileId,
        title = "New Job",
        description = null,
        domain = "Career",
        status = ProjectStatus.ACTIVE,
        emoji = "🎯",
        targetDate = null,
        isAiProposed = true,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    private val testTask = Task(
        taskId = "task-1",
        goalId = null,
        objectId = null,
        projectId = "project-1",
        title = "Update LinkedIn",
        description = null,
        priority = TaskPriority.MEDIUM,
        dueDate = null,
        status = TaskStatus.PENDING,
        source = TaskSource.AI_PROPOSED,
        completedAt = null,
    )

    private val testObject = LifeObject(
        objectId = "obj-1",
        profileId = testProfileId,
        objectType = "Job",
        domain = "Career",
        title = "Software Engineer at Acme",
        description = null,
        status = ObjectStatus.ACTIVE,
        metadata = emptyList(),
        archived = false,
        deleted = false,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    @Before
    fun setUp() {
        executor = ActionPlanExecutorImpl(
            preferenceManager = preferenceManager,
            planningEngine = planningEngine,
            objectRepository = objectRepository,
            metadataRepository = metadataRepository,
            projectRepository = projectRepository,
            updateObjectStatusUseCase = updateObjectStatusUseCase,
            domainLifeStateEngine = domainLifeStateEngine,
            lifeStateEngine = lifeStateEngine,
        )
        coEvery { preferenceManager.getActiveProfileId() } returns testProfileId
        coEvery {
            projectRepository.createProject(any(), any(), any(), any(), any(), any(), any())
        } returns testProject
        coEvery {
            planningEngine.createTask(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns Result.success(testTask)
        coEvery {
            objectRepository.createObject(any(), any(), any(), any(), any())
        } returns testObject
        coEvery { updateObjectStatusUseCase(any(), any()) } returns Result.success(Unit)
    }

    // ---------------------------------------------------------------------------
    // CREATE_PROJECT
    // ---------------------------------------------------------------------------

    @Test
    fun `execute CREATE_PROJECT calls projectRepository createProject with correct title and domain`() = runTest {
        val item = ActionItem.CreateProject(
            itemId = "item-proj-1",
            summary = "Create new job project",
            title = "New Job",
            description = "Career transition project",
            domain = "Career",
        )

        val result = executor.execute(makePlan(listOf(item)))

        assertThat(result.isSuccess).isTrue()
        coVerify {
            projectRepository.createProject(
                profileId = testProfileId,
                title = "New Job",
                description = "Career transition project",
                domain = "Career",
                emoji = any(),
                targetDate = null,
                isAiProposed = true,
            )
        }
    }

    @Test
    fun `execute CREATE_PROJECT passes through targetDate and derives emoji from domain`() = runTest {
        val target = java.time.LocalDate.of(2027, 6, 1)
        val item = ActionItem.CreateProject(
            itemId = "item-proj-2",
            summary = "Wedding project",
            title = "Wedding",
            domain = "Major Life Events",
            emoji = com.lifepilot.domain.model.DomainEmoji.DEFAULT, // generic → replaced by domain emoji
            targetDate = target,
        )

        executor.execute(makePlan(listOf(item)))

        coVerify {
            projectRepository.createProject(
                profileId = testProfileId,
                title = "Wedding",
                description = any(),
                domain = "Major Life Events",
                emoji = com.lifepilot.domain.model.DomainEmoji.forDomain("Major Life Events"),
                targetDate = target,
                isAiProposed = true,
            )
        }
    }

    // ---------------------------------------------------------------------------
    // CREATE_PROJECT + CREATE_TASK — critical invariant
    // ---------------------------------------------------------------------------

    /**
     * CRITICAL INVARIANT TEST.
     * Both CREATE_PROJECT and CREATE_TASK items must each produce exactly one repository call.
     * If either call is silently skipped the plan is only partially executed, losing user-approved
     * work without any visible error. This test must not be removed.
     */
    @Test
    fun `execute_createProject_and_createTask_bothRepositoryCallsMade`() = runTest {
        val createProjectItem = ActionItem.CreateProject(
            itemId = "item-proj-1",
            summary = "Create new job project",
            title = "New Job",
            domain = "Career",
        )
        val createTaskItem = ActionItem.CreateTask(
            itemId = "item-task-1",
            summary = "Update LinkedIn profile",
            title = "Update LinkedIn",
            projectItemId = "item-proj-1",
            dependsOn = listOf("item-proj-1"),
        )

        val result = executor.execute(makePlan(listOf(createProjectItem, createTaskItem)))

        assertThat(result.isSuccess).isTrue()
        // Both items must produce a repository call — neither may be silently skipped.
        coVerify(exactly = 1) {
            projectRepository.createProject(any(), eq("New Job"), any(), eq("Career"), any(), any(), any())
        }
        coVerify(exactly = 1) {
            planningEngine.createTask(any(), eq("Update LinkedIn"), any(), any(), any(), any(), any(), any(), any())
        }
    }

    // ---------------------------------------------------------------------------
    // Dependency resolution: projectId is wired from CREATE_PROJECT to CREATE_TASK
    // ---------------------------------------------------------------------------

    @Test
    fun `execute CREATE_TASK with projectItemId resolves projectId from sibling CREATE_PROJECT result`() = runTest {
        val resolvedProjectId = "project-resolved-99"
        val resolvedProject = testProject.copy(projectId = resolvedProjectId)
        coEvery {
            projectRepository.createProject(any(), any(), any(), any(), any(), any(), any())
        } returns resolvedProject

        val projectItemId = "item-proj-ref"
        val createProjectItem = ActionItem.CreateProject(
            itemId = projectItemId,
            summary = "Create project",
            title = "New Job",
            domain = "Career",
        )
        val createTaskItem = ActionItem.CreateTask(
            itemId = "item-task-1",
            summary = "Create task",
            title = "Update CV",
            projectItemId = projectItemId,
            dependsOn = listOf(projectItemId),
        )

        val capturedProjectId = slot<String?>()
        coEvery {
            planningEngine.createTask(
                any(), any(), any(), any(), any(), any(),
                captureNullable(capturedProjectId),
                any(), any(),
            )
        } returns Result.success(testTask)

        executor.execute(makePlan(listOf(createProjectItem, createTaskItem)))

        assertThat(capturedProjectId.captured).isEqualTo(resolvedProjectId)
    }

    // ---------------------------------------------------------------------------
    // CREATE_RECORD
    // ---------------------------------------------------------------------------

    @Test
    fun `execute CREATE_RECORD calls objectRepository createObject with correct objectType domain and title`() = runTest {
        val item = ActionItem.CreateRecord(
            itemId = "item-rec-1",
            summary = "Create job record",
            objectType = "Job",
            domain = "Career",
            title = "Software Engineer at Acme",
            initialNotes = "Starting 1 August",
        )

        val result = executor.execute(makePlan(listOf(item)))

        assertThat(result.isSuccess).isTrue()
        coVerify {
            objectRepository.createObject(
                profileId = testProfileId,
                objectType = "Job",
                domain = "Career",
                title = "Software Engineer at Acme",
                description = "Starting 1 August",
            )
        }
    }

    // ---------------------------------------------------------------------------
    // UPDATE_STATUS
    // ---------------------------------------------------------------------------

    @Test
    fun `execute UPDATE_STATUS calls updateObjectStatusUseCase with correct objectId and newStatus`() = runTest {
        val item = ActionItem.UpdateStatus(
            itemId = "item-status-1",
            summary = "Mark old job as inactive",
            objectId = "obj-old-job",
            objectTitle = "Old Company",
            newStatus = ObjectStatus.INACTIVE,
        )

        val result = executor.execute(makePlan(listOf(item)))

        assertThat(result.isSuccess).isTrue()
        coVerify { updateObjectStatusUseCase("obj-old-job", ObjectStatus.INACTIVE) }
    }

    // ---------------------------------------------------------------------------
    // Dependency ordering: all dependent items must still execute
    // ---------------------------------------------------------------------------

    @Test
    fun `execute items with dependsOn processes all items without skipping any`() = runTest {
        val projectItem = ActionItem.CreateProject(
            itemId = "proj-a",
            summary = "Project A",
            title = "Project Alpha",
            domain = "Finance",
        )
        val recordItem = ActionItem.CreateRecord(
            itemId = "rec-b",
            summary = "Record B",
            objectType = "BankAccount",
            domain = "Finance",
            title = "New Account",
            dependsOn = listOf("proj-a"),
        )

        val result = executor.execute(makePlan(listOf(projectItem, recordItem)))

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) {
            projectRepository.createProject(any(), eq("Project Alpha"), any(), eq("Finance"), any(), any(), any())
        }
        coVerify(exactly = 1) {
            objectRepository.createObject(any(), eq("BankAccount"), eq("Finance"), eq("New Account"), any())
        }
    }

    // ---------------------------------------------------------------------------
    // Empty items list
    // ---------------------------------------------------------------------------

    @Test
    fun `execute with empty items list makes no repository calls and returns success`() = runTest {
        val result = executor.execute(makePlan(emptyList()))

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 0) {
            projectRepository.createProject(any(), any(), any(), any(), any(), any(), any())
        }
        coVerify(exactly = 0) {
            objectRepository.createObject(any(), any(), any(), any(), any())
        }
        coVerify(exactly = 0) {
            planningEngine.createTask(any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
        coVerify(exactly = 0) { updateObjectStatusUseCase(any(), any()) }
    }

    // ---------------------------------------------------------------------------
    // No active profile
    // ---------------------------------------------------------------------------

    @Test
    fun `execute returns failure and makes no repository calls when no active profile is set`() = runTest {
        coEvery { preferenceManager.getActiveProfileId() } returns null
        val plan = makePlan(
            listOf(
                ActionItem.CreateProject(
                    itemId = "proj-1",
                    summary = "Should not run",
                    title = "Title",
                )
            )
        )

        val result = executor.execute(plan)

        assertThat(result.isFailure).isTrue()
        coVerify(exactly = 0) {
            projectRepository.createProject(any(), any(), any(), any(), any(), any(), any())
        }
    }

    // ---------------------------------------------------------------------------
    // Empty planId handled gracefully
    // ---------------------------------------------------------------------------

    @Test
    fun `execute with empty planId does not crash and processes enabled items normally`() = runTest {
        val plan = ActionPlan(
            planId = "",
            type = ActionPlanType.CUSTOM,
            summary = "Test plan with empty planId",
            items = listOf(
                ActionItem.CreateProject(
                    itemId = "proj-1",
                    summary = "Create project",
                    title = "My Project",
                )
            ),
        )

        val result = executor.execute(plan)

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) {
            projectRepository.createProject(any(), eq("My Project"), any(), any(), any(), any(), any())
        }
    }

    // ---------------------------------------------------------------------------
    // Disabled / unchecked items are skipped
    // ---------------------------------------------------------------------------

    @Test
    fun `execute skips disabled items and returns success`() = runTest {
        val disabledItem = ActionItem.CreateProject(
            itemId = "proj-disabled",
            summary = "Should not execute",
            title = "Disabled Project",
            isEnabled = false,
        )

        val result = executor.execute(makePlan(listOf(disabledItem)))

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 0) {
            projectRepository.createProject(any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `execute skips unchecked items and returns success`() = runTest {
        val uncheckedItem = ActionItem.CreateProject(
            itemId = "proj-unchecked",
            summary = "Should not execute",
            title = "Unchecked Project",
            isChecked = false,
        )

        val result = executor.execute(makePlan(listOf(uncheckedItem)))

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 0) {
            projectRepository.createProject(any(), any(), any(), any(), any(), any(), any())
        }
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun makePlan(items: List<ActionItem>) = ActionPlan(
        planId = "plan-test-1",
        type = ActionPlanType.JOB_CHANGE,
        summary = "Test action plan",
        items = items,
    )
}
