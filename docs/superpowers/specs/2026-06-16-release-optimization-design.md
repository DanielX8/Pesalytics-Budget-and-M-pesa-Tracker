# Release Build Optimization — Design Spec

**Date:** 2026-06-16
**Status:** Approved, ready for implementation
**Scope:** Make the release APK animation-smooth without architectural changes

---

## Problem

The v1.4.0 release APK has noticeably sluggish animations across all screens — hero card balance transitions, LazyColumn scroll, spring press feedback, and screen navigation. Debug builds are smooth. The gap is caused by three compounding issues:

1. **No baseline profile** — ART JIT-compiles the entire Compose animation framework cold on first interaction in release builds. Debug skips R8 so this warm-up doesn't show up there.
2. **Unstable types** — `HomeUiState` contains `List<>` and `Map<>` fields, which Compose treats as unstable. The entire Dashboard recomposes on every state emission even mid-animation.
3. **Per-frame allocations** — `NumberFormat.getInstance()` and `new SimpleDateFormat(...)` are called inside `formatCurrency()` and `TransactionItem` on every recompose, creating GC pressure that causes frame drops during scroll.

---

## Approach: Option B — Build Config + Targeted Code Fixes

No architectural changes. Three targeted areas.

---

## Section 1 — Build Configuration

### `app/build.gradle.kts`

Add `isShrinkResources = true` to the release build type and add the `profileinstaller` dependency:

```kotlin
buildTypes {
    release {
        isCrunchPngs = false
        isMinifyEnabled = true
        isShrinkResources = true           // ADD
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        signingConfig = signingConfigs.getByName("debug")
    }
}

dependencies {
    // ... existing deps ...
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")  // ADD
}
```

### `gradle.properties`

```properties
android.enableR8.fullMode=true     # ADD — aggressive dead-code elimination
```

### `app/build.gradle.kts` — Compose compiler block (add alongside the existing `android {}` block)

This project uses the Kotlin 2.0 Compose compiler plugin (`alias(libs.plugins.kotlin.compose)`), which exposes `composeCompiler {}` — the correct way to enable strong skipping mode:

```kotlin
composeCompiler {
    enableStrongSkippingMode = true   // ADD — skip recompose when params are equal
}
```

### `app/src/main/baseline-prof.txt` (new file)

The Compose BOM ships baseline profiles for the animation framework. This file only needs to list the app's own composable entry points:

```
Lcom/pesalytics/ui/screens/DashboardScreenKt;
Lcom/pesalytics/ui/screens/PolishKt;
Lcom/pesalytics/ui/screens/AnalyticsScreenKt;
Lcom/pesalytics/ui/screens/SettingsScreenKt;
Lcom/pesalytics/ui/screens/SubscriptionScreenKt;
Lcom/pesalytics/ui/screens/OnboardingScreenKt;
Lcom/pesalytics/ui/theme/ThemeKt;
Lcom/pesalytics/ui/screens/PesaViewModel;
```

