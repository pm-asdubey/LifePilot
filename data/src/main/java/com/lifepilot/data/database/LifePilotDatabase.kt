package com.lifepilot.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lifepilot.data.database.dao.DocumentDao
import com.lifepilot.data.database.dao.EventDao
import com.lifepilot.data.database.dao.MetadataDao
import com.lifepilot.data.database.dao.ObjectDao
import com.lifepilot.data.database.dao.ProfileDao
import com.lifepilot.data.database.dao.ReminderDao
import com.lifepilot.data.database.dao.TaskDao
import com.lifepilot.data.database.dao.TimelineDao
import com.lifepilot.data.database.entity.DocumentEntity
import com.lifepilot.data.database.entity.DocumentVersionEntity
import com.lifepilot.data.database.entity.EventEntity
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
    ],
    version = 1,
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

    companion object {
        const val DATABASE_NAME = "lifepilot.db"
    }
}
