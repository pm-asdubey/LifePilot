package com.lifepilot.data.di

import android.content.Context
import androidx.room.Room
import com.lifepilot.data.database.LifePilotDatabase
import com.lifepilot.data.database.dao.DocumentDao
import com.lifepilot.data.database.dao.EventDao
import com.lifepilot.data.database.dao.MetadataDao
import com.lifepilot.data.database.dao.ObjectDao
import com.lifepilot.data.database.dao.ProfileDao
import com.lifepilot.data.database.dao.ReminderDao
import com.lifepilot.data.database.dao.TaskDao
import com.lifepilot.data.database.dao.TimelineDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LifePilotDatabase =
        Room.databaseBuilder(
            context,
            LifePilotDatabase::class.java,
            LifePilotDatabase.DATABASE_NAME,
        ).build()

    @Provides
    fun provideProfileDao(db: LifePilotDatabase): ProfileDao = db.profileDao()

    @Provides
    fun provideObjectDao(db: LifePilotDatabase): ObjectDao = db.objectDao()

    @Provides
    fun provideMetadataDao(db: LifePilotDatabase): MetadataDao = db.metadataDao()

    @Provides
    fun provideDocumentDao(db: LifePilotDatabase): DocumentDao = db.documentDao()

    @Provides
    fun provideEventDao(db: LifePilotDatabase): EventDao = db.eventDao()

    @Provides
    fun provideTaskDao(db: LifePilotDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideReminderDao(db: LifePilotDatabase): ReminderDao = db.reminderDao()

    @Provides
    fun provideTimelineDao(db: LifePilotDatabase): TimelineDao = db.timelineDao()
}
