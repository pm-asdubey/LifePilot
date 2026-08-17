package com.lifepilot.features.library.viewmodel

import com.google.common.truth.Truth.assertThat
import com.lifepilot.domain.engine.SchemaEngine
import com.lifepilot.domain.model.DomainLifeState
import com.lifepilot.domain.model.LifeObject
import com.lifepilot.domain.model.MetadataEntry
import com.lifepilot.domain.model.ObjectStatus
import com.lifepilot.domain.model.Profile
import com.lifepilot.domain.repository.DomainRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.ProfileRepository
import com.lifepilot.domain.usecase.ArchiveObjectUseCase
import com.lifepilot.domain.usecase.DeleteObjectUseCase
import com.lifepilot.features.library.state.LibrarySortOrder
import com.lifepilot.features.library.state.LibraryUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for [LibraryViewModel].
 *
 * Covers: initial state, object/domain-count emission, sort-order transitions,
 * domain filtering, selection-mode management, and the ISSUE-47 regression guard
 * that ensures the `projects` field is absent from [LibraryUiState].
 */
class LibraryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val profileRepository: ProfileRepository = mockk()
    private val objectRepository: ObjectRepository = mockk()
    private val domainRepository: DomainRepository = mockk()
    private val schemaEngine: SchemaEngine = mockk(relaxed = true)
    private val archiveObjectUseCase: ArchiveObjectUseCase = mockk(relaxed = true)
    private val deleteObjectUseCase: DeleteObjectUseCase = mockk(relaxed = true)

    // Controllable backing flows — StateFlow never completes, so the ViewModel
    // keeps observing even after the initial emission (required for sort/filter tests).
    private val fakeProfileFlow = MutableStateFlow<Profile?>(null)
    private val fakeObjectsFlow = MutableStateFlow<List<LifeObject>>(emptyList())
    private val fakeDomainCountsFlow = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val fakeLifeStatesFlow = MutableStateFlow<List<DomainLifeState>>(emptyList())

    private val testProfile = Profile(
        profileId = "profile-1",
        displayName = "Alice",
        avatarPath = null,
        isPrimary = true,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { profileRepository.observeActiveProfile() } returns fakeProfileFlow
        every { objectRepository.observeObjectsByProfile(any()) } returns fakeObjectsFlow
        every { objectRepository.observeObjectCountByDomain(any()) } returns fakeDomainCountsFlow
        every { domainRepository.observeAllDomainLifeStates(any()) } returns fakeLifeStatesFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = LibraryViewModel(
        profileRepository = profileRepository,
        objectRepository = objectRepository,
        domainRepository = domainRepository,
        schemaEngine = schemaEngine,
        archiveObjectUseCase = archiveObjectUseCase,
        deleteObjectUseCase = deleteObjectUseCase,
    )

    // -------------------------------------------------------------------------
    // ISSUE-47 regression guard
    // -------------------------------------------------------------------------

    @Test
    fun `LibraryUiState has no projects field - regression guard for ISSUE-47`() {
        // ISSUE-47: a `projects` field was accidentally added to LibraryUiState and later
        // removed. This test uses Java reflection to assert the field is absent, so a
        // future re-introduction fails at test time rather than at review time.
        val fieldNames = LibraryUiState::class.java.declaredFields.map { it.name }
        assertThat(fieldNames).doesNotContain("projects")
    }

    // -------------------------------------------------------------------------
    // Initial / default state
    // -------------------------------------------------------------------------

    @Test
    fun `default LibraryUiState has isLoading true`() {
        assertThat(LibraryUiState().isLoading).isTrue()
    }

    @Test
    fun `default sort order is UPDATED_RECENT`() {
        assertThat(LibraryUiState().sortOrder).isEqualTo(LibrarySortOrder.UPDATED_RECENT)
    }

    @Test
    fun `when active profile is null, isLoading becomes false`() = runTest {
        fakeProfileFlow.value = null

        val viewModel = createViewModel()

        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }

    // -------------------------------------------------------------------------
    // Objects observation
    // -------------------------------------------------------------------------

    @Test
    fun `when objectRepository emits objects, uiState objects is populated`() = runTest {
        val obj1 = testObject("obj-1", domain = "identity", title = "Passport")
        val obj2 = testObject("obj-2", domain = "finance", title = "Bank Account")
        fakeObjectsFlow.value = listOf(obj1, obj2)
        fakeDomainCountsFlow.value = mapOf("identity" to 1, "finance" to 1)
        fakeProfileFlow.value = testProfile

        val viewModel = createViewModel()

        assertThat(viewModel.uiState.value.objects).hasSize(2)
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }

    @Test
    fun `when objectRepository emits empty list, uiState objects is empty`() = runTest {
        fakeObjectsFlow.value = emptyList()
        fakeDomainCountsFlow.value = emptyMap()
        fakeProfileFlow.value = testProfile

        val viewModel = createViewModel()

        assertThat(viewModel.uiState.value.objects).isEmpty()
    }

    @Test
    fun `when domain counts emitted, uiState domains list is built`() = runTest {
        fakeObjectsFlow.value = emptyList()
        fakeDomainCountsFlow.value = mapOf("identity" to 3, "finance" to 1)
        fakeProfileFlow.value = testProfile

        val viewModel = createViewModel()

        val domains = viewModel.uiState.value.domains
        assertThat(domains).hasSize(2)
        val domainNames = domains.map { it.domain }
        assertThat(domainNames).containsExactly("identity", "finance")
    }

    @Test
    fun `domain items carry correct object counts`() = runTest {
        fakeObjectsFlow.value = emptyList()
        fakeDomainCountsFlow.value = mapOf("employment" to 5)
        fakeProfileFlow.value = testProfile

        val viewModel = createViewModel()

        val domain = viewModel.uiState.value.domains.single()
        assertThat(domain.domain).isEqualTo("employment")
        assertThat(domain.objectCount).isEqualTo(5)
    }

    @Test
    fun `all canonical domains are present even with zero objects - container regression guard`() = runTest {
        // The Library must expose every life domain as a container so documents and life history
        // can be filed into any of them, even before any record exists. SchemaEngine supplies the
        // canonical list; the ViewModel must surface all of them regardless of object counts.
        val canonical = listOf(
            "Career", "Education", "Finance", "Health", "Home", "Identity",
            "Legal", "Major Life Events", "People", "Property", "Transport", "Travel",
        )
        every { schemaEngine.getAllDomains() } returns canonical
        fakeObjectsFlow.value = emptyList()
        fakeDomainCountsFlow.value = emptyMap()
        fakeProfileFlow.value = testProfile

        val viewModel = createViewModel()

        val shownDomains = viewModel.uiState.value.domains.map { it.domain }
        assertThat(shownDomains).containsAtLeastElementsIn(canonical)
        // Every canonical domain shows even though every objectCount is zero.
        assertThat(viewModel.uiState.value.domains.filter { it.domain in canonical })
            .hasSize(canonical.size)
        assertThat(viewModel.uiState.value.domains.all { it.objectCount == 0 }).isTrue()
    }

    // -------------------------------------------------------------------------
    // Sort order
    // -------------------------------------------------------------------------

    @Test
    fun `setSortOrder updates uiState sortOrder`() = runTest {
        fakeObjectsFlow.value = emptyList()
        fakeDomainCountsFlow.value = emptyMap()
        fakeProfileFlow.value = testProfile
        val viewModel = createViewModel()

        viewModel.setSortOrder(LibrarySortOrder.TITLE_ASC)

        assertThat(viewModel.uiState.value.sortOrder).isEqualTo(LibrarySortOrder.TITLE_ASC)
    }

    @Test
    fun `setSortOrder TITLE_ASC sorts objects by title ascending`() = runTest {
        val apple = testObject("obj-1", title = "apple", updatedAt = Instant.EPOCH.plusSeconds(100))
        val banana = testObject("obj-2", title = "Banana", updatedAt = Instant.EPOCH.plusSeconds(200))
        val cherry = testObject("obj-3", title = "cherry", updatedAt = Instant.EPOCH.plusSeconds(50))
        fakeObjectsFlow.value = listOf(banana, cherry, apple)
        fakeDomainCountsFlow.value = mapOf("identity" to 3)
        fakeProfileFlow.value = testProfile
        val viewModel = createViewModel()

        viewModel.setSortOrder(LibrarySortOrder.TITLE_ASC)

        val titles = viewModel.uiState.value.objects.map { it.title }
        assertThat(titles).isInOrder(Comparator.comparing(String::lowercase))
    }

    @Test
    fun `setSortOrder TITLE_DESC sorts objects by title descending`() = runTest {
        val apple = testObject("obj-1", title = "apple")
        val zebra = testObject("obj-2", title = "zebra")
        fakeObjectsFlow.value = listOf(apple, zebra)
        fakeDomainCountsFlow.value = emptyMap()
        fakeProfileFlow.value = testProfile
        val viewModel = createViewModel()

        viewModel.setSortOrder(LibrarySortOrder.TITLE_DESC)

        assertThat(viewModel.uiState.value.objects[0].title).isEqualTo("zebra")
        assertThat(viewModel.uiState.value.objects[1].title).isEqualTo("apple")
    }

    @Test
    fun `setSortOrder UPDATED_RECENT sorts objects by updatedAt descending`() = runTest {
        val older = testObject("obj-1", updatedAt = Instant.EPOCH.plusSeconds(1))
        val newer = testObject("obj-2", updatedAt = Instant.EPOCH.plusSeconds(999))
        fakeObjectsFlow.value = listOf(older, newer)
        fakeDomainCountsFlow.value = emptyMap()
        fakeProfileFlow.value = testProfile
        val viewModel = createViewModel()

        viewModel.setSortOrder(LibrarySortOrder.UPDATED_RECENT)

        assertThat(viewModel.uiState.value.objects[0].objectId).isEqualTo("obj-2")
        assertThat(viewModel.uiState.value.objects[1].objectId).isEqualTo("obj-1")
    }

    // -------------------------------------------------------------------------
    // Domain filtering
    // -------------------------------------------------------------------------

    @Test
    fun `selectDomain filters objects to the chosen domain`() = runTest {
        val finance = testObject("obj-f", domain = "finance", title = "Bank")
        val identity = testObject("obj-i", domain = "identity", title = "Passport")
        fakeObjectsFlow.value = listOf(finance, identity)
        fakeDomainCountsFlow.value = mapOf("finance" to 1, "identity" to 1)
        fakeProfileFlow.value = testProfile
        val viewModel = createViewModel()

        viewModel.selectDomain("finance")

        val objects = viewModel.uiState.value.objects
        assertThat(objects).hasSize(1)
        assertThat(objects[0].objectId).isEqualTo("obj-f")
    }

    @Test
    fun `selectDomain null clears filter and shows all objects`() = runTest {
        val finance = testObject("obj-f", domain = "finance")
        val identity = testObject("obj-i", domain = "identity")
        fakeObjectsFlow.value = listOf(finance, identity)
        fakeDomainCountsFlow.value = mapOf("finance" to 1, "identity" to 1)
        fakeProfileFlow.value = testProfile
        val viewModel = createViewModel()
        viewModel.selectDomain("finance")

        viewModel.selectDomain(null)

        assertThat(viewModel.uiState.value.objects).hasSize(2)
        assertThat(viewModel.uiState.value.selectedDomain).isNull()
    }

    @Test
    fun `selectDomain updates selectedDomain in uiState`() = runTest {
        fakeObjectsFlow.value = emptyList()
        fakeDomainCountsFlow.value = emptyMap()
        fakeProfileFlow.value = testProfile
        val viewModel = createViewModel()

        viewModel.selectDomain("health")

        assertThat(viewModel.uiState.value.selectedDomain).isEqualTo("health")
    }

    // -------------------------------------------------------------------------
    // Selection mode
    // -------------------------------------------------------------------------

    @Test
    fun `enterSelectionMode sets isSelecting true and seeds selectedObjectIds`() = runTest {
        val viewModel = createViewModel()

        viewModel.enterSelectionMode("obj-1")

        assertThat(viewModel.uiState.value.isSelecting).isTrue()
        assertThat(viewModel.uiState.value.selectedObjectIds).containsExactly("obj-1")
    }

    @Test
    fun `toggleObjectSelection adds id when not already selected`() = runTest {
        val viewModel = createViewModel()
        viewModel.enterSelectionMode("obj-1")

        viewModel.toggleObjectSelection("obj-2")

        assertThat(viewModel.uiState.value.selectedObjectIds).containsExactly("obj-1", "obj-2")
    }

    @Test
    fun `toggleObjectSelection removes id when already selected`() = runTest {
        val viewModel = createViewModel()
        viewModel.enterSelectionMode("obj-1")
        viewModel.toggleObjectSelection("obj-2")

        viewModel.toggleObjectSelection("obj-1")

        assertThat(viewModel.uiState.value.selectedObjectIds).containsExactly("obj-2")
    }

    @Test
    fun `toggleObjectSelection exits selection mode when set becomes empty`() = runTest {
        val viewModel = createViewModel()
        viewModel.enterSelectionMode("obj-1")

        viewModel.toggleObjectSelection("obj-1") // deselect the only selected item

        assertThat(viewModel.uiState.value.isSelecting).isFalse()
        assertThat(viewModel.uiState.value.selectedObjectIds).isEmpty()
    }

    @Test
    fun `exitSelectionMode clears isSelecting and selectedObjectIds`() = runTest {
        val viewModel = createViewModel()
        viewModel.enterSelectionMode("obj-1")
        viewModel.toggleObjectSelection("obj-2")

        viewModel.exitSelectionMode()

        assertThat(viewModel.uiState.value.isSelecting).isFalse()
        assertThat(viewModel.uiState.value.selectedObjectIds).isEmpty()
    }

    // -------------------------------------------------------------------------
    // Archive / delete
    // -------------------------------------------------------------------------

    @Test
    fun `archiveSelected invokes archiveObjectUseCase for each selected id`() = runTest {
        coEvery { archiveObjectUseCase(any()) } returns Result.success(Unit)
        val viewModel = createViewModel()
        viewModel.enterSelectionMode("obj-1")
        viewModel.toggleObjectSelection("obj-2")

        viewModel.archiveSelected()

        coVerify { archiveObjectUseCase("obj-1") }
        coVerify { archiveObjectUseCase("obj-2") }
    }

    @Test
    fun `archiveSelected exits selection mode before archiving`() = runTest {
        coEvery { archiveObjectUseCase(any()) } returns Result.success(Unit)
        val viewModel = createViewModel()
        viewModel.enterSelectionMode("obj-1")

        viewModel.archiveSelected()

        // Selection mode should be cleared regardless of use-case outcome.
        assertThat(viewModel.uiState.value.isSelecting).isFalse()
        assertThat(viewModel.uiState.value.selectedObjectIds).isEmpty()
    }

    @Test
    fun `deleteSelected invokes deleteObjectUseCase for each selected id`() = runTest {
        coEvery { deleteObjectUseCase(any()) } returns Result.success(Unit)
        val viewModel = createViewModel()
        viewModel.enterSelectionMode("obj-a")
        viewModel.toggleObjectSelection("obj-b")

        viewModel.deleteSelected()

        coVerify { deleteObjectUseCase("obj-a") }
        coVerify { deleteObjectUseCase("obj-b") }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun testObject(
        id: String,
        domain: String = "identity",
        title: String = "Test Object",
        status: ObjectStatus = ObjectStatus.ACTIVE,
        updatedAt: Instant = Instant.EPOCH,
    ) = LifeObject(
        objectId = id,
        profileId = "profile-1",
        objectType = "generic",
        domain = domain,
        title = title,
        description = null,
        status = status,
        metadata = emptyList<MetadataEntry>(),
        archived = false,
        deleted = false,
        createdAt = Instant.EPOCH,
        updatedAt = updatedAt,
    )
}
