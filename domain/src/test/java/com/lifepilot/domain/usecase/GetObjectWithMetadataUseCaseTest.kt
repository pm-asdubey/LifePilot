package com.lifepilot.domain.usecase

import com.lifepilot.domain.model.LifeObject
import com.lifepilot.domain.model.MetadataEntry
import com.lifepilot.domain.model.MetadataFieldType
import com.lifepilot.domain.model.MetadataSource
import com.lifepilot.domain.model.ObjectStatus
import com.lifepilot.domain.repository.MetadataRepository
import com.lifepilot.domain.repository.ObjectRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant

class GetObjectWithMetadataUseCaseTest {

    private val objectRepository: ObjectRepository = mockk()
    private val metadataRepository: MetadataRepository = mockk()

    private lateinit var useCase: GetObjectWithMetadataUseCase

    private val testObject = LifeObject(
        objectId = "obj-1",
        profileId = "profile-1",
        objectType = "passport",
        domain = "Identity",
        title = "My Passport",
        description = null,
        status = ObjectStatus.ACTIVE,
        metadata = emptyList(),
        archived = false,
        deleted = false,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    private val testMetadata = listOf(
        MetadataEntry(
            metadataId = "m1",
            objectId = "obj-1",
            fieldId = "expiry_date",
            fieldType = MetadataFieldType.DATE,
            value = "2030-01-01",
            version = 1,
            confidence = null,
            source = MetadataSource.USER,
            updatedAt = Instant.now(),
        )
    )

    @Before
    fun setUp() {
        useCase = GetObjectWithMetadataUseCase(
            objectRepository = objectRepository,
            metadataRepository = metadataRepository,
        )
    }

    @Test
    fun `invoke returns ObjectWithMetadata when object exists`() = runTest {
        coEvery { objectRepository.getObjectById("obj-1") } returns testObject
        coEvery { metadataRepository.getMetadataByObject("obj-1") } returns testMetadata

        val result = useCase("obj-1")

        assertNotNull(result)
        assertEquals(testObject, result!!.lifeObject)
        assertEquals(testMetadata, result.metadata)
    }

    @Test
    fun `invoke returns null when object does not exist`() = runTest {
        coEvery { objectRepository.getObjectById("missing") } returns null

        val result = useCase("missing")

        assertNull(result)
    }

    @Test
    fun `invoke returns empty metadata list when no metadata exists`() = runTest {
        coEvery { objectRepository.getObjectById("obj-1") } returns testObject
        coEvery { metadataRepository.getMetadataByObject("obj-1") } returns emptyList()

        val result = useCase("obj-1")

        assertNotNull(result)
        assertEquals(emptyList<MetadataEntry>(), result!!.metadata)
    }
}
