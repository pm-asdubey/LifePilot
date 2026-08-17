package com.lifepilot.data.engine

import com.google.common.truth.Truth.assertThat
import com.lifepilot.domain.engine.ObjectReasoner
import com.lifepilot.domain.model.LifeObject
import com.lifepilot.domain.model.ObjectSnapshot
import com.lifepilot.domain.model.ObjectStatus
import com.lifepilot.domain.model.Profile
import com.lifepilot.domain.model.Task
import com.lifepilot.domain.model.TaskPriority
import com.lifepilot.domain.model.TaskSource
import com.lifepilot.domain.model.TaskStatus
import com.lifepilot.domain.repository.DomainRepository
import com.lifepilot.domain.repository.MetadataRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.ProfileRepository
import com.lifepilot.domain.repository.ProjectRepository
import com.lifepilot.domain.repository.ReminderRepository
import com.lifepilot.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for [RetrievalEngineImpl].
 *
 * Verifies that [RetrievalEngineImpl.retrieve] assembles a correct [RetrievalContext] from its
 * repository dependencies. Each test stubs only the collaborators relevant to the assertion being
 * made; all other collaborators default to relaxed mocks that return empty collections.
 */
class RetrievalEngineImplTest {

    private val profileRepository = mockk<ProfileRepository>(relaxed = true)
    private val objectRepository = mockk<ObjectRepository>(relaxed = true)
    private val metadataRepository = mockk<MetadataRepository>(relaxed = true)
    private val taskRepository = mockk<TaskRepository>(relaxed = true)
    private val reminderRepository = mockk<ReminderRepository>(relaxed = true)
    private val objectReasoner = mockk<ObjectReasoner>(relaxed = true)
    private val domainRepository = mockk<DomainRepository>(relaxed = true)
    private val projectRepository = mockk<ProjectRepository>(relaxed = true)

    private lateinit var engine: RetrievalEngineImpl

    private val testProfileId = "profile-1"

