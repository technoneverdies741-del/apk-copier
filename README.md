# APK Copier

A small Android utility that copies installed apps' APK files to app storage and opens the system share menu so you can transfer them for inspection.

## Download

Install the APK from [app-debug.apk](app/build/outputs/apk/debug/app-debug.apk).

## Use

1. Install APK Copier on your Android or VR headset.
2. Choose an installed app and tap **Export**.
3. Use the Android share menu to copy the base APK and any split APK files to a location you can access.

APK files contain compiled code and resources, not the original source project. Tools such as JADX can decompile code for inspection. Respect each app's license and terms.

## Build

Open this project in Android Studio and build the `app` debug variant. The project targets SDK 35 and supports Android 6.0+.
