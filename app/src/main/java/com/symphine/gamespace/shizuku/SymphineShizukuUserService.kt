package com.symphine.gamespace.shizuku

import android.content.Context
import android.os.Process
import androidx.annotation.Keep
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

@Keep
class SymphineShizukuUserService : ISymphineShizukuService.Stub {

    @Suppress("unused")
    private val context: Context?

    constructor(context: Context?) {
        this.context = context
    }

    constructor() {
        this.context = null
    }

    private val allowedNamespaces = setOf("global", "system", "secure")
    private val allowedSettingKeys = setOf(
        "angle_gl_driver_selection_pkgs",
        "angle_gl_driver_selection_values",
        "app_standby_enabled",
        "app_restriction_enabled",
        "background_check_enabled",
        "low_power",
        "cached_apps_freezer",
        "peak_refresh_rate",
        "min_refresh_rate",
        "window_animation_scale",
        "transition_animation_scale",
        "animator_duration_scale"
    )
    private val allowedProps = setOf(
        "debug.angle.feature_overrides_enabled",
        "debug.hwui.renderer",
        "ro.hardware.egl",
        "ro.hardware.vulkan",
        "ro.opengles.version"
    )
    private val packageRegex = Regex("^[a-zA-Z0-9._]{3,128}$")
    private val valueRegex = Regex("^[a-zA-Z0-9._,: -]{0,256}$")
    private val angleFeatureRegex = Regex("^(disableAnisotropicFiltering|forceNearestFiltering|forceTextureLodOffset[1-4]|[:]){1,128}$")
    private val resolutionRegex = Regex("^([3-9][0-9]{2}|[1-9][0-9]{3})x([3-9][0-9]{2}|[1-9][0-9]{3})$")

    override fun getUid(): Int = Process.myUid()

    override fun getSetting(namespaceName: String?, key: String?): String {
        val ns = namespaceName?.trim() ?: return "ERROR:Null namespace"
        val k = key?.trim() ?: return "ERROR:Null key"
        if (ns !in allowedNamespaces || k !in allowedSettingKeys) {
            return "ERROR:Unsupported setting key $ns/$k"
        }
        return runCommand(listOf("settings", "get", ns, k))
    }

    override fun putSetting(namespaceName: String?, key: String?, value: String?): String {
        val ns = namespaceName?.trim() ?: return "ERROR:Null namespace"
        val k = key?.trim() ?: return "ERROR:Null key"
        val v = value?.trim() ?: return "ERROR:Null value"
        if (ns !in allowedNamespaces || k !in allowedSettingKeys) {
            return "ERROR:Unsupported setting key $ns/$k"
        }
        if (!valueRegex.matches(v)) {
            return "ERROR:Invalid characters in setting value"
        }
        return runCommand(listOf("settings", "put", ns, k, v))
    }

    override fun deleteSetting(namespaceName: String?, key: String?): String {
        val ns = namespaceName?.trim() ?: return "ERROR:Null namespace"
        val k = key?.trim() ?: return "ERROR:Null key"
        if (ns !in allowedNamespaces || k !in allowedSettingKeys) {
            return "ERROR:Unsupported setting key $ns/$k"
        }
        return runCommand(listOf("settings", "delete", ns, k))
    }

    override fun getSystemProperty(propName: String?): String {
        val prop = propName?.trim() ?: return "ERROR:Null property"
        if (prop !in allowedProps) {
            return "ERROR:Property not in allowlist"
        }
        return runCommand(listOf("getprop", prop))
    }

    override fun setAngleFeatureOverride(featureValue: String?): String {
        val feat = featureValue?.trim() ?: return "ERROR:Null feature"
        if (feat.isNotEmpty() && !angleFeatureRegex.matches(feat)) {
            return "ERROR:Invalid ANGLE feature string"
        }
        return runCommand(listOf("setprop", "debug.angle.feature_overrides_enabled", feat))
    }

    override fun getWmSize(): String = runCommand(listOf("wm", "size"))

