package com.lifepilot.app.di

import android.content.Context
import com.lifepilot.data.schema.SchemaEngineImpl
import com.lifepilot.domain.engine.SchemaEngine
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule
