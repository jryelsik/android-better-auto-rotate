package com.example.reverseportrait

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
}
