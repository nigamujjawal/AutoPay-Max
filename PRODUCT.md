# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Individuals with multiple recurring UPI autopay mandates and digital subscriptions who want one place to see what's recurring, what it costs per month, and to catch (and cancel) things they forgot they signed up for.

## Product Purpose

AutoPayMax detects, tracks, and lets users manage recurring payments (UPI autopay mandates and subscriptions) without manual entry. Success means a user opens the app and immediately sees an accurate, current picture of their recurring spend, with the ability to act (view a mandate, cancel it, get reminded before it charges).

## Positioning

Automatic detection, not manual entry. Most trackers require the user to log every subscription by hand. AutoPayMax sources mandates automatically — currently via Gmail email-sync (the live source; SMS/notification parsing exists but is disconnected/dormant), with sources tagged `GOOGLE_PLAY` / `EMAIL` / `MANUAL` / `INFERRED` — and surfaces a direct cancel path per mandate (deep-link to Play Store for Play-billed subscriptions, in-app status change otherwise).

## Operating Context

- Pre-launch / internal testing — no public store listing yet. Google/Firebase console setup is a known blocker for shipping (per project history), not a design concern, but it means there is no live user base or store screenshots to treat as evidence yet.
- Auth is in-app Google sign-in (Credential Manager + Firebase bridge); users land on the app already signed into a Google account.
- Core loop: Dashboard (total monthly recurring spend + active mandate list) → mandate detail (source, status, cancel) → Passbook (transaction history) → Bills / Reports / Settings / Notifications as supporting surfaces.
- A third-party in-app messaging/campaign SDK (AppStorys) is wired into screens (e.g. Dashboard) for tagging and campaign surfaces; treat its containers as existing integration points, not something to redesign away silently.

## Capabilities and Constraints

- Currency/locale is not hardcoded: a country picker drives a global currency symbol/code preference used throughout the UI (own subscription pricing copy is deliberately excluded from this and stays as authored).
- Mandate has a `source` enum (GOOGLE_PLAY / EMAIL / MANUAL / INFERRED) that must remain visually distinguishable — it already drives different cancellation behavior (Play Store deep-link vs. in-app status-only cancel, no hard delete).
- Voice/TTS reminders exist (male/female voice selection, chime fallback) — any redesign of Settings/notification screens must keep this configurable, not remove it.
- No swipe-to-delete anywhere in the app (removed deliberately in a prior change) — do not reintroduce swipe-to-delete gestures for mandates or transactions.
- No unlicensed logo or sound assets may be added — brand/icon assets must be either user-supplied or from CC0 sources (e.g. Simple Icons); this constrains any new iconography or imagery introduced by the redesign.

## Brand Commitments

- App name: **AutoPayMax** (`app_name` string, package `com.autopaymax`).
- Current brand color is a vivid orange (`#FF7600` family), inherited from a sibling app ("SoundBox") in the same product family — not yet confirmed as a binding identity constraint; open for this redesign to affirm, adjust, or replace as part of establishing a deliberate visual world (see DESIGN.md once written).
- No other confirmed brand voice, tagline, or marketing copy exists yet.

## Evidence on Hand

- No screenshots, testimonials, press, or usage data exist (pre-launch). Do not fabricate any.
- The only "evidence" of a visual system today is the shipped Compose code itself (Material3 default typography, ad hoc inline colors, `PremiumGradientCard` / `PremiumNormalCard` / `StatusBadge` / `AppHeader` / `SettingsItem` in `ui/components/CommonComponents.kt`) — treat this as an incumbent implementation to inspect, not a validated design system.

## Product Principles

1. Automatic beats manual — every design decision should make detected, low-effort tracking feel trustworthy and immediate, not another form to fill in.
2. Money clarity first — recurring spend totals, per-mandate amounts, and status must always be the most legible thing on screen; decoration never competes with a number the user is trying to read.
3. Source and status are load-bearing information, not decoration — how a mandate was detected and whether it's active/pending/cancelled must stay visually distinct across any redesign.
4. Built for a US-primary audience currently expanding from an India-first base — currency/locale must never be assumed or hardcoded in new UI.
5. Pre-launch means no real evidence to lean on — the redesign must not invent testimonials, ratings, or usage claims.

## Accessibility & Inclusion

No product-specific accessibility requirement has been established yet.
