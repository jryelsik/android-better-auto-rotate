package com.example.reverseportrait

import android.content.pm.ActivityInfo
import android.view.OrientationEventListener
import org.junit.Assert.assertEquals
import org.junit.Test

class ReversePortraitServiceTest {
  @Test
  fun nextAction_withoutOverlayPermission_requestsPermission() {
    assertEquals(
      ToggleDecision.REQUEST_OVERLAY_PERMISSION,
      ReversePortraitService.nextAction(hasOverlayPermission = false, currentlyRunning = false)
    )
    assertEquals(
      ToggleDecision.REQUEST_OVERLAY_PERMISSION,
      ReversePortraitService.nextAction(hasOverlayPermission = false, currentlyRunning = true)
    )
  }

  @Test
  fun nextAction_withOverlayPermission_togglesServiceState() {
    assertEquals(
      ToggleDecision.START_SERVICE,
      ReversePortraitService.nextAction(hasOverlayPermission = true, currentlyRunning = false)
    )
    assertEquals(
      ToggleDecision.STOP_SERVICE,
      ReversePortraitService.nextAction(hasOverlayPermission = true, currentlyRunning = true)
    )
  }

  @Test
  fun resolvePortraitOrientation_portraitAngles_returnPortrait() {
    for (deg in listOf(0, 45, 315, 359)) {
      assertEquals(
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        ReversePortraitService.resolvePortraitOrientation(deg, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
      )
      assertEquals(
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        ReversePortraitService.resolvePortraitOrientation(deg, ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT)
      )
    }
  }

  @Test
  fun resolvePortraitOrientation_reversePortraitAngles_returnReversePortrait() {
    for (deg in listOf(180, 135, 225)) {
      assertEquals(
        ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
        ReversePortraitService.resolvePortraitOrientation(deg, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
      )
      assertEquals(
        ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
        ReversePortraitService.resolvePortraitOrientation(deg, ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT)
      )
    }
  }

  @Test
  fun resolvePortraitOrientation_landscapeAngles_preservesCurrentOrientation() {
    for (deg in listOf(90, 270)) {
      assertEquals(
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        ReversePortraitService.resolvePortraitOrientation(deg, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
      )
      assertEquals(
        ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
        ReversePortraitService.resolvePortraitOrientation(deg, ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT)
      )
    }
  }

  @Test
  fun resolvePortraitOrientation_unknown_preservesCurrentOrientation() {
    assertEquals(
      ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
      ReversePortraitService.resolvePortraitOrientation(
        OrientationEventListener.ORIENTATION_UNKNOWN,
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
      )
    )
    assertEquals(
      ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
      ReversePortraitService.resolvePortraitOrientation(
        OrientationEventListener.ORIENTATION_UNKNOWN,
        ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
      )
    )
  }

  @Test
  fun defaultRotationMode_isPortraitAndReverse() {
    assertEquals(RotationMode.PORTRAIT_AND_REVERSE, ReversePortraitService.rotationMode.value)
  }
}
