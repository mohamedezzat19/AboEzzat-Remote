# IR capture guide for AboEzzat Remote

This guide prepares the exact command data needed for the JVC RC-EZ38S / RM-SRCEZ38A profile. It does not invent or pre-fill any JVC command.

## Why a real capture is needed

The JVC 37.9 kHz specification describes the carrier and pulse timing format, but it does not publish the RC-EZ38S custom/address byte or the data byte for each button. A matching physical RM-SRCEZ38A remote, or an independently verified capture from that exact remote, is therefore required.

A phone's built-in Consumer IR feature is a transmitter. It cannot receive or learn the missing signal.

## Safe capture setup

Use an external IR receiver module and a separate ESP32 or Arduino board:

- A 38 kHz demodulating IR receiver module (for example, a suitable VS1838B/TSOP-style module).
- A compatible ESP32/Arduino development board.
- Connect the receiver's OUT, GND, and supply pins only according to that module's datasheet and the development board's logic-voltage limits.
- Keep the receiver circuit electrically separate from the JVC unit. Point the original remote at the external receiver; do not probe the JVC's internal circuitry.
- Do not open the JVC unit. Its service manual warns about hazardous mains circuitry and requires qualified service procedures.

The JVC physical specification gives a nominal carrier of 37.9 kHz. A common 38 kHz demodulating receiver is the nearest standard receiver class, but the capture must still be checked for clean, repeatable timings.

## Capture procedure

1. Load a raw IR receive/dump example for the chosen board and receiver. The Arduino-IRremote project's `ReceiveDump` example can print raw timings in microseconds; its documentation also describes the raw output. The IRremoteESP8266 project's `IRrecvDumpV2` example is another option. See the source links below.
2. Capture one button at a time, beginning with STANDBY/ON.
3. For each button, make at least three separate captures. Keep the complete raw timing output and note whether the receiver reports buffer overflow.
4. Repeat for every key on the RM-SRCEZ38A. Do not infer one key from another.
5. Preserve the signed raw timing output until it is reviewed. In common dump formats, positive values represent marks and negative values represent spaces; the initial idle gap is not a button mark.
6. If the decoder reports an UNKNOWN hash, do not use that hash as a sendable command. The raw timing sequence is what matters for reproducing an unknown protocol.
7. Verify a candidate by replaying it through a suitable IR transmitter toward the RC-EZ38S and confirming the intended action. A capture alone is not proof that the command controls the unit.

## AboEzzat Remote profile format

The app's existing parser accepts a command entry in the profile's `commands` object with this structure:

```json
{
  "commands": {
    "power": {
      "carrierFrequencyHz": 37900,
      "patternMicros": [8440, 4220, 527, 1055],
      "verified": false
    }
  }
}
```

The numbers above are only a schema illustration, not a real power command. Do not copy this example into the active JVC profile as a working command.

For the app's current transmitter, `patternMicros` must be an even-length array of positive integers, alternating mark, space, mark, space, starting with a mark. Remove the leading idle gap from a signed capture and convert each mark/space duration to a positive microsecond value while preserving order. Do not silently truncate a capture or guess missing timings.

Keep `verified` false until the exact command has been replayed and the expected RC-EZ38S action has been observed. Only then may that individual command be marked true.

## Useful primary project references

- Arduino-IRremote ReceiveDump example: https://github.com/Arduino-IRremote/Arduino-IRremote/blob/master/examples/ReceiveDump/ReceiveDump.ino
- Arduino-IRremote project documentation: https://github.com/Arduino-IRremote/Arduino-IRremote
- IRremoteESP8266 IRrecvDumpV2 example: https://github.com/crankyoldgit/IRremoteESP8266/blob/master/examples/IRrecvDumpV2/IRrecvDumpV2.ino
- IRremoteESP8266 FAQ on unknown signals and raw replay: https://github.com/crankyoldgit/IRremoteESP8266/wiki/Frequently-Asked-Questions
- Exact-model JVC service-manual research: [JVC RC-EZ38S IR research](JVC_RC-EZ38S_IR_RESEARCH.md)

## Current status

- The active JVC profile remains intentionally without command entries.
- The protocol carrier reference remains 37,900 Hz.
- No button is marked verified.
- The next evidence needed is a complete raw capture set from an RM-SRCEZ38A, followed by replay testing on the exact RC-EZ38S.
