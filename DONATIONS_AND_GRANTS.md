# Support SATNET in Bitcoin: Donations, Grants, and Sponsorships

Date: 2026-04-24
Status: Public funding strategy page

## Why support this project

SATNET is building resilient, user-controlled communication and self-custodial Bitcoin tooling without surveillance advertising, mandatory protocol tolls, or custodial lock-in.

The project goal is simple:

- keep core communication free
- keep wallets and recovery user-controlled
- improve reliability on real Android devices
- support staged, evidence-based rollout instead of hype-based launch claims

Support helps fund public-good engineering work that is difficult to finance through conventional app monetization.

## Bitcoin-only support policy

Public-facing SATNET support guidance should now be Bitcoin-aligned.

That means:

- one-time donations should point to Bitcoin support channels
- recurring sustainers should use Bitcoin-compatible arrangements where practical
- grants, sponsorships, and partnerships should be discussed in Bitcoin-denominated or Bitcoin-settled terms whenever possible
- help pages and public documentation should not direct users to legacy non-Bitcoin payment copy

This matches the product direction of the app itself, which now includes Bitcoin wallet and voucher workflows.

## What Bitcoin support funds

Priority funding areas:

1. **Security and audits**
   - wallet and key-management review
   - cryptography review
   - release hardening

2. **QA and ship readiness**
   - physical device testing
   - 24-hour soak and recovery validation
   - CI and release evidence automation

3. **Localization and accessibility**
   - translations
   - low-end Android support
   - usability improvements for real-world field deployment

4. **Regional pilot operations**
   - documentation for local operators
   - support tooling
   - deployment and incident-response preparation

5. **Open documentation and maintenance**
   - contributor guides
   - architecture docs
   - release notes and public transparency updates

## How we want to fund the work

### 1. Bitcoin donations first

Best-fit community support channels:

- one-time on-chain Bitcoin donations
- Lightning or Bitcoin-based recurring sustainers where available
- sponsor circles for testing, translations, and documentation funded in Bitcoin terms
- mission-aligned sponsorships that do not control the protocol or product direction

Recommended public CTA copy:

> Support open, user-controlled communication and non-custodial Bitcoin infrastructure. Your Bitcoin support helps fund security reviews, device testing, release engineering, localization, and community operations.

### 2. Grants and partnerships second

Best-fit grant themes:

- digital rights and censorship resistance
- humanitarian and community communications resilience
- open-source public infrastructure
- independent security review and release engineering
- accessibility, localization, and low-end device support

Recommended grant framing:

- fund public goods, not extractive lock-in
- strengthen resilience, safety, and deployability
- improve real-world readiness in underserved environments
- preserve self-custody and user freedom
- settle in Bitcoin or define support in Bitcoin-denominated terms whenever practical

### 3. Optional ethical services third

These can be offered only as optional layers around the network:

- deployment support for NGOs, cooperatives, and local operators
- device certification and field QA services
- operator training for agent, merchant, and verifier roles
- hosted relay, directory, or analytics services that are not required for basic user freedom

## Funding models we will not use

We do **not** want to fund the project through:

- surveillance advertising
- behavioral profiling
- selling user data
- mandatory protocol taxes
- custody-based rent extraction
- premium locks on essential wallet, recovery, or communication features
- public donation copy that points users away from the project&apos;s Bitcoin-first model

This matches the project policy in:

- `doc/SATNET_COMMUNITY_SUSTAINABILITY_MODEL.md`
- `app/src/main/java/org/servalproject/satnet/SatnetPolicy.java`
- `app/build.gradle`

## Current readiness note

The repository&apos;s current evidence indicates the project is **not yet ready for unrestricted global public shipping**.

That means support right now helps close the final readiness gap responsibly, including:

- signed release readiness
- physical device coverage
- soak and recovery validation
- security and release evidence
- staged rollout preparation

For the current assessment, see:

- `doc/GLOBAL_PUBLIC_SHIPPING_AND_FUNDING_DECISION.md`
- `doc/SHIP_READINESS_PUNCH_LIST.md`

## Transparency commitments

We should publish, at minimum:

- what funding categories exist
- what Bitcoin donations, grants, and sponsorships are used for
- major completed milestones
- active funding priorities
- any material sponsor constraints or conflicts

Recommended cadence:

- lightweight quarterly funding update
- release-by-release note on what support enabled

## Copy blocks you can reuse

### Short version

> SATNET is supported through Bitcoin donations, Bitcoin-denominated grants, and mission-aligned sponsorships—not ads, tracking, or protocol tolls. Support helps fund security audits, device testing, release engineering, localization, and resilient communications infrastructure.

### Slightly longer version

> We are building open, self-custodial, censorship-resistant communication and Bitcoin infrastructure. We prefer Bitcoin donations and public-interest grants over extractive monetization. Support goes toward audits, QA, accessibility, localization, and staged real-world deployment readiness.

## Fill-in items before publishing publicly

Add your real links here before promoting this page:

- On-chain Bitcoin donation address: `bc1q7jjcz3gvssv7jrqc7c54xmvx2fthrlj8vper5t`
- Lightning / Bitcoin support contact:
- BTC-denominated grants / partnership contact:
- Transparency updates page:
- Security disclosure contact:

### Build-time donation address provisioning

The Android app can display a release-specific on-chain Bitcoin donation address in **Help -> About SATNET and how to support it in Bitcoin**.

Configure at build time:

- `satnet.donations.btc.address` for the on-chain address text
- `satnet.donations.btc.uri` for an optional explicit `bitcoin:` URI
- `satnet.support.url` for the support page button target

Example:

`./gradlew assembleRelease -Psatnet.donations.btc.address=bc1qyouraddresshere -Psatnet.donations.btc.uri=bitcoin:bc1qyouraddresshere -Psatnet.support.url=https://satnet.app/support`

## Bottom line

The recommended public funding strategy is:

- **mostly Bitcoin donations**
- **then Bitcoin-denominated or Bitcoin-settled grants**
- **optional services only where they remain non-coercive and non-custodial**

That keeps the project aligned with its mission while funding the work needed to move from pilot readiness to safer public launch.
