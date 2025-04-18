package com.healthtracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY name")
    fun getAllUsers(): Flow<List<User>>
    
    @Query("SELECT * FROM users WHERE isDefault = 1 LIMIT 1")
    fun getDefaultUser(): Flow<User?>
    
    @Insert
    suspend fun insert(user: User): Long
    
    @Update
    suspend fun update(user: User)
    
    @Delete
    suspend fun delete(user: User)
    
    @Query("UPDATE users SET isDefault = 0")
    suspend fun clearDefaultUser()
    
    @Query("UPDATE users SET isDefault = 1 WHERE id = :userId")
    suspend fun setDefaultUser(userId: Long)
}
