# SATNET Project Index

**Status:** Current repository snapshot  
**Focus:** Navigation for the docs and source layout that actually exist in this tree

## What this repository contains

- A single Android app with the SATNET launcher, communication hub, Maps, and SATNET tools.
- Core source under `app/src/main/java/org/servalproject/`.
- Current docs centered on `README.md`, `CURRENT-RELEASE.md`, `START_HERE.md`, `QUICK_REFERENCE.md`, `DOCUMENTATION_INDEX.md`, `TECHNICAL_ARCHITECTURE.md`, and `DEPLOYMENT_TESTING_GUIDE.md`.

## Start here

1. `README.md` - current top-level overview.
2. `CURRENT-RELEASE.md` - current release posture and feature summary.
3. `START_HERE.md` - short onboarding guide.
4. `QUICK_REFERENCE.md` - fast feature and command reference.
5. `DOCUMENTATION_INDEX.md` - docs navigation.

## Main app areas

- `org.servalproject` - launcher, hub, and app shell.
- `org.servalproject.messages` - MeshMS conversation screens and helpers.
- `org.servalproject.rhizome` - file sharing and Rhizome storage flows.
- `org.servalproject.satnet` - role setup, wallet, voucher, verifier, merchant, maps, and policy support.
- `org.servalproject.bitcoin` - Bitcoin wallet and transaction tooling.
- `org.servalproject.video` - video call support.
- `org.servalproject.system` - Wi-Fi, hotspot, Bluetooth, and network control.
- `org.servalproject.relay` - relay and fallback networking helpers.

## Notes

- Build-dependent features, especially Lightning, should be treated as conditional rather than universal.
- Historical documents remain in the repository, but this index points to the current starting set.

