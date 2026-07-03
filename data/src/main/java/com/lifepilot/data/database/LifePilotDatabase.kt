package com.lifepilot.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lifepilot.data.database.dao.ConversationDao
import com.lifepilot.data.database.dao.DocumentDao
import com.lifepilot.data.database.dao.DomainLifeStateDao
import com.lifepilot.data.database.dao.EventDao
import com.lifepilot.data.database.dao.GoalDao
import com.lifepilot.data.database.dao.MetadataDao
import com.lifepilot.data.database.dao.ObjectDao
import com.lifepilot.data.database.dao.ProfileDao
import com.lifepilot.data.database.dao.ProjectDao
import com.lifepilot.data.database.dao.RelationshipDao
import com.lifepilot.data.database.dao.ReminderDao
import com.lifepilot.data.database.dao.TaskDao
import com.lifepilot.data.database.dao.TimelineDao
import com.lifepilot.data.database.entity.ChatMessageEntity
import com.lifepilot.data.database.entity.ConversationEntity
import com.lifepilot.data.database.entity.DocumentEntity
import com.lifepilot.data.database.entity.DocumentVersionEntity
import com.lifepilot.data.database.entity.DomainLifeStateEntity
import com.lifepilot.data.database.entity.EventEntity
import com.lifepilot.data.database.entity.GoalEntity
import com.lifepilot.data.database.entity.MetadataEntity
import com.lifepilot.data.database.entity.ObjectEntity
import com.lifepilot.data.database.entity.ProfileEntity
import com.lifepilot.data.database.entity.ProjectEntity
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
        DomainLifeStateEntity::class,
        ProjectEntity::class,
    ],
    version = 6,
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
    abstract fun domainLifeStateDao(): DomainLifeStateDao
    abstract fun projectDao(): ProjectDao

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add verification_status to metadata for full provenance tracking.
                // Default UNVERIFIED preserves existing rows without data loss.
                database.execSQL(
                    "ALTER TABLE metadata ADD COLUMN verification_status TEXT NOT NULL DEFAULT 'UNVERIFIED'"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Introduce domain_life_states table for Domain Life State Architecture.
                // Stores continuously-maintained AI understanding per domain.
                // Composite primary key: (profile_id, domain) — one document per domain per profile.
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS domain_life_states (
                        profile_id TEXT NOT NULL,
                        domain TEXT NOT NULL,
                        current_situation TEXT NOT NULL DEFAULT '',
                        current_priorities TEXT NOT NULL DEFAULT '',
                        known_risks TEXT NOT NULL DEFAULT '',
                        open_questions TEXT NOT NULL DEFAULT '',
                        recommendations TEXT NOT NULL DEFAULT '',
                        recent_changes TEXT NOT NULL DEFAULT '',
                        last_updated INTEGER NOT NULL DEFAULT 0,
                        version INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY (profile_id, domain)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_domain_life_states_profile_id ON domain_life_states(profile_id)"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Introduce Projects — life initiatives that club Objects, Tasks, Goals, and
                // Documents under one umbrella (e.g. "Japan Trip", "Job Change").
                // Nullable project_id FK columns are added to existing tables without defaults
                // so all existing rows retain NULL (no project) and the migration is safe.
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS projects (
                        project_id TEXT NOT NULL PRIMARY KEY,
                        profile_id TEXT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT,
                        domain TEXT,
                        status TEXT NOT NULL,
                        emoji TEXT NOT NULL DEFAULT '🎯',
                        target_date INTEGER,
                        is_ai_proposed INTEGER NOT NULL DEFAULT 0,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        FOREIGN KEY (profile_id) REFERENCES profiles(profile_id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_projects_profile_id ON projects(profile_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_projects_status ON projects(status)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_projects_domain ON projects(domain)")

                // Add project_id FK column to life entity tables.
                database.execSQL("ALTER TABLE objects ADD COLUMN project_id TEXT")
                database.execSQL("ALTER TABLE tasks ADD COLUMN project_id TEXT")
                database.execSQL("ALTER TABLE goals ADD COLUMN project_id TEXT")

                database.execSQL("CREATE INDEX IF NOT EXISTS index_objects_project_id ON objects(project_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_project_id ON tasks(project_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_goals_project_id ON goals(project_id)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add emoji, target_date, and is_ai_proposed to projects table.
                // This migration is a no-op if MIGRATION_4_5 already created the table
                // with these columns (as is the case for fresh installs).
                // For existing users who had version 5 with the old schema, these columns
                // are added here. For fresh installs this migration is still needed to satisfy
                // Room's version chain — SQLite ALTER TABLE ADD COLUMN is idempotent in spirit
                // but not in syntax, so we use a try/catch approach.
                runCatching {
                    database.execSQL("ALTER TABLE projects ADD COLUMN emoji TEXT NOT NULL DEFAULT '🎯'")
                }
                runCatching {
                    database.execSQL("ALTER TABLE projects ADD COLUMN target_date INTEGER")
                }
                runCatching {
                    database.execSQL("ALTER TABLE projects ADD COLUMN is_ai_proposed INTEGER NOT NULL DEFAULT 0")
                }
            }
        }
    }
}
