# SATNET GLOBAL Quick Reference

**Current snapshot:** local-first communication app plus SATNET wallet and field tools

## What SATNET currently does

- **Communication**: calls, MeshMS messaging, contacts, file sharing, network controls, help, and app sharing.
- **SATNET tools**: role setup, Bitcoin wallet access, voucher issuance/redemption, verifier review, and merchant Lightning tooling when enabled.
- **Maps**: local-first mapping with temporary markers, secure bookmarks, and optional Rhizome exchange.

## Main roles

- **User**: holds the wallet and redeems vouchers.
- **Agent**: issues vouchers.
- **Merchant**: accepts Lightning payments when the build enables them.
- **Verifier**: reviews disputes and settlement flow.

## Important build facts

- Min SDK 19, target SDK 34.
- Lightning, relay, and routing features are controlled by build flags.
- Current release posture is pilot / pre-production, not public production.

## Main files

- `README.md`
- `CURRENT-RELEASE.md`
- `START_HERE.md`
- `DOCUMENTATION_INDEX.md`
- `SATNET_GLOBAL_README.md`
- `TECHNICAL_ARCHITECTURE.md`

## Common commands

```bash
./gradlew assembleDebug
./gradlew test
./gradlew assembleRelease
```

## Safety notes

- Do not rely on SATNET telephony for emergencies.
- Hotspot and legacy ad hoc networking can affect normal Wi-Fi behavior.
- Rhizome copies shared files to participating devices.

