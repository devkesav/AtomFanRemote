package com.example.atomfan.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "automation_rules")
data class AutomationRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deviceId: Long,
    val name: String,
    val timeStr: String,
    val daysOfWeek: String,
    val targetSpeed: Int,
    val targetMode: String,
    val targetPower: Boolean,
    val isEnabled: Boolean = true
)
