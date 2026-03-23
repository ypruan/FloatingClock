package com.example.floatingclock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.floatingclock.MainActivity
import com.example.floatingclock.R
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class FloatingClockService : Service() {

    companion object {
        const val TAG = "FloatingClockService"
        const val NOTIFICATION_CHANNEL_ID = "floating_clock_channel"
        const val NOTIFICATION_ID = 1001

        // 操作命令
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_UPDATE_CONFIG = "ACTION_UPDATE_CONFIG"

        // 配置键
        const val EXTRA_IS_COUNTDOWN = "is_countdown"
        const val EXTRA_TARGET_TIME = "target_time"
        const val EXTRA_ENABLE_SOUND = "enable_sound"
        const val EXTRA_ENABLE_VIBRATE = "enable_vibrate"
        const val EXTRA_ALERT_BEFORE_MS = "alert_before_ms"
    }

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var params: WindowManager.LayoutParams? = null

    private var isCountdownMode = false
    private var targetTimeMillis: Long = 0
    private var enableSound = true
    private var enableVibrate = true
    private var alertBeforeMs = 5000L // 提前5秒提醒

    private val handler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private val countdownFormat = SimpleDateFormat("mm:ss.SSS", Locale.getDefault())

    private var ringtone: Ringtone? = null
    private var hasAlerted = false

    private var initialX = 0
    private var initialY = 0
    private var touchX = 0f
    private var touchY = 0f

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 16) // 约60fps更新
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE_CONFIG -> {
                updateConfigFromIntent(intent)
                return START_STICKY
            }
        }

        // 解析配置
        updateConfigFromIntent(intent)

        // 启动前台服务
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // 创建悬浮窗
        createFloatingWindow()

        // 开始更新
        handler.post(updateRunnable)

        return START_STICKY
    }

    private fun updateConfigFromIntent(intent: Intent?) {
        intent?.let {
            isCountdownMode = it.getBooleanExtra(EXTRA_IS_COUNTDOWN, isCountdownMode)
            targetTimeMillis = it.getLongExtra(EXTRA_TARGET_TIME, targetTimeMillis)
            enableSound = it.getBooleanExtra(EXTRA_ENABLE_SOUND, enableSound)
            enableVibrate = it.getBooleanExtra(EXTRA_ENABLE_VIBRATE, enableVibrate)
            alertBeforeMs = it.getLongExtra(EXTRA_ALERT_BEFORE_MS, alertBeforeMs)
        }
    }

    private fun createFloatingWindow() {
        if (floatingView != null) return

        val inflater = LayoutInflater.from(this)
        floatingView = inflater.inflate(R.layout.layout_floating_clock, null)

        // 设置触摸事件以实现拖动
        setupDragTouchListener()

        // 设置关闭按钮
        floatingView?.findViewById<View>(R.id.btn_close)?.setOnClickListener {
            stopSelf()
        }

        // 设置点击切换到模式切换
        floatingView?.findViewById<View>(R.id.clock_container)?.setOnClickListener {
            // 可以添加切换逻辑
        }

        params = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        try {
            windowManager?.addView(floatingView, params)
        } catch (e: Exception) {
            Log.e(TAG, "添加悬浮窗失败", e)
        }
    }

    private fun setupDragTouchListener() {
        floatingView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params?.x ?: 0
                    initialY = params?.y ?: 0
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - touchX).toInt()
                    val deltaY = (event.rawY - touchY).toInt()
                    params?.x = initialX + deltaX
                    params?.y = initialY + deltaY
                    if (params != null && floatingView != null) {
                        windowManager?.updateViewLayout(floatingView, params)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun updateClock() {
        val timeTextView = floatingView?.findViewById<TextView>(R.id.tv_time)
        val modeTextView = floatingView?.findViewById<TextView>(R.id.tv_mode)

        if (timeTextView == null || modeTextView == null) return

        val currentTime = System.currentTimeMillis()

        if (isCountdownMode) {
            modeTextView.text = "倒计时"
            val diff = targetTimeMillis - currentTime

            if (diff <= 0) {
                timeTextView.text = "00:00.000"
                if (!hasAlerted) {
                    triggerAlert()
                    hasAlerted = true
                }
            } else {
                timeTextView.text = formatCountdown(diff)
                hasAlerted = false

                // 提前提醒
                if (diff <= alertBeforeMs && diff > alertBeforeMs - 1000) {
                    triggerPreAlert()
                }
            }
        } else {
            modeTextView.text = "当前时间"
            timeTextView.text = timeFormat.format(currentTime)
        }
    }

    private fun formatCountdown(diffMillis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(diffMillis) % 60
        val millis = (diffMillis % 1000).toInt()
        return String.format("%02d:%02d.%03d", minutes, seconds, millis)
    }

    private fun triggerAlert() {
        // 震动
        if (enableVibrate) {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 500, 200, 500, 200, 500),
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 500, 200, 500, 200, 500), -1)
            }
        }

        // 提示音
        if (enableSound) {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(this, notificationUri)
            ringtone?.play()
        }
    }

    private fun triggerPreAlert() {
        if (enableVibrate) {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(100)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "悬浮时钟服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持悬浮时钟在后台运行"
                setSound(null, null)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, FloatingClockService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("悬浮时钟运行中")
            .setContentText("点击返回应用")
            .setSmallIcon(R.drawable.ic_clock)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stop, "停止", stopIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
        ringtone?.stop()
        if (floatingView != null) {
            try {
                windowManager?.removeView(floatingView)
            } catch (e: Exception) {
                Log.e(TAG, "移除悬浮窗失败", e)
            }
            floatingView = null
        }
    }
}