    private val testProfile = Profile(
        profileId = testProfileId,
        displayName = "Test User",
        avatarPath = null,
        isPrimary = true,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    private val careerObject = LifeObject(
        objectId = "obj-job-1",
        profileId = testProfileId,
        objectType = "Job",
        domain = "Career",
        title = "Software Engineer",
        description = null,
        status = ObjectStatus.ACTIVE,
        metadata = emptyList(),
        archived = false,
        deleted = false,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    private val identityObject = LifeObject(
        objectId = "obj-passport-1",
        profileId = testProfileId,
        objectType = "Passport",
        domain = "Identity",
        title = "Indian Passport",
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
        engine = RetrievalEngineImpl(
            profileRepository = profileRepository,
            objectRepository = objectRepository,
            metadataRepository = metadataRepository,
            taskRepository = taskRepository,
            reminderRepository = reminderRepository,
            objectReasoner = objectReasoner,
            domainRepository = domainRepository,
            projectRepository = projectRepository,
        )
        // Safe defaults: active profile, no objects, no tasks, no reminders, no projects.
        every { profileRepository.observeActiveProfile() } returns flowOf(testProfile)
        every { objectRepository.observeObjectsByProfile(any()) } returns flowOf(emptyList())
        coEvery { metadataRepository.getMetadataForObjects(any()) } returns emptyMap()
        every { domainRepository.observeAllDomainLifeStates(any()) } returns flowOf(emptyList())
        every { taskRepository.observePendingTasks(any()) } returns flowOf(emptyList())
        every { reminderRepository.observeUpcomingReminders(any()) } returns flowOf(emptyList())
        coEvery { projectRepository.getActiveProjects(any()) } returns emptyList()
    }

    // ---------------------------------------------------------------------------
    // Basic contract: non-null result with correct profile fields
    // ---------------------------------------------------------------------------

    @Test
    fun `retrieve returns non-null RetrievalContext`() = runTest {
        val context = engine.retrieve(testProfileId, "What is my passport status?")

        assertThat(context).isNotNull()
    }

    @Test
    fun `retrieve populates profileId from active profile`() = runTest {
        val context = engine.retrieve(testProfileId, "")

        assertThat(context.profileId).isEqualTo(testProfileId)
    }

    @Test
    fun `retrieve populates profileName from active profile display name`() = runTest {
        val context = engine.retrieve(testProfileId, "")

        assertThat(context.profileName).isEqualTo("Test User")
    }

    // ---------------------------------------------------------------------------
    // allObjectIndex — objectId → (title, objectType)
    // ---------------------------------------------------------------------------

    @Test
    fun `retrieve allObjectIndex contains one entry per object in the profile`() = runTest {
        every { objectRepository.observeObjectsByProfile(testProfileId) } returns flowOf(
            listOf(careerObject, identityObject)
        )

        val context = engine.retrieve(testProfileId, "")

        assertThat(context.allObjectIndex).hasSize(2)
        assertThat(context.allObjectIndex).containsKey("obj-job-1")
        assertThat(context.allObjectIndex).containsKey("obj-passport-1")
    }

    @Test
    fun `retrieve allObjectIndex maps objectId to correct title and objectType`() = runTest {
        every { objectRepository.observeObjectsByProfile(testProfileId) } returns flowOf(
            listOf(careerObject)
        )

        val context = engine.retrieve(testProfileId, "")

        val entry = context.allObjectIndex["obj-job-1"]
        assertThat(entry).isNotNull()
        assertThat(entry!!.first).isEqualTo("Software Engineer")
        assertThat(entry.second).isEqualTo("Job")
    }

    // ---------------------------------------------------------------------------
    // allObjectDomainIndex — objectId → domain
    // ---------------------------------------------------------------------------

    @Test
    fun `retrieve allObjectDomainIndex maps each objectId to its domain`() = runTest {
        every { objectRepository.observeObjectsByProfile(testProfileId) } returns flowOf(
            listOf(careerObject, identityObject)
        )

        val context = engine.retrieve(testProfileId, "")

        assertThat(context.allObjectDomainIndex["obj-job-1"]).isEqualTo("Career")
        assertThat(context.allObjectDomainIndex["obj-passport-1"]).isEqualTo("Identity")
    }

    // ---------------------------------------------------------------------------
    // Empty object list
    // ---------------------------------------------------------------------------

    @Test
    fun `retrieve with empty object list produces empty allObjectIndex and allObjectDomainIndex`() = runTest {
        every { objectRepository.observeObjectsByProfile(any()) } returns flowOf(emptyList())

        val context = engine.retrieve(testProfileId, "")

        assertThat(context.allObjectIndex).isEmpty()
        assertThat(context.allObjectDomainIndex).isEmpty()
    }

    @Test
    fun `retrieve with empty object list reports totalObjectCount of zero`() = runTest {
        every { objectRepository.observeObjectsByProfile(any()) } returns flowOf(emptyList())

        val context = engine.retrieve(testProfileId, "")

        assertThat(context.totalObjectCount).isEqualTo(0)
    }

    @Test
    fun `retrieve with empty object list produces empty relevantSnapshots`() = runTest {
        every { objectRepository.observeObjectsByProfile(any()) } returns flowOf(emptyList())

        val context = engine.retrieve(testProfileId, "")

        assertThat(context.relevantSnapshots).isEmpty()
    }

    // ---------------------------------------------------------------------------
    // totalObjectCount
    // ---------------------------------------------------------------------------

    @Test
    fun `retrieve totalObjectCount reflects the number of objects loaded for the profile`() = runTest {
        every { objectRepository.observeObjectsByProfile(testProfileId) } returns flowOf(
            listOf(careerObject, identityObject)
        )

        val context = engine.retrieve(testProfileId, "")

        assertThat(context.totalObjectCount).isEqualTo(2)
    }

    // ---------------------------------------------------------------------------
    // relevantSnapshots — scored selection from objects
    // ---------------------------------------------------------------------------

    @Test
    fun `retrieve relevantSnapshots contains snapshot for object whose type matches the query`() = runTest {
        val snapshot = ObjectSnapshot(
            objectId = "obj-job-1",
            title = "Software Engineer",
            objectType = "Job",
            domain = "Career",
            status = ObjectStatus.ACTIVE,
            metadata = emptyList(),
            aiContext = null,
            pendingTaskCount = 0,
            documentCount = 0,
            relevanceScore = 0f,
        )
        every { objectRepository.observeObjectsByProfile(testProfileId) } returns flowOf(listOf(careerObject))
        coEvery { objectReasoner.buildSnapshot(testProfileId, "obj-job-1") } returns snapshot

        // "job" in query → matches careerObject.objectType "Job"
        val context = engine.retrieve(testProfileId, "What is my current job?")

        assertThat(context.relevantSnapshots).hasSize(1)
        assertThat(context.relevantSnapshots[0].objectId).isEqualTo("obj-job-1")
        assertThat(context.relevantSnapshots[0].domain).isEqualTo("Career")
    }

    @Test
    fun `retrieve relevantSnapshots reflects domain of the retrieved objects`() = runTest {
        val careerSnapshot = ObjectSnapshot(
            objectId = "obj-job-1",
            title = "Software Engineer",
            objectType = "Job",
            domain = "Career",
            status = ObjectStatus.ACTIVE,
            metadata = emptyList(),
            aiContext = null,
            pendingTaskCount = 0,
            documentCount = 0,
        )
        val identitySnapshot = ObjectSnapshot(
            objectId = "obj-passport-1",
            title = "Indian Passport",
            objectType = "Passport",
            domain = "Identity",
            status = ObjectStatus.ACTIVE,
            metadata = emptyList(),
            aiContext = null,
            pendingTaskCount = 0,
            documentCount = 0,
        )
        every { objectRepository.observeObjectsByProfile(testProfileId) } returns flowOf(
            listOf(careerObject, identityObject)
        )
        coEvery { objectReasoner.buildSnapshot(testProfileId, "obj-job-1") } returns careerSnapshot
        coEvery { objectReasoner.buildSnapshot(testProfileId, "obj-passport-1") } returns identitySnapshot

        // Query tokens match both objects (no score filter, falls back to top-N)
        val context = engine.retrieve(testProfileId, "")

        // Both objects should appear since empty query falls back to top-N with score 0
        val domains = context.relevantSnapshots.map { it.domain }
        assertThat(domains).containsAtLeast("Career", "Identity")
    }

    // ---------------------------------------------------------------------------
    // pendingTasks
    // ---------------------------------------------------------------------------

    @Test
    fun `retrieve pendingTasks are populated from the task repository`() = runTest {
        val pendingTask = Task(
            taskId = "task-pending-1",
            goalId = null,
            objectId = null,
            projectId = null,
            title = "Renew passport",
            description = null,
            priority = TaskPriority.HIGH,
            dueDate = null,
            status = TaskStatus.PENDING,
            source = TaskSource.SYSTEM,
            completedAt = null,
        )
        every { taskRepository.observePendingTasks(any()) } returns flowOf(listOf(pendingTask))

        val context = engine.retrieve(testProfileId, "")

        assertThat(context.pendingTasks).hasSize(1)
        assertThat(context.pendingTasks[0].taskId).isEqualTo("task-pending-1")
    }

    // ---------------------------------------------------------------------------
    // Null active profile fallback
    // ---------------------------------------------------------------------------

    @Test
    fun `retrieve falls back to the passed profileId when active profile returns null`() = runTest {
        every { profileRepository.observeActiveProfile() } returns flowOf(null)
        every { objectRepository.observeObjectsByProfile(testProfileId) } returns flowOf(emptyList())

        val context = engine.retrieve(testProfileId, "")

        assertThat(context.profileId).isEqualTo(testProfileId)
        assertThat(context.profileName).isNull()
    }
}
