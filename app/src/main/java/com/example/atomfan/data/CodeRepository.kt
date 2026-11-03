package com.example.atomfan.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.atomfan.R
import com.example.atomfan.model.IrCode
import com.example.atomfan.model.RemoteButton
import com.example.atomfan.model.TransportMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.atomFanDataStore: DataStore<Preferences> by preferencesDataStore(name = "atomfan_settings")

class CodeRepository(
    private val context: Context,
    customDataStore: DataStore<Preferences>? = null
) {
    private val dataStore: DataStore<Preferences> by lazy {
        customDataStore ?: context.atomFanDataStore
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    companion object {
        val KEY_TRANSPORT_MODE = stringPreferencesKey("transport_mode")
        val KEY_REPEAT_COUNT = intPreferencesKey("repeat_count")
        val KEY_EXPERIMENTAL_BLE_WRITE = booleanPreferencesKey("experimental_ble_write")
        val KEY_CUSTOM_IR_CODES = stringPreferencesKey("custom_ir_codes")
        val KEY_ELECTRICITY_TARIFF = doublePreferencesKey("electricity_tariff")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_AUTOMATION_NOTIF_ENABLED = booleanPreferencesKey("automation_notif_enabled")
        val KEY_DEFAULT_DEVICE_ID = longPreferencesKey("default_device_id")
    }

    val electricityTariffFlow: Flow<Double> = dataStore.data.map { prefs ->
        prefs[KEY_ELECTRICITY_TARIFF] ?: 7.00
    }

    val notificationsEnabledFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    val automationNotifEnabledFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_AUTOMATION_NOTIF_ENABLED] ?: true
    }

    val defaultDeviceIdFlow: Flow<Long?> = dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_DEVICE_ID]
    }

    val transportModeFlow: Flow<TransportMode> = dataStore.data.map { prefs ->
        val modeName = prefs[KEY_TRANSPORT_MODE] ?: TransportMode.PHONE_IR.name
        runCatching { TransportMode.valueOf(modeName) }.getOrDefault(TransportMode.PHONE_IR)
    }

    val repeatCountFlow: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_REPEAT_COUNT] ?: 1
    }

    val experimentalBleWriteFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_EXPERIMENTAL_BLE_WRITE] ?: false
    }

    val irCodesFlow: Flow<Map<RemoteButton, IrCode>> = dataStore.data.map { prefs ->
        val rawJson = prefs[KEY_CUSTOM_IR_CODES]
        if (!rawJson.isNullOrEmpty()) {
            runCatching {
                val list = json.decodeFromString<List<IrCode>>(rawJson)
                list.associateBy { it.button }
            }.getOrElse {
                loadDefaultIrCodesMap()
            }
        } else {
            loadDefaultIrCodesMap()
        }
    }

    fun loadDefaultIrCodes(): List<IrCode> {
        return try {
            val jsonText = context.resources.openRawResource(R.raw.ir_codes).bufferedReader().use { it.readText() }
            json.decodeFromString<List<IrCode>>(jsonText)
        } catch (e: Exception) {
            RemoteButton.entries.map { button ->
                IrCode(button = button, comment = "REPLACE ME")
            }
        }
    }

    private fun loadDefaultIrCodesMap(): Map<RemoteButton, IrCode> {
        val list = loadDefaultIrCodes()
        val defaultMap = list.associateBy { it.button }.toMutableMap()
        for (button in RemoteButton.entries) {
            if (!defaultMap.containsKey(button)) {
                defaultMap[button] = IrCode(button = button, comment = "REPLACE ME")
            }
        }
        return defaultMap
    }

    suspend fun getIrCode(button: RemoteButton): IrCode {
        val map = irCodesFlow.first()
        return map[button] ?: IrCode(button = button, comment = "REPLACE ME")
    }

    suspend fun saveIrCode(button: RemoteButton, code: IrCode) {
        dataStore.edit { prefs ->
            val currentMap = irCodesFlow.first().toMutableMap()
            currentMap[button] = code.copy(button = button)
            prefs[KEY_CUSTOM_IR_CODES] = json.encodeToString(currentMap.values.toList())
        }
    }

    suspend fun saveAllIrCodes(codes: List<IrCode>) {
        dataStore.edit { prefs ->
            prefs[KEY_CUSTOM_IR_CODES] = json.encodeToString(codes)
        }
    }

    suspend fun setTransportMode(mode: TransportMode) {
        dataStore.edit { prefs ->
            prefs[KEY_TRANSPORT_MODE] = mode.name
        }
    }

    suspend fun setRepeatCount(count: Int) {
        val validCount = count.coerceIn(1, 2)
        dataStore.edit { prefs ->
            prefs[KEY_REPEAT_COUNT] = validCount
        }
    }

    suspend fun setExperimentalBleWrite(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_EXPERIMENTAL_BLE_WRITE] = enabled
        }
    }

    suspend fun setElectricityTariff(tariff: Double) {
        dataStore.edit { prefs ->
            prefs[KEY_ELECTRICITY_TARIFF] = tariff
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setAutomationNotifEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_AUTOMATION_NOTIF_ENABLED] = enabled
        }
    }

    suspend fun setDefaultDeviceId(deviceId: Long?) {
        dataStore.edit { prefs ->
            if (deviceId != null) {
                prefs[KEY_DEFAULT_DEVICE_ID] = deviceId
            } else {
                prefs.remove(KEY_DEFAULT_DEVICE_ID)
            }
        }
    }

    suspend fun resetToDefaultCodes() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_CUSTOM_IR_CODES)
        }
    }
}
