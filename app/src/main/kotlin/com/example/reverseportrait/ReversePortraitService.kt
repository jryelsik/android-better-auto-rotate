package com.example.reverseportrait

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.service.quicksettings.TileService
import android.view.OrientationEventListener
import android.view.View
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RotationMode {
  PORTRAIT_AND_REVERSE,
  FULL_FOUR_WAY
}

class ReversePortraitService : Service() {
  private var overlayView: View? = null
  private var overlayParams: WindowManager.LayoutParams? = null
  private var orientationListener: OrientationEventListener? = null

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP || !Settings.canDrawOverlays(this)) {
      stopSelf()
      return START_NOT_STICKY
    }

    val mode = loadRotationMode(this)
    startForeground(
      NOTIFICATION_ID,
      buildNotification(mode),
      ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
    )
    attachOrUpdateOverlay(mode)
    return START_STICKY
  }

  override fun onDestroy() {
    detachOverlay()
    super.onDestroy()
  }

  private fun attachOrUpdateOverlay(mode: RotationMode) {
    val wm = getSystemService(WindowManager::class.java)
    val isNew = (overlayView == null)
    val view = overlayView ?: View(this).also { overlayView = it }
    val params = overlayParams ?: WindowManager.LayoutParams(
      0,
      0,
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
      PixelFormat.TRANSLUCENT
    ).also { overlayParams = it }

    if (mode == RotationMode.FULL_FOUR_WAY) {
      orientationListener?.disable()
      params.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
    } else {
      if (params.screenOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT &&
        params.screenOrientation != ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
      ) {
        params.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
      }
      if (orientationListener == null) {
        orientationListener = object : OrientationEventListener(this) {
          override fun onOrientationChanged(degrees: Int) {
            val currentParams = overlayParams ?: return
            val currentView = overlayView ?: return
            val next = resolvePortraitOrientation(degrees, currentParams.screenOrientation)
            if (next != currentParams.screenOrientation) {
              currentParams.screenOrientation = next
              runCatching { wm.updateViewLayout(currentView, currentParams) }
            }
          }
        }
      }
      orientationListener?.enable()
    }

    if (isNew) {
      wm.addView(view, params)
      _isRunning.value = true
      syncTile(this)
    } else {
      runCatching { wm.updateViewLayout(view, params) }
    }
  }

  private fun detachOverlay() {
    orientationListener?.disable()
    orientationListener = null
    overlayView?.let { view ->
      runCatching { getSystemService(WindowManager::class.java).removeView(view) }
    }
    overlayView = null
    overlayParams = null
    _isRunning.value = false
    syncTile(this)
  }

  private fun buildNotification(mode: RotationMode): Notification {
    val nm = getSystemService(NotificationManager::class.java)
    if (nm.getNotificationChannel(CHANNEL_ID) == null) {
      nm.createNotificationChannel(
        NotificationChannel(CHANNEL_ID, "Better Auto-rotate Active", NotificationManager.IMPORTANCE_LOW)
      )
    }
    val stopIntent = PendingIntent.getService(
      this,
      0,
      Intent(this, ReversePortraitService::class.java).setAction(ACTION_STOP),
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val contentText = when (mode) {
      RotationMode.PORTRAIT_AND_REVERSE ->
        "0° and 180° (Portrait & Reverse Portrait) auto-rotate active."
      RotationMode.FULL_FOUR_WAY ->
        "4-way auto-rotate (0°, 90°, 180°, 270°) active."
    }
    return Notification.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_reverse_portrait)
      .setContentTitle("Better Auto-rotate is ON")
      .setContentText(contentText)
      .setOngoing(true)
      .addAction(Notification.Action.Builder(null, "Turn Off", stopIntent).build())
      .build()
  }

  companion object {
    const val ACTION_STOP = "com.example.reverseportrait.ACTION_STOP"
    private const val CHANNEL_ID = "reverse_portrait"
    private const val NOTIFICATION_ID = 1001

    private const val PREFS_NAME = "reverse_portrait_prefs"
    private const val KEY_ROTATION_MODE = "rotation_mode"

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _rotationMode = MutableStateFlow(RotationMode.PORTRAIT_AND_REVERSE)
    val rotationMode: StateFlow<RotationMode> = _rotationMode.asStateFlow()

    fun loadRotationMode(context: Context): RotationMode {
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val modeName = prefs.getString(KEY_ROTATION_MODE, RotationMode.PORTRAIT_AND_REVERSE.name)
      val mode = runCatching { RotationMode.valueOf(modeName ?: "") }
        .getOrDefault(RotationMode.PORTRAIT_AND_REVERSE)
      _rotationMode.value = mode
      return mode
    }

    fun selectModeAndStart(context: Context, mode: RotationMode) {
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_ROTATION_MODE, mode.name)
        .apply()
      _rotationMode.value = mode
      if (Settings.canDrawOverlays(context)) {
        context.startForegroundService(Intent(context, ReversePortraitService::class.java))
        syncTile(context)
      }
    }

    fun turnOff(context: Context) {
      context.stopService(Intent(context, ReversePortraitService::class.java))
      _isRunning.value = false
      syncTile(context)
    }

    fun setRotationMode(context: Context, mode: RotationMode) {
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_ROTATION_MODE, mode.name)
        .apply()
      _rotationMode.value = mode
      syncTile(context)
      if (_isRunning.value && Settings.canDrawOverlays(context)) {
        context.startForegroundService(Intent(context, ReversePortraitService::class.java))
      }
    }

    /** Pure helper function for unit testing and sensor listener. */
    fun resolvePortraitOrientation(
      degrees: Int,
      currentOrientation: Int = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    ): Int = when {
      degrees == OrientationEventListener.ORIENTATION_UNKNOWN -> currentOrientation
      degrees in 135..225 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
      degrees in 0..45 || degrees in 315..359 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
      else -> currentOrientation
    }

    /** Pure decision helper for unit testing and tile/button clicks. */
    fun nextAction(hasOverlayPermission: Boolean, currentlyRunning: Boolean): ToggleDecision =
      when {
        !hasOverlayPermission -> ToggleDecision.REQUEST_OVERLAY_PERMISSION
        currentlyRunning -> ToggleDecision.STOP_SERVICE
        else -> ToggleDecision.START_SERVICE
      }

    fun toggle(context: Context): ToggleDecision {
      val decision = nextAction(Settings.canDrawOverlays(context), _isRunning.value)
      when (decision) {
        ToggleDecision.START_SERVICE ->
          context.startForegroundService(Intent(context, ReversePortraitService::class.java))
        ToggleDecision.STOP_SERVICE ->
          turnOff(context)
        ToggleDecision.REQUEST_OVERLAY_PERMISSION -> Unit
      }
      return decision
    }

    private fun syncTile(context: Context) {
      runCatching {
        TileService.requestListeningState(
          context,
          ComponentName(context, ReversePortraitTileService::class.java)
        )
      }
    }
  }
}

enum class ToggleDecision {
  REQUEST_OVERLAY_PERMISSION,
  START_SERVICE,
  STOP_SERVICE
}
