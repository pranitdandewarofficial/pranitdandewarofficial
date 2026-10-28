# PRANIT VIBES — JioSaavn Playlist Automation (Beta)

Android Accessibility-based UI automation prototype for personal use.

## How to test
1. Install the APK built by the GitHub Actions workflow.
2. Open PRANIT VIBES and paste one test song per line, e.g. `Husn — Anuv Jain`.
3. Tap **Enable automation service** and enable **PRANIT VIBES Playlist Automation** in Android Accessibility settings.
4. Return to PRANIT VIBES, save the list, and tap **Start playlist automation**.
5. Keep the official JioSaavn app in the foreground and supervise the first run. Stop it immediately if it selects an unexpected control.

## Limitations
- JioSaavn has no verified public playlist-write API available to this project. This prototype attempts visible UI interaction and depends on JioSaavn's current accessible labels/layout.
- The integration has not yet been validated against a real JioSaavn account/device. It must be treated as beta; don't trust a completion message without checking the playlist.
- It never requests passwords, OTPs, session cookies, or hidden account data. It does not bypass login or paywalls.
- If the app UI changes or controls are not exposed to Android Accessibility, automation may stop without adding all tracks.
- Playlist creation flows vary by app version; first run with one non-critical test track.

## Stop
Tap **Stop automation** in PRANIT VIBES. You can also disable the service in Android Accessibility settings.
