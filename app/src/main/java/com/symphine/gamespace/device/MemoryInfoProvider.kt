package com.symphine.gamespace.device

import android.app.ActivityManager
import android.content.Context
import com.symphine.gamespace.core.model.MemoryHardwareInfo
import java.io.File

class MemoryInfoProvider(private val context: Context) {

    fun getMemoryInfo(): MemoryHardwareInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val total = memInfo.totalMem.coerceAtLeast(0L)
        val avail = memInfo.availMem.coerceAtLeast(0L)
        val used = (total - avail).coerceAtLeast(0L)

        var swapTotal: Long? = null
        var swapFree: Long? = null
        runCatching {
            val file = File("/proc/meminfo")
            if (file.canRead()) {
                file.readLines().forEach { line ->
                    when {
                        line.startsWith("SwapTotal:") -> swapTotal = parseKbLineToBytes(line)
                        line.startsWith("SwapFree:") -> swapFree = parseKbLineToBytes(line)
                    }
                }
            }
        }

        return MemoryHardwareInfo(
            totalRamBytes = total,
            availableRamBytes = avail,
            usedRamBytes = used,
            thresholdBytes = memInfo.threshold,
            isLowMemory = memInfo.lowMemory,
            swapTotalBytes = swapTotal,
            swapFreeBytes = swapFree
        )
    }

    private fun parseKbLineToBytes(line: String): Long? {
        val kb = line.substringAfter(":").trim().split(Regex("\\s+")).firstOrNull()?.toLongOrNull()
        return kb?.times(1024L)
    }
}
