Build and Install (HealthTracker)
=================================

Prerequisites
-------------
- Android SDK installed and `adb` available on PATH.
- USB debugging enabled on the phone.
- Phone connected and authorized for `adb`.
- If running via Codex CLI or another sandboxed LLM agent, note the permission
  requirements below.

Build APKs
----------
From the `HealthTracker` directory:

```bash
./gradlew assembleDebug
```

This produces:
- `app/build/outputs/apk/dev/debug/app-dev-debug.apk`
- `app/build/outputs/apk/prod/debug/app-prod-debug.apk`

Install on device
-----------------
Check that the device is detected:

```bash
adb devices
```

Install devDebug:

```bash
adb install -r app/build/outputs/apk/dev/debug/app-dev-debug.apk
```

Install prodDebug:

```bash
adb install -r app/build/outputs/apk/prod/debug/app-prod-debug.apk
```

If you get a signature mismatch error, uninstall the existing app and retry:

```bash
adb uninstall com.healthtracker
```

Notes for Codex / sandboxed LLM agents
--------------------------------------
- Building may require elevated permissions so Gradle can write to `~/.gradle`
  and download the wrapper distribution.
- Installing/uninstalling with `adb` requires access to USB devices, which
  often needs escalation.
- If your environment is in "on-request" approval mode, expect to approve
  the Gradle build and any `adb` commands that touch the device.
