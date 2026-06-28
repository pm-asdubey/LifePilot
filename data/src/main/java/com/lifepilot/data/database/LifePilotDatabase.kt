package com.lifepilot.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lifepilot.data.database.dao.ConversationDao
import com.lifepilot.data.database.dao.DocumentDao
import com.lifepilot.data.database.dao.EventDao
import com.lifepilot.data.database.dao.GoalDao
import com.lifepilot.data.database.dao.MetadataDao
import com.lifepilot.data.database.dao.ObjectDao
import com.lifepilot.data.database.dao.ProfileDao
import com.lifepilot.data.database.dao.RelationshipDao
import com.lifepilot.data.database.dao.ReminderDao
import com.lifepilot.data.database.dao.TaskDao
import com.lifepilot.data.database.dao.TimelineDao
import com.lifepilot.data.database.entity.ChatMessageEntity
import com.lifepilot.data.database.entity.ConversationEntity
import com.lifepilot.data.database.entity.DocumentEntity
import com.lifepilot.data.database.entity.DocumentVersionEntity
import com.lifepilot.data.database.entity.EventEntity
import com.lifepilot.data.database.entity.GoalEntity
import com.lifepilot.data.database.entity.MetadataEntity
import com.lifepilot.data.database.entity.ObjectEntity
import com.lifepilot.data.database.entity.ProfileEntity
import com.lifepilot.data.database.entity.RelationshipEntity
import com.lifepilot.data.database.entity.ReminderEntity
import com.lifepilot.data.database.entity.TaskEntity
import com.lifepilot.data.database.entity.TimelineEntity

@Database(
    entities = [
        ProfileEntity::class,
        ObjectEntity::class,
        MetadataEntity::class,
        DocumentEntity::class,
        DocumentVersionEntity::class,
        EventEntity::class,
        TaskEntity::class,
        ReminderEntity::class,
        RelationshipEntity::class,
        TimelineEntity::class,
        GoalEntity::class,
        ConversationEntity::class,
        ChatMessageEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class LifePilotDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun objectDao(): ObjectDao
    abstract fun metadataDao(): MetadataDao
    abstract fun documentDao(): DocumentDao
    abstract fun eventDao(): EventDao
    abstract fun taskDao(): TaskDao
    abstract fun reminderDao(): ReminderDao
    abstract fun timelineDao(): TimelineDao
    abstract fun relationshipDao(): RelationshipDao
    abstract fun goalDao(): GoalDao
    abstract fun conversationDao(): ConversationDao

    companion object {
        const val DATABASE_NAME = "lifepilot.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create goals table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS goals (
                        goal_id TEXT NOT NULL PRIMARY KEY,
                        profile_id TEXT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT,
                        deadline INTEGER,
                        status TEXT NOT NULL,
                        progress INTEGER NOT NULL DEFAULT 0,
                        object_id TEXT,
                        notes TEXT,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_goals_profile_id ON goals(profile_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_goals_status ON goals(status)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_goals_deadline ON goals(deadline)")

                // Create conversations table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS conversations (
                        conversation_id TEXT NOT NULL PRIMARY KEY,
                        profile_id TEXT NOT NULL,
                        title TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_conversations_profile_id ON conversations(profile_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_conversations_updated_at ON conversations(updated_at)")

                // Create chat_messages table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS chat_messages (
                        message_id TEXT NOT NULL PRIMARY KEY,
                        conversation_id TEXT NOT NULL,
                        role TEXT NOT NULL,
                        content TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        FOREIGN KEY (conversation_id) REFERENCES conversations(conversation_id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_conversation_id ON chat_messages(conversation_id)")

                // Add goal_id column to tasks
                database.execSQL("ALTER TABLE tasks ADD COLUMN goal_id TEXT")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_goal_id ON tasks(goal_id)")
            }
        }
    }
}
