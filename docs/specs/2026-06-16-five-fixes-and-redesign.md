# Pesalytics — Five Fixes + Settings/Subscription Redesign

**Date:** 2026-06-16
**Status:** Approved, in implementation
**Scope:** 5 requested fixes + approved extra correctness fixes. Roadmap items (AI insights, referral end-to-end) explicitly deferred.

---

## 1. Budget limit editing (Fix 1)

**Root cause:** `Budget` PK is auto-generated `id`; `insertBudget` uses `OnConflictStrategy.REPLACE`. `addOrUpdateBudget` builds `Budget(id=0, …)`, so Room generates a new row each edit instead of replacing. `budgets.find { it.category == "Overall" }` returns the oldest row → edits appear to do nothing; duplicate rows accumulate.

**Fix:**
- `addOrUpdateBudget(category, limit)` looks up the existing budget for `category`+`monthYear` and reuses its `id` (true upsert).
- `MIGRATION_10_11`: de-duplicate existing `budgets` rows (keep newest per category+monthYear), then add a UNIQUE index on `(category, monthYear)` so REPLACE upserts correctly going forward. DB version 10 → 11.
- Remove dead `setBudget()` (duplicate of `addOrUpdateBudget`).
- Redesign the edit field: replace the plain `AlertDialog` with a styled bottom sheet (KES prefix, large number field), used for both global and per-category limit edits.

## 2. Parse all M-PESA SMS (Fix 2)

`syncMpesaSms` loop cap `transactionsList.size < 500 && scannedCount < 1000` removed → entire inbox parsed. Add `isSyncing` + parsed-count state, surfaced as a loading indicator on the Dashboard/Settings sync triggers so large inboxes don't look frozen.

## 3. Settings + Subscription redesign (Fix 3)

Full redesign using the `ui-ux-pro-max` skill, **same brand** (Safaricom-green palette, Space Grotesk, light+dark preserved). Decompose the 882-line `SettingsScreen` monolith into section composables. Redesign `SubscriptionScreen` pricing/comparison/promo/my-subscriptions.

## 4. Onboarding nicknames (Fix 4)

Replace 5-name `funNicknames` + `.random()` with two 50-name lists (masculine + feminine). "Generate" alternates gender each tap and advances through a shuffled order; add a "Previous" control to step back. State: current index + gender + history.

## 5. Needs vs Wants setting (Fix 5)

New **"Needs vs Wants"** sub-screen in Settings listing every known category (transactions + custom rules + defaults) with a Need/Want segmented toggle each. Classification stored in `pesa_prefs` SharedPreferences, exposed as a `StateFlow` (consistent with theme/notif prefs — no new Room table). `NeedsVsWantsCard` reads the saved map; the old keyword list becomes only the default seed for unclassified categories. New nav route `NeedsWants`.

---

## Extra fixes (approved)

- **Correctness:** Settings version footer → `BuildConfig.VERSION_NAME`; wire **Delete All Data** (confirmation dialog → clears DB + prefs); remove false premium claims ("Cloud sync & backup", "Smart categorization algorithms") and replace with accurate offline features.
- **Wire no-op buttons:** Restore Purchases (`subscriptionManager.syncPurchases()`), Rate App (Play listing intent), Share App + Refer a Friend (share intents), Tip Jar (show M-PESA `0719713362`), Privacy Policy + Terms (open URLs from centralized constants in `AppLinks.kt`, placeholders until site deployed).
- **Real category budget data:** compute per-category spent from current-month transactions; replace hardcoded "Best Month"/"Avg Saved" with real 6-month calculations (or hide when no data).

## Version

`versionName` 1.2.1 → 1.3.0 (matches existing CHANGELOG/dev version, not yet shipped), `versionCode` 2 → 3.

## Verification

Build with JBR (`C:\Program Files\Android\Android Studio\jbr`) via `gradlew compileDebugKotlin` / `assembleDebug`; fix all errors until clean.

## Out of scope (deferred)

AI Smart Insights, Goals cap enforcement, double-sided referral end-to-end, Live Support Chat.
