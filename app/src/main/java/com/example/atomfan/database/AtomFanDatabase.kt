package com.example.atomfan.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FanDevice::class, UsageLog::class, AutomationRule::class, ComfortProfile::class],
    version = 1,
    exportSchema = false
)
abstract class AtomFanDatabase : RoomDatabase() {
    abstract fun fanDeviceDao(): FanDeviceDao
    abstract fun usageLogDao(): UsageLogDao
    abstract fun automationRuleDao(): AutomationRuleDao
    abstract fun comfortProfileDao(): ComfortProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AtomFanDatabase? = null

        fun getDatabase(context: Context): AtomFanDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AtomFanDatabase::class.java,
                    "atomfan_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
