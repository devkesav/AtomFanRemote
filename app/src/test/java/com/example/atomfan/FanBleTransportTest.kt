package com.example.atomfan

import android.app.Application
import com.example.atomfan.ble.FanBleManager
import com.example.atomfan.model.IrCode
import com.example.atomfan.model.RemoteButton
import com.example.atomfan.transport.FanBleTransport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FanBleTransportTest {

    private lateinit var application: Application
    private lateinit var fanBleManager: FanBleManager

    @Before
    fun setUp() {
        application = Application()
        fanBleManager = FanBleManager(application)
    }

    @Test
    fun testCommandFailsWhenDisconnected() = runTest {
        val transport = FanBleTransport(
            bleManager = fanBleManager,
            targetMacProvider = { "AA:BB:CC:DD:EE:FF" },
            isExperimentalWriteEnabled = { true }
        )

        val result = transport.send(IrCode(button = RemoteButton.POWER, address = 0x40, command = 0x12))
        assertFalse("Command should fail when device is not connected", result.isSuccess)
    }

    @Test
    fun testCommandFailsWhenExperimentalWriteDisabled() = runTest {
        val transport = FanBleTransport(
            bleManager = fanBleManager,
            targetMacProvider = { "AA:BB:CC:DD:EE:FF" },
            isExperimentalWriteEnabled = { false }
        )

        val result = transport.send(IrCode(button = RemoteButton.POWER, address = 0x40, command = 0x12))
        assertFalse("Command should fail when experimental write is disabled", result.isSuccess)
    }

    @Test
    fun testCommandIsolationFailsWhenTargetMacMismatch() = runTest {
        // Attempting to write with a mismatched target MAC should be prevented
        val result = fanBleManager.writeExperimentalHexString(
            hexString = "4012",
            targetMacAddress = "11:22:33:44:55:66"
        )
        assertFalse("Command must be rejected when target MAC does not match active connection", result.isSuccess)
    }
}
