package com.healthtracker.di

import android.content.Context
import android.content.SharedPreferences
import com.healthtracker.jellyfin.JellyfinClient
import com.healthtracker.jellyfin.JellyfinSettings
import com.healthtracker.jellyfin.JellyfinSettingsStore
import com.healthtracker.jellyfin.OkHttpJellyfinClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("health_tracker_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideJellyfinClient(
        impl: OkHttpJellyfinClient
    ): JellyfinClient = impl

    @Provides
    @Singleton
    fun provideJellyfinSettings(
        store: JellyfinSettingsStore
    ): JellyfinSettings = store
}
