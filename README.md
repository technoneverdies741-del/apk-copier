# APK Copier

Android utility for exporting installed apps' APK files for inspection on Android devices and VR headsets such as Meta Quest 3S.

## Download

Get the APK from the latest [APK Copier release](https://github.com/technoneverdies741-del/apk-copier/releases).

## Use

1. Install APK Copier on your Android device or headset.
2. `com.playgilla.gilla` is pinned to the top of the app list.
3. Tap **Export** beside an app. It packages base and split APKs into a ZIP in the background.
4. Choose **Send to ChatGPT** to share over Wi-Fi. If the ChatGPT app is unavailable, the ZIP is saved through the file picker and ChatGPT opens in the browser for upload. You can also use **Share** or **Save copy**.

The exported ZIP contains APK files, which hold compiled code and resources, not the original source project. Tools such as JADX can decompile code for inspection. Respect each app's license and terms.

## Build

Open this project in Android Studio and build the `app` debug variant. The project targets SDK 35 and supports Android 6.0+.
