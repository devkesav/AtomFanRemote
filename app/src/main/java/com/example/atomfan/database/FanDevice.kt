package com.example.atomfan.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fan_devices")
data class FanDevice(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val roomName: String,
    val macAddress: String,
    val transportType: String,
    val isDefault: Boolean = false,
    val lastConnected: Long = System.currentTimeMillis()
)
