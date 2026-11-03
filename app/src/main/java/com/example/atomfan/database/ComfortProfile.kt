package com.example.atomfan.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comfort_profiles")
data class ComfortProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val daytimeSpeed: Int = 3,
    val nighttimeSpeed: Int = 1,
    val preferredMode: String = "Normal"
)
