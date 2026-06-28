package com.lifepilot.data.di

import com.lifepilot.data.repository.ConversationRepositoryImpl
import com.lifepilot.data.repository.DocumentRepositoryImpl
import com.lifepilot.data.repository.EventRepositoryImpl
import com.lifepilot.data.repository.GoalRepositoryImpl
import com.lifepilot.data.repository.RelationshipRepositoryImpl
import com.lifepilot.data.repository.MetadataRepositoryImpl
import com.lifepilot.data.repository.ObjectRepositoryImpl
import com.lifepilot.data.repository.ProfileRepositoryImpl
import com.lifepilot.data.repository.ReminderRepositoryImpl
import com.lifepilot.data.repository.SearchRepositoryImpl
import com.lifepilot.data.repository.TaskRepositoryImpl
import com.lifepilot.data.repository.TimelineRepositoryImpl
import com.lifepilot.data.engine.LifeStateEngineImpl
import com.lifepilot.data.engine.RuleEngineImpl
import com.lifepilot.data.ocr.MlKitOcrService
import com.lifepilot.data.schema.SchemaEngineImpl
import com.lifepilot.domain.engine.LifeStateEngine
import com.lifepilot.domain.engine.RuleEngine
import com.lifepilot.domain.engine.SchemaEngine
import com.lifepilot.domain.ocr.OcrService
import com.lifepilot.domain.repository.ConversationRepository
import com.lifepilot.domain.repository.DocumentRepository
import com.lifepilot.domain.repository.EventRepository
import com.lifepilot.domain.repository.GoalRepository
import com.lifepilot.domain.repository.RelationshipRepository
import com.lifepilot.domain.repository.MetadataRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.ProfileRepository
import com.lifepilot.domain.repository.ReminderRepository
import com.lifepilot.domain.repository.SearchRepository
import com.lifepilot.domain.repository.TaskRepository
import com.lifepilot.domain.repository.TimelineRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindObjectRepository(impl: ObjectRepositoryImpl): ObjectRepository

    @Binds
    @Singleton
    abstract fun bindMetadataRepository(impl: MetadataRepositoryImpl): MetadataRepository

    @Binds
    @Singleton
    abstract fun bindDocumentRepository(impl: DocumentRepositoryImpl): DocumentRepository

    @Binds
    @Singleton
    abstract fun bindEventRepository(impl: EventRepositoryImpl): EventRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository

    @Binds
    @Singleton
    abstract fun bindTimelineRepository(impl: TimelineRepositoryImpl): TimelineRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository

    @Binds
    @Singleton
    abstract fun bindRelationshipRepository(impl: RelationshipRepositoryImpl): RelationshipRepository

    @Binds
    @Singleton
    abstract fun bindSchemaEngine(impl: SchemaEngineImpl): SchemaEngine

    @Binds
    @Singleton
    abstract fun bindLifeStateEngine(impl: LifeStateEngineImpl): LifeStateEngine

    @Binds
    @Singleton
    abstract fun bindRuleEngine(impl: RuleEngineImpl): RuleEngine

    @Binds
    @Singleton
    abstract fun bindOcrService(impl: MlKitOcrService): OcrService

    @Binds
    @Singleton
    abstract fun bindGoalRepository(impl: GoalRepositoryImpl): GoalRepository

    @Binds
    @Singleton
    abstract fun bindConversationRepository(impl: ConversationRepositoryImpl): ConversationRepository
}
