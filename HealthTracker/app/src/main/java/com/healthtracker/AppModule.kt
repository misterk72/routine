package com.healthtracker

import android.content.Context
import androidx.room.Room
import com.healthtracker.data.HealthDatabase
import com.healthtracker.data.LocationDao
import com.healthtracker.data.UserDao
import com.healthtracker.data.repository.LocationRepository
import com.healthtracker.data.repository.UserRepository
import com.healthtracker.location.LocationService
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
    
    @Provides
    @Singleton
    fun provideLocationDao(database: HealthDatabase): LocationDao {
        return database.locationDao()
    }
    
    @Provides
    @Singleton
    fun provideLocationRepository(locationDao: LocationDao): LocationRepository {
        return LocationRepository(locationDao)
    }
    
    @Provides
    @Singleton
    fun provideLocationService(
        @ApplicationContext context: Context,
        locationRepository: LocationRepository
    ): LocationService {
        return LocationService(context, locationRepository)
    }
}
