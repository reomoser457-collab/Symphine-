package com.symphine.gamespace.device

import android.os.Build
import com.symphine.gamespace.core.model.CpuHardwareInfo
import java.io.File

class CpuInfoProvider {
    private var prevIdle: Long? = null
    private var prevTotal: Long? = null

    fun getCpuInfo(): CpuHardwareInfo {
        val coreCount = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val arch = System.getProperty("os.arch") ?: Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"
        val modelName = readCpuModelName()

        val currentFreqs = (0 until coreCount).map { idx ->
            readLongNode("/sys/devices/system/cpu/cpu$idx/cpufreq/scaling_cur_freq")
        }
        val maxFreqs = (0 until coreCount).map { idx ->
            readLongNode("/sys/devices/system/cpu/cpu$idx/cpufreq/cpuinfo_max_freq")
        }
        val governors = (0 until coreCount).map { idx ->
            readStringNode("/sys/devices/system/cpu/cpu$idx/cpufreq/scaling_governor")
        }

        val (utilization, utilNote) = readCpuUtilizationFromProcStat()

        return CpuHardwareInfo(
            architecture = arch,
            modelName = modelName,
            coreCount = coreCount,
            coreFrequenciesKhz = currentFreqs,
            coreMaxFrequenciesKhz = maxFreqs,
            governors = governors,
            utilizationPercent = utilization,
            utilizationNote = utilNote
        )
    }

    private fun readCpuModelName(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val soc = Build.SOC_MODEL
            if (!soc.isNullOrBlank() && soc != Build.UNKNOWN) {
                return soc
            }
        }
        return runCatching {
            val file = File("/proc/cpuinfo")
            if (!file.canRead()) return@runCatching Build.HARDWARE ?: "Restricted by SELinux"
            val lines = file.readLines()
            val hardware = lines.firstOrNull { it.startsWith("Hardware", ignoreCase = true) }
                ?.substringAfter(":")?.trim()
            val model = lines.firstOrNull { it.startsWith("model name", ignoreCase = true) }
                ?.substringAfter(":")?.trim()
            val processor = lines.firstOrNull { it.startsWith("Processor", ignoreCase = true) }
                ?.substringAfter(":")?.trim()
            hardware?.takeIf { it.isNotBlank() }
                ?: model?.takeIf { it.isNotBlank() }
                ?: processor?.takeIf { it.isNotBlank() }
                ?: Build.HARDWARE
                ?: "Unknown"
        }.getOrDefault(Build.HARDWARE ?: "Unknown")
    }

    private fun readCpuUtilizationFromProcStat(): Pair<Float?, String> {
        return runCatching {
            val statFile = File("/proc/stat")
            if (!statFile.canRead()) {
                return@runCatching null to "Restricted by Android SELinux (/proc/stat inaccessible)"
            }
            val firstLine = statFile.bufferedReader().use { it.readLine() }
                ?: return@runCatching null to "Unavailable"
            if (!firstLine.startsWith("cpu ")) {
                return@runCatching null to "Unavailable"
            }
            val parts = firstLine.trim().split(Regex("\\s+")).drop(1).mapNotNull { it.toLongOrNull() }
            if (parts.size < 4) return@runCatching null to "Unavailable"
            val idle = parts[3] + parts.getOrElse(4) { 0L }
            val total = parts.sum()

            val oldIdle = prevIdle
            val oldTotal = prevTotal
            prevIdle = idle
            prevTotal = total

            if (oldIdle != null && oldTotal != null && total > oldTotal) {
                val totalDelta = (total - oldTotal).toFloat()
                val idleDelta = (idle - oldIdle).toFloat()
                val usage = ((totalDelta - idleDelta) / totalDelta * 100f).coerceIn(0f, 100f)
                usage to "Measured via /proc/stat delta"
            } else {
                null to "Sampling baseline..."
            }
        }.getOrElse {
            null to "Restricted by Android SELinux (/proc/stat)"
        }
    }

    private fun readLongNode(path: String): Long? {
        return runCatching {
            val f = File(path)
            if (f.canRead()) f.readText().trim().toLongOrNull() else null
        }.getOrNull()
    }

    private fun readStringNode(path: String): String? {
        return runCatching {
            val f = File(path)
            if (f.canRead()) f.readText().trim().takeIf { it.isNotBlank() } else null
        }.getOrNull()
    }
}
