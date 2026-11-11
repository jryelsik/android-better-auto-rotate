# Better Auto-rotate

An Android utility that enables 4-way sensor auto-rotation system-wide, including 180° reverse portrait, via a Quick Settings tile.

By default, Android's built-in auto-rotate only rotates between 3 orientations (0°, 90°, 270°) and many launchers and apps lock themselves to standard upright portrait. Better Auto-rotate uses a `0x0` non-touchable `TYPE_APPLICATION_OVERLAY` window inside a foreground service to allow all 4 physical orientations across the launcher and installed applications.

## Requirements

- Android 14 (API 34) or higher (compiled against Android 17 / API 37.0)
- "Display over other apps" permission

## Usage

1. Launch **Better Auto-rotate** and tap **Grant Overlay Permission** to enable **Display over other apps**.
2. Tap **Add Quick Settings Tile to Shade** (or add the tile manually from the Quick Settings editor).
3. Disable the default Android **Auto-rotate** tile so the system defaults to standard portrait (`0°`) when this service is stopped.
4. Toggle the **Better Auto-rotate** tile:
   - **Active (`4-way`)**: Starts the foreground overlay service and rotates the display across all 4 sensor orientations (0°, 90°, 180°, 270°).
   - **Inactive (`Portrait`)**: Stops the overlay service and returns the display to standard portrait.

## Project Structure

- [`ReversePortraitService.kt`](app/src/main/kotlin/com/example/reverseportrait/ReversePortraitService.kt): `specialUse` foreground service that attaches and removes the `0x0` `TYPE_APPLICATION_OVERLAY` view configured with `ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR`.
- [`ReversePortraitTileService.kt`](app/src/main/kotlin/com/example/reverseportrait/ReversePortraitTileService.kt): Quick Settings `TileService` that starts/stops `ReversePortraitService` or opens `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` if overlay permission has not been granted.
- [`MainActivity.kt`](app/src/main/kotlin/com/example/reverseportrait/MainActivity.kt): Compose setup screen for granting permission, toggling the service, and requesting Quick Settings tile placement via `StatusBarManager.requestAddTileService`.

## Build and Test

Requires JDK 17+ and Android SDK Platform `android-37.0` (`build-tools;37.0.0`).

```bash
# Run unit tests and assemble the debug APK
./gradlew testDebugUnitTest assembleDebug

# Install on a connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Privacy & Permissions

Better Auto-rotate operates entirely offline. It does not request the `android.permission.INTERNET` permission and does not collect, store, or transmit any user data, analytics, or telemetry.

### Permissions Declared in [`AndroidManifest.xml`](app/src/main/AndroidManifest.xml)

| Permission | Reason |
| :--- | :--- |
| `SYSTEM_ALERT_WINDOW` ("Display over other apps") | Required to attach an invisible `0x0`, non-focusable, non-touchable `TYPE_APPLICATION_OVERLAY` view so the system WindowManager applies 4-way sensor rotation (`SCREEN_ORIENTATION_FULL_SENSOR`) across all apps. |
| `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_SPECIAL_USE` | Required on Android 14+ to keep the rotation overlay service alive in the background while you use other apps. |
| `POST_NOTIFICATIONS` | Used only to display the low-priority ongoing status notification (with a "Turn Off" action button) while the foreground service is active. |

## Disclaimer

This project (code and documentation) was generated with the assistance of AI. Review the source code and permissions before installing or building.
