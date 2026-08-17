package com.lifepilot.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            // Base read timeout — individual AI requests override this via OkHttpClient.newBuilder().
            // Set high so the per-request override (120–360s) is never capped by the base.
            .readTimeout(400, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            // Prevents TCP connection-reset errors caused by server-side idle connection close.
            .connectionPool(ConnectionPool(maxIdleConnections = 5, keepAliveDuration = 30L, TimeUnit.SECONDS))
            .retryOnConnectionFailure(true)
            .build()
    }
}
