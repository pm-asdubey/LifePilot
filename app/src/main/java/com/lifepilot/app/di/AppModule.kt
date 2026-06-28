package com.lifepilot.app.di

import com.lifepilot.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides the installed version name to non-app modules (e.g. UpdateRepositoryImpl)
     * without requiring them to depend on the app's BuildConfig directly.
     */
    @Provides
    @Named("currentVersionName")
    fun provideCurrentVersionName(): String = BuildConfig.VERSION_NAME

    /**
     * Provides the GitHub repository slug ("owner/repo") sourced from a Gradle property.
     * Set `github.repo=yourname/lifepilot` in local.properties for local development.
     * In CI, pass `-Pgithub.repo=${{ github.repository }}` in the Gradle command.
     */
    @Provides
    @Named("githubRepo")
    fun provideGithubRepo(): String = BuildConfig.GITHUB_REPO
}
