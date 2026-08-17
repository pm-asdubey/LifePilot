package com.lifepilot.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifepilot.data.database.dao.ConversationDao
import com.lifepilot.data.database.entity.ChatMessageEntity
import com.lifepilot.data.database.entity.ConversationEntity
import com.lifepilot.data.database.entity.ProfileEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversationDaoTest {

    private lateinit var db: LifePilotDatabase
    private lateinit var dao: ConversationDao

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LifePilotDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.conversationDao()
        db.profileDao().insertProfile(testProfile())
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── Save and retrieve ─────────────────────────────────────────────────────

    @Test
    fun insertMessage_thenGetMessages_returnsMessage() = runTest {
        dao.upsertConversation(testConversation("conv-1"))
        dao.insertMessage(testMessage("msg-1", "conv-1", "USER", 1000L))

        val messages = dao.getMessagesByConversation("conv-1")
        assertEquals(1, messages.size)
        assertEquals("msg-1", messages[0].messageId)
        assertEquals("USER", messages[0].role)
    }

    @Test
    fun insertUserThenAssistantMessage_userComesFirst() = runTest {
        dao.upsertConversation(testConversation("conv-2"))
        dao.insertMessage(testMessage("msg-user", "conv-2", "USER", 1000L))
        dao.insertMessage(testMessage("msg-assistant", "conv-2", "ASSISTANT", 2000L))

        val messages = dao.getMessagesByConversation("conv-2")
        assertEquals(2, messages.size)
        assertEquals("USER", messages[0].role)
        assertEquals("ASSISTANT", messages[1].role)
    }

    @Test
    fun getMessages_unknownConversationId_returnsEmptyList() = runTest {
        val messages = dao.getMessagesByConversation("does-not-exist")
        assertTrue(messages.isEmpty())
    }

    // ── CASCADE DELETE guard ──────────────────────────────────────────────────

    /**
     * CRITICAL REGRESSION TEST — guards against the CASCADE DELETE bug.
     *
     * ConversationDao.updateTitle() uses a targeted SQL UPDATE (not INSERT OR REPLACE),
     * so it must NEVER delete existing messages. If this test ever fails, it means
     * someone changed the DAO back to upsertConversation() for title updates.
     */
    @Test
    fun updateTitle_doesNotDeleteExistingMessages() = runTest {
        dao.upsertConversation(testConversation("conv-3", title = "New conversation"))
        dao.insertMessage(testMessage("msg-first", "conv-3", "USER", 1000L))
        dao.insertMessage(testMessage("msg-second", "conv-3", "ASSISTANT", 2000L))

        // updateTitle uses targeted UPDATE — messages must survive
        dao.updateTitle("conv-3", "Canada Trip", System.currentTimeMillis())

        val messages = dao.getMessagesByConversation("conv-3")
        assertEquals(2, messages.size)
        assertEquals("msg-first", messages[0].messageId)
        assertEquals("msg-second", messages[1].messageId)

        // Verify the title actually changed
        val conv = dao.getConversationById("conv-3")
        assertEquals("Canada Trip", conv?.title)
    }

    /**
     * Documents the DANGEROUS behaviour of upsertConversation (INSERT OR REPLACE).
     *
     * SQLite's INSERT OR REPLACE deletes the old row first, triggering the CASCADE
     * on chat_messages. If this test ever PASSES (messages survive), the FK strategy
     * has changed and the workarounds in updateTitle/touchConversation may be removable.
     */
    @Test
    fun upsertConversation_afterMessagesExist_deletesAllMessages() = runTest {
        dao.upsertConversation(testConversation("conv-4", title = "Original"))
        dao.insertMessage(testMessage("msg-a", "conv-4", "USER", 1000L))
        dao.insertMessage(testMessage("msg-b", "conv-4", "ASSISTANT", 2000L))

        // Calling upsertConversation with same PK triggers INSERT OR REPLACE → CASCADE DELETE
        dao.upsertConversation(testConversation("conv-4", title = "Updated title"))

        // Messages are GONE — this is the bug we fixed by using updateTitle() instead
        val messages = dao.getMessagesByConversation("conv-4")
        assertTrue(
            "upsertConversation must delete messages via CASCADE (this documents the dangerous behaviour)",
            messages.isEmpty()
        )
    }

    // ── Delete cascade ────────────────────────────────────────────────────────

    @Test
    fun deleteConversation_cascadesToMessages() = runTest {
        dao.upsertConversation(testConversation("conv-5"))
        dao.insertMessage(testMessage("msg-x", "conv-5", "USER", 1000L))
        dao.insertMessage(testMessage("msg-y", "conv-5", "ASSISTANT", 2000L))

        dao.deleteConversation("conv-5")

        val messages = dao.getMessagesByConversation("conv-5")
        assertTrue(messages.isEmpty())
        assertNull(dao.getConversationById("conv-5"))
    }

    // ── Observe flow ──────────────────────────────────────────────────────────

    @Test
    fun observeMessages_emitsInsertedMessages() = runTest {
        dao.upsertConversation(testConversation("conv-6"))
        dao.insertMessage(testMessage("msg-obs", "conv-6", "USER", 1000L))

        val messages = dao.observeMessagesByConversation("conv-6").first()
        assertEquals(1, messages.size)
        assertEquals("msg-obs", messages[0].messageId)
    }

    @Test
    fun touchConversation_doesNotDeleteMessages() = runTest {
        dao.upsertConversation(testConversation("conv-7"))
        dao.insertMessage(testMessage("msg-touch", "conv-7", "USER", 1000L))

        dao.touchConversation("conv-7", System.currentTimeMillis() + 1000)

        val messages = dao.getMessagesByConversation("conv-7")
        assertEquals(1, messages.size)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun testProfile() = ProfileEntity(
        profileId = "profile-1",
        name = "Test User",
        createdAt = 0L,
        updatedAt = 0L,
    )

    private fun testConversation(id: String, title: String = "Test conversation") = ConversationEntity(
        conversationId = id,
        profileId = "profile-1",
        title = title,
        createdAt = 1000L,
        updatedAt = 1000L,
    )

    private fun testMessage(
        messageId: String,
        conversationId: String,
        role: String,
        timestamp: Long,
    ) = ChatMessageEntity(
        messageId = messageId,
        conversationId = conversationId,
        role = role,
        content = "Test message content",
        timestamp = timestamp,
    )
}
