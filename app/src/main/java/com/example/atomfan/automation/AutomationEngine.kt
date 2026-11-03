package com.example.atomfan.automation

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.atomfan.database.AutomationRule
import java.util.Calendar
import java.util.concurrent.TimeUnit

class AutomationEngine(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    fun scheduleRule(rule: AutomationRule) {
        if (!rule.isEnabled) {
            cancelRule(rule.id)
            return
        }

        val initialDelayMs = calculateDelayToNextOccurrence(rule.timeStr, rule.daysOfWeek)

        val inputData = Data.Builder()
            .putLong("rule_id", rule.id)
            .putLong("device_id", rule.deviceId)
            .putString("rule_name", rule.name)
            .putInt("target_speed", rule.targetSpeed)
            .putBoolean("target_power", rule.targetPower)
            .putString("target_mode", rule.targetMode)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<AutomationWorker>()
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag("atomfan_rule_${rule.id}")
            .build()

        workManager.enqueueUniqueWork(
            "atomfan_rule_work_${rule.id}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelRule(ruleId: Long) {
        workManager.cancelUniqueWork("atomfan_rule_work_$ruleId")
        workManager.cancelAllWorkByTag("atomfan_rule_$ruleId")
    }

    fun rescheduleAll(rules: List<AutomationRule>) {
        rules.forEach { rule ->
            scheduleRule(rule)
        }
    }

    private fun calculateDelayToNextOccurrence(timeStr: String, daysOfWeekStr: String): Long {
        val parts = timeStr.split(":")
        val targetHour = parts.getOrNull(0)?.toIntOrNull() ?: 12
        val targetMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val now = Calendar.getInstance()
        val targetCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (targetCal.before(now)) {
            targetCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // If specific days selected (e.g., "Mon,Wed,Fri")
        val activeDays = parseDaysOfWeek(daysOfWeekStr)
        if (activeDays.isNotEmpty()) {
            while (!activeDays.contains(targetCal.get(Calendar.DAY_OF_WEEK))) {
                targetCal.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        return (targetCal.timeInMillis - now.timeInMillis).coerceAtLeast(1000L)
    }

    private fun parseDaysOfWeek(daysStr: String): Set<Int> {
        if (daysStr.isBlank() || daysStr.equals("Everyday", ignoreCase = true)) return emptySet()
        val set = mutableSetOf<Int>()
        val tokens = daysStr.split(",").map { it.trim().lowercase() }

        if (tokens.contains("sun")) set.add(Calendar.SUNDAY)
        if (tokens.contains("mon")) set.add(Calendar.MONDAY)
        if (tokens.contains("tue")) set.add(Calendar.TUESDAY)
        if (tokens.contains("wed")) set.add(Calendar.WEDNESDAY)
        if (tokens.contains("thu")) set.add(Calendar.THURSDAY)
        if (tokens.contains("fri")) set.add(Calendar.FRIDAY)
        if (tokens.contains("sat")) set.add(Calendar.SATURDAY)

        return set
    }
}
