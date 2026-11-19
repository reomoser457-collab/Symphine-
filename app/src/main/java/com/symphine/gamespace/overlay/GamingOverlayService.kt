package com.symphine.gamespace.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.R
import com.symphine.gamespace.app.SymphineApplication
import com.symphine.gamespace.core.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GamingOverlayService : Service() {

    companion object {
        const val ACTION_START = "com.symphine.gamespace.overlay.START"
        const val ACTION_STOP = "com.symphine.gamespace.overlay.STOP"
        const val EXTRA_PROFILE_NAME = "profile_name"
        const val EXTRA_OPACITY = "opacity"
        const val EXTRA_COMPACT = "compact"
        private const val NOTIFICATION_ID = 4010

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun startOverlay(context: Context, profileName: String, opacity: Float = 0.88f, compact: Boolean = false) {
            if (!Settings.canDrawOverlays(context)) return
            val intent = Intent(context, GamingOverlayService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PROFILE_NAME, profileName)
                putExtra(EXTRA_OPACITY, opacity)
                putExtra(EXTRA_COMPACT, compact)
            }
            context.startForegroundService(intent)
        }

        fun stopOverlay(context: Context) {
            val intent = Intent(context, GamingOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var monitorJob: Job? = null
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var headerTextView: TextView? = null
    private var metricsTextView: TextView? = null
    private var startMillis = System.currentTimeMillis()
    private var activeProfile = "Balanced"
    private var isCompactMode = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP || !Settings.canDrawOverlays(this)) {
            stopSelfCleanly()
            return START_NOT_STICKY
        }

        activeProfile = intent?.getStringExtra(EXTRA_PROFILE_NAME) ?: "Balanced"
        val opacity = intent?.getFloatExtra(EXTRA_OPACITY, 0.88f)?.coerceIn(0.35f, 1.0f) ?: 0.88f
        isCompactMode = intent?.getBooleanExtra(EXTRA_COMPACT, false) ?: false

        val app = application as SymphineApplication
        app.notificationHelper.ensureChannels()
        val notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_OVERLAY)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("SYMPHINE Gaming Overlay")
            .setContentText("Active telemetry overlay • Profile: $activeProfile")
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        showOverlayWindow(opacity)
        _isRunning.value = true
        return START_NOT_STICKY
    }

    private fun showOverlayWindow(opacity: Float) {
        if (overlayView != null) {
            overlayView?.alpha = opacity
            return
        }
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (12 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            alpha = opacity
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E6111319"))
                cornerRadius = 16 * resources.displayMetrics.density
                setStroke((1.5f * resources.displayMetrics.density).toInt(), Color.parseColor("#A78BFA"))
            }
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val titleView = TextView(this).apply {
            text = "SYMPHINE • $activeProfile"
            setTextColor(Color.parseColor("#C4B5FD"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerTextView = titleView

        val closeBtn = TextView(this).apply {
            text = " ✕ "
            setTextColor(Color.parseColor("#D5D5DF"))
            textSize = 12f
            setOnClickListener { stopSelfCleanly() }
        }

        val modeBtn = TextView(this).apply {
            text = " ◱ "
            setTextColor(Color.parseColor("#A78BFA"))
            textSize = 12f
            setOnClickListener {
                isCompactMode = !isCompactMode
            }
        }

        topRow.addView(titleView)
        topRow.addView(modeBtn)
        topRow.addView(closeBtn)

        val statsView = TextView(this).apply {
            text = "Initializing telemetry..."
            setTextColor(Color.parseColor("#D5D5DF"))
            textSize = 10.5f
            typeface = Typeface.MONOSPACE
            setPadding(0, (4 * resources.displayMetrics.density).toInt(), 0, 0)
        }
        metricsTextView = statsView

        container.addView(topRow)
        container.addView(statsView)

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 140
        }

        container.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var touchX = 0f
            private var touchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        touchX = event.rawX
                        touchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        layoutParams.x = initialX + (event.rawX - touchX).toInt()
                        layoutParams.y = initialY + (event.rawY - touchY).toInt()
                        runCatching { windowManager?.updateViewLayout(container, layoutParams) }
                        return true
                    }
                }
                return false
            }
        })

        overlayView = container
        runCatching {
            windowManager?.addView(container, layoutParams)
        }.onFailure {
            stopSelfCleanly()
            return
        }

        startMillis = System.currentTimeMillis()
        startTelemetryLoop()
    }

    private fun startTelemetryLoop() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            val app = application as SymphineApplication
            while (isActive) {
                val mem = app.memoryInfoProvider.getMemoryInfo()
                val bat = app.batteryInfoProvider.getBatteryInfo()
                val thermal = app.thermalInfoProvider.getThermalInfo()
                val disp = app.displayInfoProvider.getDisplayInfo()
                val elapsedSec = ((System.currentTimeMillis() - startMillis) / 1000L).coerceAtLeast(0L)
                val mm = elapsedSec / 60
                val ss = elapsedSec % 60
                val availGb = mem.availableRamBytes / (1024f * 1024f * 1024f)
                val tempStr = bat.temperatureCelsius?.let { String.format("%.1f°C", it) } ?: "N/A"
                val hzStr = "${disp.currentRefreshRateHz.toInt()}Hz"

                headerTextView?.text = "SYMPHINE • $activeProfile • %02d:%02d".format(mm, ss)
                metricsTextView?.text = if (isCompactMode) {
                    "RAM ${String.format("%.1fG", availGb)} free | ${bat.percentage}% $tempStr | $hzStr"
                } else {
                    "RAM Free : ${String.format("%.2f GB", availGb)}\n" +
                        "Battery  : ${bat.percentage}% ($tempStr)\n" +
                        "Thermal  : ${thermal.statusLabel.substringBefore(" ")}\n" +
                        "Display  : $hzStr"
                }
                delay(2000L)
            }
        }
    }

    private fun stopSelfCleanly() {
        monitorJob?.cancel()
        overlayView?.let { v ->
            runCatching { windowManager?.removeView(v) }
        }
        overlayView = null
        _isRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        overlayView?.let { v ->
            runCatching { windowManager?.removeView(v) }
        }
        overlayView = null
        _isRunning.value = false
        scope.cancel()
        super.onDestroy()
    }
}
