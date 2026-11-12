# AboEzzat Remote

**أبو عزت ريموت** — Universal Android remote-control platform.

A modular Android app for TVs, air conditioners, fans, cassette players, radios, stereos, and other appliances. Each model has its own remote layout and independently maintained command profile.

## Current first target: JVC cassette

- Brand: JVC
- Exact model: not confirmed yet
- Active profile: `app/src/main/assets/remotes/jvc_cassette.json`
- Status: initial cassette-control layout only; no verified IR commands are included.
- The user does not have the original physical remote. The next step is to identify the exact model from the label on the device and research its manual / compatible IR protocol.

An earlier GVC placeholder was created while resolving the brand. The user has now clarified that the cassette is **JVC**, so the active app screen and repository use the JVC placeholder profile. The earlier GVC asset remains inactive and is not the active target.

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

1. Confirm the exact JVC cassette model from its rear / underside label.
2. Find the model's manual and original remote layout, if supplied.
3. Research verified IR command data and test each button against the actual device.
4. Build a model catalogue for TVs, AC units, fans, and audio systems.
5. Add profile import/update and custom remote creation.
6. Add supported Wi-Fi/Bluetooth control integrations.

## Accuracy policy

A button is not considered functional merely because it appears on screen. Its command must be verified for the exact model and tested on the target device. Never invent codes or claim a successful control test without evidence.