**How it works:** `profileinstaller` reads this file at first app launch and installs the profile into ART. On subsequent launches (and on devices with Play Store's Dex Metadata), the listed classes are pre-compiled to native code before the user touches anything.

---

## Section 2 — Stable Types

### `app/src/main/java/com/example/ui/screens/PesaViewModel.kt`

`HomeUiState` has `List<Transaction>`, `List<Budget>`, and `Map<String, Double>` fields. Kotlin `List` and `Map` are mutable interfaces — Compose treats them as unstable and re-runs `DashboardScreen` on every emission from any StateFlow, even during animations.

Add `@Immutable` (the ViewModel never mutates `HomeUiState` in place — it always creates a new copy):

```kotlin
import androidx.compose.runtime.Immutable   // ADD

@Immutable                                  // ADD
data class HomeUiState(
    val transactions: List<Transaction> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val currentBalance: Double = 0.0,
    val isBalanceVisible: Boolean = true,
    val currentBudgetLimit: Double = 0.0,
    val hasBudget: Boolean = false,
    val budgets: List<Budget> = emptyList(),
    val categorySpent: Map<String, Double> = emptyMap()
)
```

### `app/src/main/java/com/example/model/Models.kt`

`Transaction` fields are all primitives and String (`val` only), so the Compose compiler should infer stability — but R8 full mode can break inference by renaming metadata. Making it explicit guarantees it survives minification:

```kotlin
import androidx.compose.runtime.Immutable   // ADD

@Immutable                                  // ADD
@Entity(tableName = "transactions", indices = [...])
@Serializable
data class Transaction(...)
```

No changes needed to `BudgetInsights`, `AppNotification`, `Bill`, `Goal`, or `Budget` — their fields are all primitives and String, already inferred stable.

---

## Section 3 — Composition Fixes

### `app/src/main/java/com/example/ui/screens/DashboardScreen.kt`

#### Fix 1 — Hoist format objects

`formatCurrency()` calls `NumberFormat.getInstance()` on every invocation. `TransactionItem` creates `SimpleDateFormat("HH:mm", …)` on every recompose. Both allocate objects mid-frame on the UI thread.

Move both to top-level file vals:

```kotlin
// ADD at the top of DashboardScreen.kt (after imports)
private val currencyFormat = NumberFormat.getInstance().apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}
private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
```

Replace `formatCurrency` body:
```kotlin
// BEFORE
fun formatCurrency(amount: Double): String {
    return NumberFormat.getInstance().apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(amount)
}

// AFTER
fun formatCurrency(amount: Double): String = currencyFormat.format(amount)
```

Replace the `SimpleDateFormat` inside `TransactionItem`:
```kotlin
// BEFORE (line ~1003)
val format = SimpleDateFormat("HH:mm", Locale.getDefault())
val timeStr = format.format(Date(transaction.timestamp))

// AFTER
val timeStr = timeFormat.format(Date(transaction.timestamp))
```

#### Fix 2 — Add keys to LazyColumn items

The Dashboard `LazyColumn` uses `animateItem()` on two items but none of the `item {}` blocks have a `key`. Without keys, when `hasBudget` causes items to appear/disappear, Compose can't track which items moved and falls back to full re-layout, corrupting the animations:

```kotlin
item(key = "greeting")           { /* greeting Column */ }
item(key = "month-selector")     { /* LazyRow month chips */ }
item(key = "hero-card")          { Box(Modifier.animateItem()) { HeroCard(...) } }
item(key = "quick-nav")          { Row(Modifier.animateItem()) { QuickNavButton... } }
if (uiState.hasBudget) {
    item(key = "budget-progress") { /* budget card */ }
}
item(key = "recent-header")      { /* "Recent Activity" row */ }
item(key = "recent-content")     { AnimatedContent(...) { ... } }
```

---

## Files Changed

| File | Change |
|---|---|
| `app/build.gradle.kts` | `isShrinkResources = true`, `profileinstaller` dependency |
| `gradle.properties` | `R8.fullMode`, `strongSkippingMode` |
| `app/src/main/baseline-prof.txt` | New file — ART pre-compilation targets |
| `PesaViewModel.kt` | `@Immutable` on `HomeUiState` |
| `Models.kt` | `@Immutable` on `Transaction` |
| `DashboardScreen.kt` | Hoist format vals, `key` on LazyColumn items |

---

## Out of Scope

- Macrobenchmark-generated baseline profile (Option C — post-launch)
- Flattening the grouped transactions into individual `LazyColumn` items
- `derivedStateOf` refactor in ViewModel
- Any screen other than Dashboard for composition fixes

---

## Verification

After implementation, build release APK with:
```
.\gradlew assembleRelease
```

Install on device and verify:
- Hero card balance animation on eye-toggle: smooth crossfade
- Month chip selection: no frame drop
- LazyColumn scroll through transactions: 60fps
- `animateItem()` on hero card / quick-nav when budget card appears: smooth slide
- Spring press on `clickableScale` buttons: no stutter
