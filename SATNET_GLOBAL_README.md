# SATNET GLOBAL - current feature reference

SATNET GLOBAL is the unified Android app for local-first communication, SATNET Maps, and SATNET Bitcoin field tools.

## Current app areas

- **Communication hub**: calls, MeshMS messaging, contacts, file sharing, network controls, help, and app sharing.
- **SATNET tools**: role setup, Bitcoin wallet setup/access, voucher flows, verifier tools, and merchant Lightning flows when enabled.
- **Maps**: local-first mapping, temporary markers, saved bookmarks, and Rhizome bookmark exchange.

## Main roles

- **User**: wallet holder and voucher redeemer.
- **Agent**: voucher issuer.
- **Merchant**: Lightning payment recipient when the build enables it.
- **Verifier**: reviews settlement and dispute flows.

## Build-dependent features

- Lightning is controlled by build flags.
- Relay and experimental routing are also build-dependent.
- The current release posture is pilot / pre-production, not public production.

## Platform facts

- Android min SDK: 19
- Android target SDK: 34
- Primary language: Java
- Build system: Gradle

## Current cautions

- Telephony is best effort and not for emergencies.
- Hotspot and legacy ad hoc networking can affect normal Wi-Fi behavior.
- Rhizome shares data to participating devices; it is not a private messenger.
- Wallet recovery material must be stored offline and kept secure.

## Read next

1. `README.md`
2. `CURRENT-RELEASE.md`
3. `QUICK_REFERENCE.md`
4. `TECHNICAL_ARCHITECTURE.md`
5. `DEPLOYMENT_TESTING_GUIDE.md`

