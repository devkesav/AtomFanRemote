package com.example.atomfan.automation

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.atomfan.AtomFanApplication
import com.example.atomfan.model.RemoteButton
import com.example.atomfan.model.TransportMode
import com.example.atomfan.notification.NotificationHelper
import kotlinx.coroutines.flow.first

class AutomationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ruleId = inputData.getLong("rule_id", -1L)
        val deviceId = inputData.getLong("device_id", 1L)
        val ruleName = inputData.getString("rule_name") ?: "Automation Rule"
        val targetSpeed = inputData.getInt("target_speed", 3)
        val targetPower = inputData.getBoolean("target_power", true)
        val targetMode = inputData.getString("target_mode") ?: "Normal"

        val app = applicationContext as AtomFanApplication
        val notificationHelper = NotificationHelper(applicationContext)

        try {
            val mode = app.repository.transportModeFlow.first()
            val repeatCount = app.repository.repeatCountFlow.first()

            if (targetPower) {
                val button = when (targetSpeed) {
                    1 -> RemoteButton.SPEED_1
                    2 -> RemoteButton.SPEED_2
                    3 -> RemoteButton.SPEED_3
                    4 -> RemoteButton.SPEED_4
                    5 -> RemoteButton.SPEED_5
                    6 -> RemoteButton.BOOST
                    else -> RemoteButton.SPEED_3
                }

                when (mode) {
                    TransportMode.PHONE_IR -> {
                        app.phoneIrTransport.setRepeatCount(repeatCount)
                        val code = app.repository.getIrCode(button)
                        app.phoneIrTransport.send(code)
                    }
                    TransportMode.FAN_BLE -> {
                        val hexCode = when (targetSpeed) {
                            1 -> "11"
                            2 -> "12"
                            3 -> "13"
                            4 -> "14"
                            5 -> "15"
                            6 -> "56"
                            else -> "13"
                        }
                        app.fanBleManager.writeExperimentalHexString(hexCode)
                    }
                    TransportMode.ESP32_BRIDGE -> {
                        val code = app.repository.getIrCode(button)
                        if (code.protocol.equals("NEC", ignoreCase = true)) {
                            app.esp32BleManager.sendNec(code.address, code.command)
                        } else {
                            app.esp32BleManager.sendRaw(code.frequency, code.rawTimings)
                        }
                    }
                }

                val powerWatts = when (targetSpeed) {
                    1 -> 6.0
                    2 -> 10.0
                    3 -> 14.0
                    4 -> 20.0
                    5 -> 28.0
                    6 -> 35.0
                    else -> 14.0
                }

                app.deviceRepository.logUsage(
                    deviceId = deviceId,
                    speed = targetSpeed,
                    powerWatts = powerWatts,
                    mode = targetMode,
                    durationSeconds = 3600
                )

                if (app.repository.automationNotifEnabledFlow.first()) {
                    notificationHelper.showAutomationNotification(
                        title = "Automation Executed",
                        message = "Rule '$ruleName' turned fan ON at Speed $targetSpeed ($targetMode)"
                    )
                }
            } else {
                when (mode) {
                    TransportMode.PHONE_IR -> {
                        val code = app.repository.getIrCode(RemoteButton.POWER)
                        app.phoneIrTransport.send(code)
                    }
                    TransportMode.FAN_BLE -> {
                        app.fanBleManager.writeExperimentalHexString("00")
                    }
                    TransportMode.ESP32_BRIDGE -> {
                        val code = app.repository.getIrCode(RemoteButton.POWER)
                        if (code.protocol.equals("NEC", ignoreCase = true)) {
                            app.esp32BleManager.sendNec(code.address, code.command)
                        } else {
                            app.esp32BleManager.sendRaw(code.frequency, code.rawTimings)
                        }
                    }
                }

                if (app.repository.automationNotifEnabledFlow.first()) {
                    notificationHelper.showAutomationNotification(
                        title = "Automation Executed",
                        message = "Rule '$ruleName' turned fan OFF"
                    )
                }
            }

            return Result.success()
        } catch (e: Exception) {
            notificationHelper.showAutomationNotification(
                title = "Automation Failed",
                message = "Failed to execute '$ruleName': ${e.message}"
            )
            return Result.failure()
        }
    }
}
