# JVC RC-EZ38S infrared command research

## Target identity

- Main unit: JVC CD Portable System RC-EZ38S.
- Original remote named in the owner's manual: RM-SRCEZ38A.
- The user does not own the original remote.
- The remote key layout is documented in the RC-EZ38S instruction manual, page 9.

## Findings

1. The official JVC infrared physical specification describes a 940 nm emitter, 37.9 kHz carrier, pulse-interval modulation, an 8.44 ms header mark, a 4.22 ms header space, 0.527 ms bit marks, and 1.055 / 2.11 ms zero/one spaces.
2. This is a transport/protocol specification only. JVC explicitly distinguishes a device-specific custom code from the 8-bit operation data code. The document does not identify the RC-EZ38S custom code or any RM-SRCEZ38A button data values.
3. Exact-name searches for RC-EZ38S and RM-SRCEZ38A did not find a matching command file in the searched GitHub code index or the searched Flipper-IRDB repository.
4. Flipper-IRDB contains other JVC audio-device remotes, including CA-D851TR / RM-SED80TRU and other JVC audio models. These are not identified as compatible with RC-EZ38S.
5. The LIRC remote database contains many device configurations, including other JVC remotes, but no matching RC-EZ38S / RM-SRCEZ38A configuration was identified in this research pass.
6. A nearby product, JVC RC-EZ35S, uses a differently named remote (RM-SRCEZ35J) and a different button set. Adjacent model numbers must not be assumed to share command codes.
7. RC-EZ38 service/parts documentation lists RM-SRCEZ38A as the remote control unit, corroborating the target identity, but does not provide per-key IR command values.
8. JVC's 37.9 kHz specification describes the signal format and a 16-bit custom+data structure. It does not supply the RC-EZ38S-specific custom byte or button data table.


9. The exact RC-EZ38S service manual was located: JVC service manual No. 28228, dated October 2006. It includes a block diagram and main/control-circuit schematics. The block diagram identifies a TCC760 CPU and an EM78P156E microcontroller, but the published manual does not include an RM-SRCEZ38A per-button IR code table or a custom/data-byte mapping.
10. The service manual therefore improves confidence in the exact hardware identity and its control architecture, but it still cannot be used to synthesize the remote's command bytes. The parts list again names RM-SRCEZ38A as the remote control unit.


11. The service manual's page-5 block diagram shows the TCC760 CPU, a separate EM78P156E microcontroller block, and a local KEY input block. This documents part of the unit's control architecture, but it does not show an IR protocol decoder mapping or remote-button command values.
12. The service manual's schematic text extraction contains no searchable IR / remote-sensor label, and the block diagram alone is not sufficient to infer the remote signal address or function bytes. The user manual independently confirms that the main unit has a remote sensor section and that the RM-SRCEZ38A remote operates it from up to approximately 7 m.
13. A further exact-model search of indexed sources for EM78P156E / TCC760 did not surface an RC-EZ38S firmware image, source listing, or RM-SRCEZ38A command table. This remains an evidence gap, not proof that no such material exists elsewhere.

## Decision

- Keep `carrierFrequencyHz: 37900` as protocol reference metadata only.
- Keep the active profile's `commands` map empty and every button command unverified.
- Do not copy codes from another JVC audio device based only on brand or carrier frequency.
- Do not mark any key as verified until its custom/address code and button data are identified for this exact unit and the signal is validated against the RC-EZ38S.

## Sources

- JVC RC-EZ38S instruction manual, remote control unit: https://www.manualslib.com/manual/2539971/Jvc-Rc-Ez38s.html?page=9
- Official JVC physical specification for infrared remote control: https://support.jvc.com/consumer/support/documents/RemoteCodes.pdf
- Official JVC RC-EZ38 archived product page: https://support.jvc.com/consumer/product.jsp?archive=true&modelId=MODL027840&page=2&pathId=36
- JVC RC-EZ38S service manual No. 28228 (October 2006), including block diagram and schematics: https://device.report/m/b6788c5b128600907d1837655f2d68e0e75951e3c839a7304e01d9943959d716.pdf
- JVC RC-EZ38S user manual, confirming remote sensor section and approximate 7 m operating distance: https://www.manualslib.com/manual/2539971/Jvc-Rc-Ez38s.html?page=9
- JVC RC-EZ35S manual showing a different remote model RM-SRCEZ35J: https://www.manualslib.com/manual/83100/Jvc-Rc-Ez35s.html
- Flipper-IRDB project: https://github.com/joshakabulldog/Flipper-IRDB
- LIRC supported remote controls and database overview: https://www.lirc.org/

## Next research path

Continue checking JVC portable audio families and archived remote-code sources for a documented relationship to RM-SRCEZ38A. Do not treat RC-EZ35S / RM-SRCEZ35J codes as compatible without proof of shared custom and data codes. If no exact profile is found, a real capture source is required (for example, a compatible IR learner or borrowed matching remote); the user's phone IR blaster is transmit-only and cannot learn the missing values.
