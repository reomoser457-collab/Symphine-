package com.symphine.gamespace.device

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import android.os.Build
import android.provider.Settings
import com.symphine.gamespace.core.model.GraphicsHardwareInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GraphicsInfoProvider(private val context: Context) {

    @Volatile
    private var cachedGlDetails: Triple<String, String, String>? = null

    suspend fun getGraphicsInfo(): GraphicsHardwareInfo = withContext(Dispatchers.Default) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val configInfo = am.deviceConfigurationInfo
        val glesVersion = configInfo.glEsVersion ?: "Unknown"

        val glDetails = cachedGlDetails ?: queryOffscreenGlStrings().also { cachedGlDetails = it }

        val pm = context.packageManager
        val features = pm.systemAvailableFeatures
        val vulkanFeature = features.firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL }
        val vulkanVersionFeature = features.firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION }

        val vulkanSupported = vulkanFeature != null || vulkanVersionFeature != null
        val vulkanVersion = vulkanVersionFeature?.version?.let { ver ->
            val major = (ver shr 22) and 0x3FF
            val minor = (ver shr 12) and 0x3FF
            val patch = ver and 0xFFF
            "$major.$minor.$patch (Level ${vulkanFeature?.version ?: 0})"
        }

        val eglHint = readSystemProp("ro.hardware.egl")
        val hwui = readSystemProp("debug.hwui.renderer")
        val angleFeature = readSystemProp("debug.angle.feature_overrides_enabled")
        val anglePkgs = runCatching {
            Settings.Global.getString(context.contentResolver, "angle_gl_driver_selection_pkgs")
        }.getOrNull()
        val angleValues = runCatching {
            Settings.Global.getString(context.contentResolver, "angle_gl_driver_selection_values")
        }.getOrNull()

        val anglePkgName = findAnglePackage(pm)

        GraphicsHardwareInfo(
            gpuRenderer = glDetails.first,
            gpuVendor = glDetails.second,
            openGlEsVersion = glesVersion,
            glVersionDetail = glDetails.third,
            vulkanSupported = vulkanSupported,
            vulkanVersion = vulkanVersion,
            eglDriverHint = eglHint,
            hwuiRenderer = hwui,
            angleInstalled = anglePkgName != null || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
            anglePackageName = anglePkgName ?: "System ANGLE / Driver Built-in",
            angleTargetPackages = anglePkgs,
            angleDriverValues = angleValues,
            angleFeatureOverrides = angleFeature,
            gpuUtilizationNote = "Standard Android APIs do not expose live GPU utilization (%) without vendor-privileged counters."
        )
    }

    private fun findAnglePackage(pm: PackageManager): String? {
        val candidates = listOf("com.android.angle", "org.chromium.angle")
        return candidates.firstOrNull { pkg ->
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkg, 0)
                }
            }.isSuccess
        }
    }

    private fun queryOffscreenGlStrings(): Triple<String, String, String> {
        return runCatching {
            val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (display == EGL14.EGL_NO_DISPLAY) return Triple("Unknown GPU", "Unknown", "Unavailable")
            val version = IntArray(2)
            if (!EGL14.eglInitialize(display, version, 0, version, 1)) {
                return Triple("Unknown GPU", "Unknown", "Unavailable")
            }
            val attribList = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfig = IntArray(1)
            EGL14.eglChooseConfig(display, attribList, 0, configs, 0, 1, numConfig, 0)
            val config = configs[0] ?: run {
                EGL14.eglTerminate(display)
                return Triple("Unknown GPU", "Unknown", "Unavailable")
            }
            val surfAttribs = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
            val surface = EGL14.eglCreatePbufferSurface(display, config, surfAttribs, 0)
            val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
            val context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
            EGL14.eglMakeCurrent(display, surface, surface, context)

            val renderer = GLES20.glGetString(GLES20.GL_RENDERER)?.takeIf { it.isNotBlank() } ?: "Unknown GPU"
            val vendor = GLES20.glGetString(GLES20.GL_VENDOR)?.takeIf { it.isNotBlank() } ?: "Unknown"
            val glVer = GLES20.glGetString(GLES20.GL_VERSION)?.takeIf { it.isNotBlank() } ?: "Unknown"

            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(display, surface)
            EGL14.eglDestroyContext(display, context)
            EGL14.eglTerminate(display)

            Triple(renderer, vendor, glVer)
        }.getOrDefault(Triple("Unavailable (EGL Context Error)", "Unknown", "Unavailable"))
    }

    private fun readSystemProp(key: String): String? {
        return runCatching {
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getMethod("get", String::class.java, String::class.java)
            (method.invoke(null, key, "") as? String)?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
