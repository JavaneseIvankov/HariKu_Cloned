# Profile Page Implementation Plan

> **For agentic workers:** Execute steps sequentially. Do not implement teammate-owned feature pages. Keep this plan/spec local; do not commit AI-generated superpowers docs.

**Goal:** Add upstream-matched Profile UI with current-user details and working logout, without implementing other menu destinations.

**Architecture:** Add a small profile presentation module using existing GetCurrentUserUseCase and LogoutUseCase. Register a Profile destination in the parent NavHost and provide a narrowly scoped way to open it from Home. Keep profile data access in the existing auth domain contract; do not add Firestore access from Compose.

**Tech Stack:** Kotlin, Jetpack Compose Material3, Navigation Compose, Koin, Firebase Auth domain use cases, JUnit.

---

## Files to inspect before edits

- Clone: app/src/main/java/com/hariku/MainActivity.kt — parent routes, current Home, logout wiring.
- Clone: app/src/main/java/com/hariku/feature/auth/domain/model/AuthUser.kt, GetCurrentUserUseCase.kt, LogoutUseCase.kt, AuthModule.kt — existing profile/logout contract.
- Clone: app/src/main/java/com/hariku/HariKuApp.kt — Koin module registration.
- Upstream: /home/arundaya/AndroidStudioProjects/hariku/app/src/main/java/com/hariku/feature_profile/presentation/ProfileScreen.kt and ProfileScreenViewModel.kt.
- Assets: inspect existing local drawables; do not copy unrelated feature assets.

## Task 1: Add profile presentation state and tests

**Files:**
- Create app/src/main/java/com/hariku/feature/profile/presentation/ProfileViewModel.kt
- Create app/src/test/java/com/hariku/feature/profile/presentation/ProfileViewModelTest.kt

- [ ] Write tests first: ViewModel exposes current AuthUser returned by GetCurrentUserUseCase, including null; logout invokes LogoutUseCase. Test public ViewModel behavior with small fakes, not Koin or Firebase internals.
- [ ] Run focused test: ./gradlew :app:testDebugUnitTest --tests 'com.hariku.feature.profile.presentation.ProfileViewModelTest' --no-daemon --console=plain. Expected initial failure is missing ProfileViewModel; fix test compilation errors until red is behavioral.
- [ ] Implement minimal ProfileViewModel with currentUser property and logout method, no repository or Firestore layer.
- [ ] Re-run same test; expect all focused tests pass.

## Task 2: Add Profile screen matching upstream

**Files:**
- Create app/src/main/java/com/hariku/feature/profile/presentation/ProfileScreen.kt
- Create app/src/main/java/com/hariku/feature/profile/di/ProfileModule.kt
- Modify app/src/main/java/com/hariku/HariKuApp.kt

- [ ] Port upstream structure: white top bar/back affordance/title; light-gray surface; profile card with circular photo/fallback, name/email; menu cards Notifikasi, Dark Mode, Ketentuan Pengguna, Bantuan, Keluar. Adapt AuthUser and use existing local assets or safe icon fallback.
- [ ] Make only Keluar functional. Other rows remain inert. Screen gets explicit nav/logout callbacks and remains previewable.
- [ ] Register profileModule with ProfileViewModel dependencies GetCurrentUserUseCase and LogoutUseCase; include module in HariKuApp.
- [ ] Run ./gradlew :app:compileDebugKotlin :app:testDebugUnitTest --no-daemon --max-workers=2 --console=plain; expect BUILD SUCCESSFUL and all unit tests pass.

## Task 3: Integrate Profile route and Home entry point narrowly

**Files:**
- Modify app/src/main/java/com/hariku/MainActivity.kt
- Modify ProfileScreen.kt only if callback contract needs adjustment.

- [ ] Register Routes.Profile.route in parent NavHost and pass logout callback. Logout through LogoutUseCase, navigate to Routes.Login.route, clear authenticated stack; do not directly call FirebaseAuth from screen.
- [ ] Add small Home profile affordance that navigates to Routes.Profile.route. Keep existing SOS affordance; do not alter MainScaffold tabs or teammate feature pages.
- [ ] Run ./gradlew :app:compileDebugKotlin :app:testDebugUnitTest --no-daemon --max-workers=2 --console=plain; expect BUILD SUCCESSFUL, 0 failures.
- [ ] Live verify with Revyl: Home → Profile, account values and five menu labels, Keluar → Login, Back does not reopen authenticated pages. Use authorized test credentials; don't create/delete unrelated Firebase data.
- [ ] Inspect git diff --name-only. Source changes should be limited to MainActivity, HariKuApp, new feature/profile files and profile tests. No colleague-owned feature implementation changes. Do not stage/commit docs/superpowers.

## Acceptance

- Profile layout matches upstream structure and labels.
- Current auth profile renders safely with missing/null fields.
- Only Keluar has behavior; other menu rows stay inert.
- Logout returns to Login and clears authenticated back stack.
- Home opens Profile without changing tab shell or colleague pages.
- Build/tests pass; Revyl confirms profile/logout flow.
- No AI-generated docs committed or staged.
