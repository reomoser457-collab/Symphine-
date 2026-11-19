package com.symphine.gamespace.device

import android.os.Build
import com.symphine.gamespace.core.model.DeviceHardwareInfo
import com.symphine.gamespace.core.model.OemBrand
import java.io.File

class DeviceInfoProvider {

    fun getDeviceInfo(): DeviceHardwareInfo {
        val manufacturer = Build.MANUFACTURER ?: "Unknown"
        val brand = Build.BRAND ?: "Unknown"
        val socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOfNotNull(
                Build.SOC_MANUFACTURER.takeIf { it.isNotBlank() && it != Build.UNKNOWN },
                Build.SOC_MODEL.takeIf { it.isNotBlank() && it != Build.UNKNOWN }
            ).joinToString(" ").ifBlank { readPropertyFallback("ro.board.platform") }
        } else {
            readPropertyFallback("ro.board.platform")
        }

        return DeviceHardwareInfo(
            manufacturer = manufacturer,
            brand = brand,
            model = Build.MODEL ?: "Unknown",
            device = Build.DEVICE ?: "Unknown",
            androidVersion = Build.VERSION.RELEASE ?: "Unknown",
            apiLevel = Build.VERSION.SDK_INT,
            buildId = Build.DISPLAY ?: Build.ID ?: "Unknown",
            securityPatch = Build.VERSION.SECURITY_PATCH ?: "Unknown",
            kernelVersion = readKernelVersion(),
            socModel = socModel,
            hardwareBoard = Build.HARDWARE ?: "Unknown",
            primaryAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown",
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            oemBrand = detectOemBrand(manufacturer, brand)
        )
    }

    fun detectOemBrand(manufacturer: String, brand: String): OemBrand {
        val combined = "$manufacturer $brand".lowercase()
        return when {
            "poco" in combined -> OemBrand.POCO
            "redmi" in combined -> OemBrand.REDMI
            "xiaomi" in combined -> OemBrand.XIAOMI
            "realme" in combined -> OemBrand.REALME
            "oppo" in combined -> OemBrand.OPPO
            "samsung" in combined -> OemBrand.SAMSUNG
            "asus" in combined -> OemBrand.ASUS
            "iqoo" in combined -> OemBrand.IQOO
            "vivo" in combined -> OemBrand.VIVO
            "motorola" in combined || "moto" in combined -> OemBrand.MOTOROLA
            "google" in combined -> OemBrand.PIXEL
            else -> OemBrand.OTHER
        }
    }

    private fun readKernelVersion(): String {
        return runCatching {
            File("/proc/version").takeIf { it.canRead() }?.readText()?.trim()
                ?.substringBefore(" (")?.take(80)
                ?: System.getProperty("os.version")
                ?: "Unavailable"
        }.getOrDefault(System.getProperty("os.version") ?: "Unavailable")
    }

    private fun readPropertyFallback(key: String): String {
        return runCatching {
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getMethod("get", String::class.java, String::class.java)
            (method.invoke(null, key, "") as? String)?.takeIf { it.isNotBlank() } ?: "Unavailable"
        }.getOrDefault("Unavailable")
    }
}
