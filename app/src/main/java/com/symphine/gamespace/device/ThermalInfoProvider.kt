package com.symphine.gamespace.device

import android.content.Context
import android.os.PowerManager
import com.symphine.gamespace.core.model.ThermalHardwareInfo
import com.symphine.gamespace.core.model.ThermalZoneReading
import java.io.File

class ThermalInfoProvider(
    private val context: Context,
    private val batteryInfoProvider: BatteryInfoProvider
) {

    fun getThermalInfo(): ThermalHardwareInfo {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val status = powerManager.currentThermalStatus
        val label = mapThermalStatus(status)
        val batteryTemp = batteryInfoProvider.getBatteryInfo().temperatureCelsius

        val zones = readThermalZones()
        val note = if (zones.isEmpty()) {
            "Kernel thermal zones restricted by SELinux; using Android PowerManager Thermal API & battery sensor."
        } else {
            "PowerManager Thermal API + ${zones.size} readable thermal zone(s)."
        }

        return ThermalHardwareInfo(
            statusCode = status,
            statusLabel = label,
            isThrottling = status >= PowerManager.THERMAL_STATUS_MODERATE,
            batteryTempCelsius = batteryTemp,
            readableZones = zones,
            sensorAccessNote = note
        )
    }

    fun mapThermalStatus(status: Int): String = when (status) {
        PowerManager.THERMAL_STATUS_NONE -> "Normal (None)"
        PowerManager.THERMAL_STATUS_LIGHT -> "Light Warmth"
        PowerManager.THERMAL_STATUS_MODERATE -> "Moderate (Throttling Possible)"
        PowerManager.THERMAL_STATUS_SEVERE -> "Severe (Thermal Throttling)"
        PowerManager.THERMAL_STATUS_CRITICAL -> "Critical"
        PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency"
        PowerManager.THERMAL_STATUS_SHUTDOWN -> "Shutdown Threshold"
        else -> "Unknown"
    }

    private fun readThermalZones(): List<ThermalZoneReading> {
        val result = mutableListOf<ThermalZoneReading>()
        runCatching {
            val baseDir = File("/sys/class/thermal")
            if (!baseDir.exists() || !baseDir.canRead()) return emptyList()
            baseDir.listFiles { f -> f.isDirectory && f.name.startsWith("thermal_zone") }
                ?.sortedBy { it.name }
                ?.take(12)
                ?.forEach { zoneDir ->
                    val typeFile = File(zoneDir, "type")
                    val tempFile = File(zoneDir, "temp")
                    if (typeFile.canRead() && tempFile.canRead()) {
                        val name = typeFile.readText().trim()
                        val raw = tempFile.readText().trim().toFloatOrNull()
                        if (name.isNotBlank() && raw != null && raw > -40f) {
                            val celsius = if (raw > 200f) raw / 1000f else raw
                            if (celsius in -20f..150f) {
                                result.add(ThermalZoneReading(name = name, temperatureCelsius = celsius))
                            }
                        }
                    }
                }
        }
        return result
    }
}
