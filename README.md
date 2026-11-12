# AboEzzat Remote

**أبو عزت ريموت** — Universal Android remote-control platform.

A modular Android app for TVs, air conditioners, fans, cassette players, radios, stereos, and other appliances. Each model has its own remote layout and independently maintained command profile.

## Current first target: JVC RC-EZ38S

- Device: JVC CD Portable System, model `RC-EZ38S`.
- Original remote model in the instruction manual: `RM-SRCEZ38A`.
- Active profile: `app/src/main/assets/remotes/jvc_cassette.json`.
- The app layout now includes the documented source, navigation, effects, sound, numeric, and volume keys.
- Layout reference: JVC RC-EZ38S instruction manual, Remote Control Unit section.
- IR command data remains unverified and is intentionally empty. The user does not have the original physical remote, so command codes must come from reliable documentation / protocol research and be tested against the device.

## Architecture

- Android app module in Kotlin.
- Reusable `RemoteProfile` model and JSON parser.
- Asset-backed `RemoteProfileRepository`.
- Device-specific remote layouts and command maps.
- IR transmission is allowed only when a button has a verified command profile.
- IR capability is optional; phones without a built-in IR emitter cannot transmit IR directly.

## Build

Open in Android Studio and sync Gradle, or run:

```bash
gradle assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

## App details

- Application ID: `com.aboezzat.remote`
- Language: Kotlin
- Minimum Android: API 23
- Compile SDK: 35

## Roadmap

1. Research reliable IR protocol / command data for the RM-SRCEZ38A remote.
2. Test each command against the RC-EZ38S before marking it verified.
3. Refine the remote's visual geometry against the manual's remote illustration.
4. Build a model catalogue for TVs, AC units, fans, and audio systems.
5. Add profile import/update and custom remote creation.
6. Add supported Wi-Fi/Bluetooth control integrations.

## Accuracy policy

A button is not considered functional merely because it appears on screen. Its command must be verified for the exact model and tested on the target device. Never invent codes or claim a successful control test without evidence.
