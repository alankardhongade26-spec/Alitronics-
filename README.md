# Alitronics V3 — Offline Chat + File Transfer

## Implemented
- Wi-Fi Direct peer discovery
- Direct offline TCP text chat
- Bidirectional file transfer on a separate TCP port
- File reception to app-specific `received` storage
- Basic filename sanitization and collision handling
- Android 13+ Nearby Wi-Fi Devices permission handling
- Phone/tablet-friendly layout

## Media calling
Voice/video calling is **not marked complete** in this build. The UI deliberately labels those
buttons as V3 media placeholders instead of pretending that a camera preview is a working
two-device call. The next implementation should use a real local WebRTC media transport
with direct Wi-Fi candidates and then be tested on physical devices.

## Validation performed
1. Extracted the previous project successfully.
2. Rebuilt the manifest, layout and Kotlin transport code.
3. Verified every `R.id.*` reference has a matching layout ID.
4. Verified required permissions and ports are present.
5. Verified the file-transfer protocol has bounded filename/size checks and exact byte-count reception.
6. Verified there are no obvious unresolved imports/references by source inspection.
7. Full APK compilation and two-phone runtime testing could not be performed here because an
   Android SDK/Gradle toolchain and physical Android devices are not attached to this environment.

## Physical test plan
On two Android devices:
1. Turn on Wi-Fi.
2. Launch Alitronics on both and grant Nearby Wi-Fi Devices permission.
3. Find the other device and connect.
4. Exchange text messages.
5. Use Send File on either device and select a file.
6. Confirm the receiver reports the file and stores it under the app's external `received` directory.

## Important
This package is source code, not a claimed tested APK.
