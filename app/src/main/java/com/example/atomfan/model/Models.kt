package com.example.atomfan.model

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
enum class RemoteButton(val displayName: String) {
    POWER("Power"),
    SPEED_1("Speed 1"),
    SPEED_2("Speed 2"),
    SPEED_3("Speed 3"),
    SPEED_4("Speed 4"),
    SPEED_5("Speed 5"),
    BOOST("Boost"),
    TIMER("Timer"),
    LED("LED"),
    SLEEP("Sleep")
}

@Serializable
enum class TransportMode(val displayName: String) {
    PHONE_IR("Phone IR"),
    FAN_BLE("Fan Direct BLE"),
    ESP32_BRIDGE("ESP32 Bridge")
}

@Serializable
data class IrCode(
    val button: RemoteButton = RemoteButton.POWER,
    val protocol: String = "NEC",
    val address: Int = 0,
    val command: Int = 0,
    val frequency: Int = 38000,
    val rawTimings: List<Int> = emptyList(),
    val comment: String = "REPLACE ME"
)

data class FanState(
    val powerOn: Boolean = false,
    val speed: Int = 0, // 1..5, or 6 for boost
    val led: Boolean = false,
    val boost: Boolean = false,
    val sleep: Boolean = false,
    val timer: Int = 0, // 0 = off
    val rawBytesHex: String = "",
    val lastUpdatedTimestamp: Long = 0L
)

data class BleLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val message: String,
    val gattStatus: Int? = null,
    val isError: Boolean = false
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
}
