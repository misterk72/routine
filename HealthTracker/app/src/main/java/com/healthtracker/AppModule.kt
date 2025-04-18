package com.healthtracker

import android.content.Context
import androidx.room.Room
import com.healthtracker.data.HealthDatabase
import com.healthtracker.data.UserDao
import com.healthtracker.data.repository.UserRepository
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
        return HealthDatabase.getDatabase(context)
    }
    
    @Provides
    @Singleton
    fun provideUserDao(database: HealthDatabase): UserDao {
        return database.userDao()
    }
    
    @Provides
    @Singleton
    fun provideUserRepository(database: HealthDatabase): UserRepository {
        return UserRepository(database)
    }
}
