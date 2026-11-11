package com.example.reverseportrait

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reverseportrait.ui.theme.ReversePortraitTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    ReversePortraitService.loadRotationMode(this)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
      navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK)
    )
    setContent {
      ReversePortraitTheme {
        ReversePortraitSetupScreen()
      }
    }
  }
}

@Composable
fun ReversePortraitSetupScreen() {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val isRunning by ReversePortraitService.isRunning.collectAsStateWithLifecycle()
  val rotationMode by ReversePortraitService.rotationMode.collectAsStateWithLifecycle()
  var canDrawOverlays by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        canDrawOverlays = Settings.canDrawOverlays(context)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = Color.Black,
    contentColor = Color.White
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .padding(innerPadding)
        .padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "Better Auto-rotate",
        style = MaterialTheme.typography.headlineMedium,
        color = Color.White
      )
      Text(
        text = if (!canDrawOverlays) {
          "Step 1: Grant 'Display over other apps' so the Quick Settings Tile can auto-rotate your screen in all 4 directions (including Reverse Portrait)."
        } else if (isRunning) {
          when (rotationMode) {
            RotationMode.PORTRAIT_AND_REVERSE ->
              "ACTIVE — Only 0° (Portrait) and 180° (Reverse Portrait) rotation is ON."
            RotationMode.FULL_FOUR_WAY ->
              "ACTIVE — Full 4-way rotation (0°, 90°, 180°, 270°) is ON."
          }
        } else {
          "OFF — Screen is in standard Portrait. Toggle below or from your Quick Settings Tile."
        },
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White
      )

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .selectableGroup()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .selectable(
              selected = (rotationMode == RotationMode.PORTRAIT_AND_REVERSE),
              onClick = {
                ReversePortraitService.setRotationMode(context, RotationMode.PORTRAIT_AND_REVERSE)
              },
              role = Role.RadioButton
            )
            .padding(vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          RadioButton(
            selected = (rotationMode == RotationMode.PORTRAIT_AND_REVERSE),
            onClick = null
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Only 0° (portrait) and 180° (reverse portrait)",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
          )
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .selectable(
              selected = (rotationMode == RotationMode.FULL_FOUR_WAY),
              onClick = {
                ReversePortraitService.setRotationMode(context, RotationMode.FULL_FOUR_WAY)
              },
              role = Role.RadioButton
            )
            .padding(vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          RadioButton(
            selected = (rotationMode == RotationMode.FULL_FOUR_WAY),
            onClick = null
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Full 4-way rotation (0°, 90°, 180°, 270°)",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
          )
        }
      }

      if (!canDrawOverlays) {
        Button(
          modifier = Modifier.fillMaxWidth(),
          onClick = {
            context.startActivity(
              Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
              )
            )
          }
        ) {
          Text("Grant Overlay Permission")
        }
      } else {
        Button(
          modifier = Modifier.fillMaxWidth(),
          onClick = { ReversePortraitService.toggle(context) }
        ) {
          Text(if (isRunning) "Turn OFF Better Auto-rotate" else "Turn ON Better Auto-rotate")
        }

        OutlinedButton(
          modifier = Modifier.fillMaxWidth(),
          onClick = {
            val statusBarManager = context.getSystemService(StatusBarManager::class.java)
            statusBarManager.requestAddTileService(
              ComponentName(context, ReversePortraitTileService::class.java),
              context.getString(R.string.app_name),
              Icon.createWithResource(context, R.drawable.ic_reverse_portrait),
              context.mainExecutor
            ) { result ->
              if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
                Toast.makeText(context, "Tile is already in your Quick Settings!", Toast.LENGTH_SHORT).show()
              }
            }
          }
        ) {
          Text("Add Quick Settings Tile to Shade")
        }
      }
    }
  }
}
