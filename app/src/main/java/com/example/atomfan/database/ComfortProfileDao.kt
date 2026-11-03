package com.example.atomfan.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ComfortProfileDao {
    @Query("SELECT * FROM comfort_profiles ORDER BY name ASC")
    fun getAllProfiles(): Flow<List<ComfortProfile>>

    @Query("SELECT * FROM comfort_profiles WHERE id = :id")
    suspend fun getProfileById(id: Long): ComfortProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ComfortProfile): Long

    @Update
    suspend fun updateProfile(profile: ComfortProfile)

    @Delete
    suspend fun deleteProfile(profile: ComfortProfile)
}
