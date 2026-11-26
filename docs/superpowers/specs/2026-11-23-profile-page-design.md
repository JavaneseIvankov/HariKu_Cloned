# Profile Page Design

**Date:** 2026-11-23  
**Scope:** HariKu_Cloned, task T51

## Goal

Port upstream Profile page UI while integrating it safely with clone authentication. Only logout is functional in this scope; no colleague-owned feature pages are implemented or modified.

## Existing context

- Clone has AuthRepository.getCurrentUser() and LogoutUseCase, but no Profile screen or profile feature module.
- Sign-up writes user fields to Firestore Users/{uid} and sets Firebase Auth display name.
- Existing Home logout currently calls FirebaseAuth directly from MainActivity.
- Upstream ProfileScreen displays current user photo/name/email and menu rows for Notifications, Dark Mode, Terms, Help, and Logout. Its other menu actions are placeholders; logout navigates to upstream AuthGraph.
- Clone has direct Login/Register routes, not an active AuthGraph route. Logout must return to Login and clear the authenticated back stack.
- Profile is not a destination in the current parent NavHost. Home access/integration requires a narrow route change in shared MainActivity and coordination is tracked in T43.

## Design

### Screen and data

Add a feature/profile presentation screen modeled after upstream layout. Display photo when available, otherwise a local placeholder; display Firebase Auth display name/email with safe fallbacks. Use existing auth domain API rather than adding a new auth data contract or querying Firestore directly from UI. getCurrentUser() is synchronous and already returns the auth user; use that contract for initial implementation.

### Menu behavior

Keep the upstream menu appearance and labels: Notifikasi, Dark Mode, Ketentuan Pengguna, Bantuan, Keluar. Only Keluar is actionable. Other entries remain visual-only; do not add notification settings, theme behavior, legal/help routes, or their pages.

### Logout and navigation

Profile invokes LogoutUseCase, then returns to Routes.Login while clearing the authenticated navigation stack. Ensure stale authenticated destinations cannot be reached with Back. Keep Profile navigation entry narrow and avoid restructuring the tab shell or parent graph beyond registering Routes.Profile and a Home entry point if needed for verification.

### Ownership boundary

Do not implement, alter, or create Mood Analytics, Quick Mood Tracker, SOS, Guided Journaling, Journal, or Statistic pages. Do not change their routes or feature data contracts. Do not add PIN, Senses, Template/Prompt, Meditation, or Article scope.

## Failure and edge cases

- Null user, missing display name/email/photo, and placeholder avatar must render without crashing.
- Logout is one-way from authenticated state; clear back stack before displaying Login.
- Non-logout menu rows must not navigate to unimplemented pages or silently change app settings.

## Verification

- Unit tests for profile display fallbacks and logout action if chosen presentation structure permits meaningful tests.
- Run :app:compileDebugKotlin and :app:testDebugUnitTest.
- Revyl/device: open Profile from Home, verify account fields and menu, tap logout, verify Login appears and Back does not reopen authenticated Home/Profile.
- Inspect diff to confirm no colleague-owned feature files/pages changed.

## Alternatives considered

1. Faithful UI, only logout functional (chosen): preserves upstream layout without inventing behavior or encroaching on other features.
2. Minimal profile card and logout only: smaller port but less UI compliance.
3. Implement every menu action: out of scope; several target pages/settings do not exist and ownership is unclear.
