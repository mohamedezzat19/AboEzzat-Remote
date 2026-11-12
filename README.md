# AboEzzat Remote

**أبو عزت ريموت** — Universal Android remote-control platform.

A modular remote-control app designed to support televisions, air conditioners, fans, radios, stereos, and other appliances. Each device model can have its own remote layout, button map, and verified command profile.

## Project goals

- Device-specific remote layouts that resemble the original physical remote.
- Infrared (IR) transmission on phones with a built-in IR emitter.
- Extensible device and model catalog.
- Separate UI definitions from command/code data.
- Future support for supported Wi-Fi and Bluetooth control methods.
- No guessed IR codes: commands must be verified for the exact model or protocol.

## First target device

- **Brand:** JVC
- **Model:** RC-EZ38S
- **Device type:** Portable CD / cassette / radio system

The first app screen includes the device categories and a profile-driven JVC remote layout. Its button arrangement follows the original RM-SRCEZ38A remote-control section in the JVC RC-EZ38S user manual. The JVC buttons are intentionally not wired to invented codes. A verified command profile must be added before a button can transmit.

## Build

Open this repository in Android Studio and sync the Gradle project, or run:

```bash
gradle assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Remote profile architecture\n\n- Profiles are stored as JSON assets under `app/src/main/assets/remotes/`.\n- `RemoteProfileRepository` loads a profile, and `RemoteProfileParser` converts it into reusable models.\n- Button groups and numeric keypad layout are generated from profile data.\n- Each command must explicitly set `verified: true` and provide a valid carrier frequency and raw timing pattern before transmission is allowed.\n- JVC remote model reference: [RM-SRCEZ38A remote-control unit in the RC-EZ38S manual](https://www.manualslib.com/manual/2539971/Jvc-Rc-Ez38s.html?page=9).\n\n## Technical notes

- Application ID: `com.aboezzat.remote`
- Language: Kotlin
- Minimum Android version: Android 6.0 (API 23)
- Compile SDK: 35
- IR capability is optional because not every Android phone has an IR emitter.

## Roadmap

1. Build and verify the Android foundation.
2. Implement a structured device/model/remote-profile catalog.
3. Add verified JVC RC-EZ38S command data and test every key.
4. Add TV, air-conditioner, fan, and audio remote profiles.
5. Add profile import/update and custom remote creation.
6. Add supported network-based control integrations.

## Safety and accuracy

A button is not considered functional merely because it appears on screen. Its command must be mapped to a verified code and tested against the target device.
