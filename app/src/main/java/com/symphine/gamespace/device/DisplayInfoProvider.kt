package com.symphine.gamespace.device

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.provider.Settings
import android.view.Display
import com.symphine.gamespace.core.model.DisplayHardwareInfo
import com.symphine.gamespace.core.model.DisplayModeInfo
import kotlin.math.roundToInt

class DisplayInfoProvider(private val context: Context) {

    fun getDisplayInfo(): DisplayHardwareInfo {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = dm.getDisplay(Display.DEFAULT_DISPLAY)
        val metrics = context.resources.displayMetrics

        val activeMode = display?.mode
        val width = activeMode?.physicalWidth ?: metrics.widthPixels
        val height = activeMode?.physicalHeight ?: metrics.heightPixels
        val currentHz = activeMode?.refreshRate ?: display?.refreshRate ?: 60f

        val modes = display?.supportedModes?.map {
            DisplayModeInfo(
                modeId = it.modeId,
                width = it.physicalWidth,
                height = it.physicalHeight,
                refreshRateHz = it.refreshRate
            )
        } ?: listOf(DisplayModeInfo(0, width, height, currentHz))

        val refreshRates = modes
            .map { (it.refreshRateHz * 10f).roundToInt() / 10f }
            .distinct()
            .sorted()

        val peakSetting = readSystemSetting("peak_refresh_rate")
        val minSetting = readSystemSetting("min_refresh_rate")

        val isHdr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            display?.isHdr ?: false
        } else false

        val isWideColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            display?.isWideColorGamut ?: false
        } else false

        return DisplayHardwareInfo(
            widthPx = width,
            heightPx = height,
            aspectRatio = formatAspectRatio(width, height),
            densityDpi = metrics.densityDpi,
            densityScale = metrics.density,
            currentRefreshRateHz = currentHz,
            supportedModes = modes,
            supportedRefreshRates = refreshRates,
            isAdaptiveRefreshSupported = refreshRates.size > 1,
            isHdrSupported = isHdr,
            isWideColorGamut = isWideColor,
            peakRefreshSetting = peakSetting,
            minRefreshSetting = minSetting,
            wmSizeOverride = null,
            wmDensityOverride = null
        )
    }

    fun readSystemSetting(key: String): String? {
        return runCatching {
            Settings.System.getString(context.contentResolver, key)
        }.getOrNull()
    }

    fun readGlobalSetting(key: String): String? {
        return runCatching {
            Settings.Global.getString(context.contentResolver, key)
        }.getOrNull()
    }

    private fun formatAspectRatio(w: Int, h: Int): String {
        if (w <= 0 || h <= 0) return "Unknown"
        val big = maxOf(w, h)
        val small = minOf(w, h)
        val g = gcd(big, small)
        val rw = big / g
        val rh = small / g
        val decimal = String.format("%.2f:1", big.toFloat() / small.toFloat())
        return if (rw <= 30 && rh <= 30) "$rw:$rh ($decimal)" else decimal
    }

    private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
}
