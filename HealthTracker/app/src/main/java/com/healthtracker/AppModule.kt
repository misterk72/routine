package com.healthtracker

import android.content.Context
import androidx.room.Room
import com.healthtracker.data.HealthDatabase
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
    fun provideHealthDatabase(@ApplicationContext context: Context): HealthDatabase {
        return Room.databaseBuilder(
            context,
            HealthDatabase::class.java,
            "health_database"
        ).build()
    }
}
