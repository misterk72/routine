package com.healthtracker.data.repository

import com.healthtracker.data.HealthDatabase
import com.healthtracker.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(private val database: HealthDatabase) {
    
    fun getAllUsers(): Flow<List<User>> {
        return database.userDao().getAllUsers()
    }
    
    fun getDefaultUser(): Flow<User?> {
        return database.userDao().getDefaultUser()
    }
    
    suspend fun insertUser(user: User): Long {
        return withContext(Dispatchers.IO) {
            database.userDao().insert(user)
        }
    }
    
    suspend fun updateUser(user: User) {
        withContext(Dispatchers.IO) {
            database.userDao().update(user)
        }
    }
    
    suspend fun deleteUser(user: User) {
        withContext(Dispatchers.IO) {
            database.userDao().delete(user)
        }
    }
    
    suspend fun setDefaultUser(userId: Long) {
        withContext(Dispatchers.IO) {
            database.userDao().clearDefaultUser()
            database.userDao().setDefaultUser(userId)
        }
    }
}
