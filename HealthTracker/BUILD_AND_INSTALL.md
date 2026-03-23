Build and Install (HealthTracker)
=================================

Prerequisites
-------------
- Android SDK installed and `adb` available on PATH.
- USB debugging enabled on the phone.
- Phone connected and authorized for `adb`.
- Java 17 available.
- If running via Codex CLI or another sandboxed LLM agent, note the permission
  requirements below.

Quick start
-----------
From the `HealthTracker` directory:

```bash
adb devices
./gradlew assembleDevDebug
adb install -r app/build/outputs/apk/dev/debug/app-dev-debug.apk
```

Build variants
--------------
From the `HealthTracker` directory:

```bash
./gradlew assembleDevDebug
./gradlew assembleProdDebug
```

These commands produce:
- `app/build/outputs/apk/dev/debug/app-dev-debug.apk`
- `app/build/outputs/apk/prod/debug/app-prod-debug.apk`

You can still build both variants in one go with:

```bash
./gradlew assembleDebug
```

Local Gradle fallback
---------------------
If the wrapper is inconvenient in your environment but Gradle 8.13 is already
installed locally on this machine, this command also works:

```bash
GRADLE_USER_HOME=/tmp/gradle-home \
/home/kassabji/.gradle/wrapper/dists/gradle-8.13-bin/5xuhj0ry160q40clulazy9h7d/gradle-8.13/bin/gradle \
--no-daemon assembleDevDebug
```

Swap `assembleDevDebug` for `assembleProdDebug` as needed.

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
adb uninstall com.healthtracker.dev
adb uninstall com.healthtracker
```

Useful package names
--------------------
- dev debug: `com.healthtracker.dev`
- prod debug: `com.healthtracker`

Notes for Codex / sandboxed LLM agents
--------------------------------------
- Building may require elevated permissions so Gradle can write to `~/.gradle`
  and download the wrapper distribution.
- In some sandboxes Gradle fails before project evaluation because it cannot
  inspect network interfaces. In that case, rerun Gradle outside the sandbox.
- Installing/uninstalling with `adb` requires access to USB devices, which
  often needs escalation.
- If your environment is in "on-request" approval mode, expect to approve
  the Gradle build and any `adb` commands that touch the device.
