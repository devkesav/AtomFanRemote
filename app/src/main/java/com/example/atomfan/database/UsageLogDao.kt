package com.example.atomfan.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageLogDao {
    @Query("SELECT * FROM usage_logs WHERE deviceId = :deviceId ORDER BY timestamp DESC")
    fun getLogsForDevice(deviceId: Long): Flow<List<UsageLog>>

    @Query("SELECT * FROM usage_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<UsageLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: UsageLog): Long

    @Query("DELETE FROM usage_logs WHERE deviceId = :deviceId")
    suspend fun clearLogsForDevice(deviceId: Long)
}
