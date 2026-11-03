package com.example.atomfan.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FanDeviceDao {
    @Query("SELECT * FROM fan_devices ORDER BY name ASC")
    fun getAllDevices(): Flow<List<FanDevice>>

    @Query("SELECT * FROM fan_devices WHERE id = :id")
    suspend fun getDeviceById(id: Long): FanDevice?

    @Query("SELECT * FROM fan_devices WHERE macAddress = :macAddress LIMIT 1")
    suspend fun getDeviceByMac(macAddress: String): FanDevice?

    @Query("SELECT * FROM fan_devices WHERE isDefault = 1 LIMIT 1")
    fun getDefaultDevice(): Flow<FanDevice?>

    @Query("SELECT * FROM fan_devices WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultDeviceSync(): FanDevice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: FanDevice): Long

    @Update
    suspend fun updateDevice(device: FanDevice)

    @Delete
    suspend fun deleteDevice(device: FanDevice)

    @Query("DELETE FROM fan_devices WHERE id = :id")
    suspend fun deleteDeviceById(id: Long)

    @Query("UPDATE fan_devices SET isDefault = (CASE WHEN id = :deviceId THEN 1 ELSE 0 END)")
    suspend fun setDefaultDevice(deviceId: Long)
}
