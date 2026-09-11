# Build Alitronics APK from your Android phone

This package contains a GitHub Actions workflow. GitHub's servers perform the Android build,
so you do not need Android Studio or an Android SDK on your phone.

## Steps
1. Create/sign in to a GitHub account in your phone browser.
2. Create a new repository.
3. Upload the **contents** of the `Alitronics` project folder from this ZIP.
4. Make sure `.github/workflows/build-apk.yml` is present.
5. Open the repository's **Actions** tab.
6. Select **Build Alitronics APK**.
7. Tap **Run workflow**.
8. Wait for the workflow to finish.
9. Open the completed run and download the `Alitronics-debug-apk` artifact.
10. Extract it and install `app-debug.apk` on both phones.

The first build may take several minutes while Gradle and Android dependencies are downloaded.

## What this build contains
- Offline Wi-Fi Direct discovery/connection
- Text chat
- Bidirectional local file transfer
- Media-call UI placeholders

Voice/video calling is intentionally not claimed as complete until a real media implementation
is compiled and tested on two physical Android devices.
