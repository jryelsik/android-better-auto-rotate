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
import android.view.View
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReversePortraitService : Service() {
  private var overlayView: View? = null

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP || !Settings.canDrawOverlays(this)) {
      stopSelf()
      return START_NOT_STICKY
    }

    startForeground(
      NOTIFICATION_ID,
      buildNotification(),
      ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
    )
    attachOverlay()
    return START_STICKY
  }

  override fun onDestroy() {
    detachOverlay()
    super.onDestroy()
  }

  private fun attachOverlay() {
    if (overlayView != null) return
    val wm = getSystemService(WindowManager::class.java)
    val view = View(this)
    val params = WindowManager.LayoutParams(
      0,
      0,
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
      PixelFormat.TRANSLUCENT
    ).apply {
      // Forces 4-way sensor rotation (Portrait, Landscape, Reverse Portrait, Reverse Landscape) across all apps.
      screenOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
    }
    wm.addView(view, params)
    overlayView = view
    _isRunning.value = true
    syncTile(this)
  }

  private fun detachOverlay() {
    overlayView?.let { view ->
      runCatching { getSystemService(WindowManager::class.java).removeView(view) }
    }
    overlayView = null
    _isRunning.value = false
    syncTile(this)
  }

  private fun buildNotification(): Notification {
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
    return Notification.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_reverse_portrait)
      .setContentTitle("Better Auto-rotate is ON")
      .setContentText("4-way auto-rotate (including Reverse Portrait) active.")
      .setOngoing(true)
      .addAction(Notification.Action.Builder(null, "Turn Off", stopIntent).build())
      .build()
  }

  companion object {
    const val ACTION_STOP = "com.example.reverseportrait.ACTION_STOP"
    private const val CHANNEL_ID = "reverse_portrait"
    private const val NOTIFICATION_ID = 1001

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

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
          context.stopService(Intent(context, ReversePortraitService::class.java))
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
