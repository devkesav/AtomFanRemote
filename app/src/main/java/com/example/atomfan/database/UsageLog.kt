package com.example.atomfan.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_logs")
data class UsageLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deviceId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val speed: Int,
    val powerWatts: Double,
    val mode: String,
    val durationSeconds: Long
)
