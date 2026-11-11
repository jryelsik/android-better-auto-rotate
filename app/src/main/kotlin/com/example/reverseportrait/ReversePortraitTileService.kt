package com.example.reverseportrait

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class ReversePortraitTileService : TileService() {
  override fun onStartListening() {
    super.onStartListening()
    val mode = ReversePortraitService.loadRotationMode(this)
    updateTileState(ReversePortraitService.isRunning.value, mode)
  }

  override fun onClick() {
    super.onClick()
    val mode = ReversePortraitService.rotationMode.value
    when (ReversePortraitService.toggle(this)) {
      ToggleDecision.REQUEST_OVERLAY_PERMISSION -> {
        val intent = Intent(
          Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
          Uri.parse("package:$packageName")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntent.getActivity(
          this,
          0,
          intent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        startActivityAndCollapse(pendingIntent)
      }
      ToggleDecision.START_SERVICE -> updateTileState(true, mode)
      ToggleDecision.STOP_SERVICE -> updateTileState(false, mode)
    }
  }

  private fun updateTileState(
    active: Boolean,
    mode: RotationMode = ReversePortraitService.rotationMode.value
  ) {
    val tile = qsTile ?: return
    tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    tile.subtitle = when {
      !active -> "Portrait"
      mode == RotationMode.PORTRAIT_AND_REVERSE -> "0° / 180°"
      else -> "4-way"
    }
    tile.updateTile()
  }
}
