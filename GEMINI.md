# Budget Tracker / kwago — Project Guidelines & Invariants

## 1. Android Manifest & Launcher Icon Invariants
- **Always Declare Activity-Level Icons**: In `AndroidManifest.xml`, both the `<application>` element and the launcher `<activity>` (the activity containing `android.intent.action.MAIN` and `android.intent.category.LAUNCHER`) MUST explicitly define:
  ```xml
  android:icon="@mipmap/kwago_launcher"
  android:roundIcon="@mipmap/kwago_launcher_round"
  ```
- **Verify Launcher Activity Badging**: When introducing or updating icons, verify APK metadata using `aapt dump badging <apk>` to confirm that `launchable-activity` has a non-empty `icon='...'` attribute.
- **Density Fallback Synchronization**: When custom launcher icons are applied, synchronize legacy fallback density drawables (`mipmap-*/ic_launcher.webp` and `ic_launcher_round.webp`) to match the new icon assets, ensuring non-adaptive fallback paths do not render generic Android droid placeholders.

## 2. Application Identity (`applicationId` vs `namespace`)
- **Decoupled Configuration**: To rebrand or customize the user-facing package name (e.g., `com.dazztine.kwago`), update `defaultConfig.applicationId` in `app/build.gradle.kts`.
- **Preserve `namespace`**: Keep `android.namespace` untouched (unless an explicit codebase-wide package refactor is requested) to preserve internal Kotlin file packages, Room queries, and `R` class references without unnecessary churn.
- **OEM Install Prompts**: When deploying an APK with a newly introduced `applicationId` to a connected device via `adb install`, note that OEM ROMs (like Xiaomi/MIUI/HyperOS) require user confirmation ("Install via USB") on the phone screen for the initial install.

## 3. Shell Execution on Windows
- **PowerShell Syntax**: Always chain multiple CLI commands with `;` instead of bash-style `&&`.
