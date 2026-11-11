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
    updateTileState(ReversePortraitService.isRunning.value)
  }

  override fun onClick() {
    super.onClick()
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
      ToggleDecision.START_SERVICE -> updateTileState(true)
      ToggleDecision.STOP_SERVICE -> updateTileState(false)
    }
  }

  private fun updateTileState(active: Boolean) {
    val tile = qsTile ?: return
    tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    tile.subtitle = if (active) "4-way" else "Portrait"
    tile.updateTile()
  }
}
