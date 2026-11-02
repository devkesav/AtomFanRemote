package com.example.atomfan.transport

import android.content.Context
import android.hardware.ConsumerIrManager
import com.example.atomfan.ir.NecEncoder
import com.example.atomfan.model.IrCode
import kotlinx.coroutines.delay

class PhoneIrTransport(
    context: Context,
    private var repeatCount: Int = 1
) : FanTransport {

    private val irManager: ConsumerIrManager? =
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    fun setRepeatCount(count: Int) {
        repeatCount = count.coerceIn(1, 2)
    }

    override suspend fun send(code: IrCode): Result<Unit> {
        val manager = irManager
            ?: return Result.failure(IllegalStateException("IR Transmit service unavailable on this device."))

        if (!manager.hasIrEmitter()) {
            return Result.failure(IllegalStateException("Device does not have an IR emitter."))
        }

        val pattern: IntArray = if (code.protocol.equals("NEC", ignoreCase = true)) {
            NecEncoder.encodeNec(code.address, code.command)
        } else if (code.rawTimings.isNotEmpty()) {
            code.rawTimings.toIntArray()
        } else {
            return Result.failure(IllegalArgumentException("Invalid IR code: empty raw timings for non-NEC protocol."))
        }

        if (pattern.isEmpty()) {
            return Result.failure(IllegalArgumentException("IR transmission pattern is empty."))
        }

        val phoneSupported = getCarrierFrequenciesAsArray(manager)
        val targetFreq = NecEncoder.getNearestSupportedFrequency(code.frequency, phoneSupported)

        return try {
            val count = repeatCount.coerceAtLeast(1)
            for (i in 0 until count) {
                if (i > 0) {
                    delay(50)
                }
                manager.transmit(targetFreq, pattern)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getCarrierFrequenciesAsArray(manager: ConsumerIrManager): IntArray? {
        return try {
            val ranges = manager.carrierFrequencies ?: return null
            val freqs = mutableListOf<Int>()
            for (range in ranges) {
                freqs.add(range.minFrequency)
                freqs.add(range.maxFrequency)
                if (range.minFrequency <= 38000 && range.maxFrequency >= 38000) {
                    freqs.add(38000)
                }
            }
            freqs.distinct().toIntArray()
        } catch (e: Exception) {
            null
        }
    }
}