    override fun setWmSize(widthXHeight: String?): String {
        val res = widthXHeight?.trim() ?: return "ERROR:Null resolution"
        if (!resolutionRegex.matches(res)) {
            return "ERROR:Invalid resolution format (expected WxH between 300 and 9999)"
        }
        return runCommand(listOf("wm", "size", res))
    }

    override fun resetWmSize(): String = runCommand(listOf("wm", "size", "reset"))

    override fun getWmDensity(): String = runCommand(listOf("wm", "density"))

    override fun setWmDensity(dpi: Int): String {
        if (dpi !in 120..1000) {
            return "ERROR:DPI out of safe validated range (120..1000)"
        }
        return runCommand(listOf("wm", "density", dpi.toString()))
    }

    override fun resetWmDensity(): String = runCommand(listOf("wm", "density", "reset"))

    override fun compilePackage(packageName: String?, mode: String?): String {
        val pkg = packageName?.trim() ?: return "ERROR:Null package"
        val m = mode?.trim() ?: return "ERROR:Null mode"
        if (!packageRegex.matches(pkg)) return "ERROR:Invalid package name"
        if (m !in setOf("speed-profile", "speed", "everything")) {
            return "ERROR:Invalid ART compile mode"
        }
        return runCommand(listOf("cmd", "package", "compile", "-m", m, "-f", pkg), timeoutSec = 60L)
    }

    override fun setGameMode(packageName: String?, mode: String?): String {
        val pkg = packageName?.trim() ?: return "ERROR:Null package"
        val m = mode?.trim() ?: return "ERROR:Null mode"
        if (!packageRegex.matches(pkg)) return "ERROR:Invalid package name"
        if (m !in setOf("performance", "standard", "battery")) {
            return "ERROR:Invalid game mode"
        }
        return runCommand(listOf("cmd", "game", "mode", m, pkg))
    }

    override fun getGameServiceDump(packageName: String?): String {
        val pkg = packageName?.trim()
        val raw = runCommand(listOf("dumpsys", "game"))
        if (pkg.isNullOrBlank() || !packageRegex.matches(pkg)) {
            return raw.lines().take(40).joinToString("\n")
        }
        val filtered = raw.lines().filter { it.contains(pkg) || it.contains("Game", ignoreCase = true) }
        return filtered.take(35).joinToString("\n").ifBlank { raw.lines().take(25).joinToString("\n") }
    }

    override fun getDisplayDump(): String {
        val raw = runCommand(listOf("dumpsys", "display"))
        return raw.lines()
            .filter {
                it.contains("supportedModes") ||
                    it.contains("refreshRate") ||
                    it.contains("DisplayDeviceInfo") ||
                    it.contains("mActiveModeId")
            }
            .take(25)
            .joinToString("\n")
    }

    override fun getThermalDump(): String {
        return runCommand(listOf("dumpsys", "thermalservice"))
            .lines()
            .take(30)
            .joinToString("\n")
    }

    override fun forceStopGamePackage(packageName: String?): String {
        val pkg = packageName?.trim() ?: return "ERROR:Null package"
        if (!packageRegex.matches(pkg)) return "ERROR:Invalid package name"
        if (pkg.startsWith("com.android.") || pkg.startsWith("android") || pkg.startsWith("com.google.android.gms")) {
            return "ERROR:Protected system package cannot be stopped"
        }
        return runCommand(listOf("am", "force-stop", pkg))
    }

    override fun destroy() {
        exitProcess(0)
    }

    private fun runCommand(args: List<String>, timeoutSec: Long = 10L): String {
        return try {
            val process = ProcessBuilder(args)
                .redirectErrorStream(true)
                .start()
            val finished = process.waitFor(timeoutSec, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return "ERROR:Command timed out after ${timeoutSec}s"
            }
            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            val code = process.exitValue()
            if (code == 0) {
                output.ifEmpty { "OK" }
            } else {
                "ERROR($code):${output.ifEmpty { "Command rejected by system" }}"
            }
        } catch (t: Throwable) {
            "ERROR:${t.message ?: t.javaClass.simpleName}"
        }
    }
}
