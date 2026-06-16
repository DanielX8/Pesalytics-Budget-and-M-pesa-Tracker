# Release Build Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Pesalytics release APK animation-smooth by adding a baseline profile, enabling R8 full mode, annotating unstable Compose types, and eliminating per-frame allocations in DashboardScreen.

**Architecture:** Three layers of fix applied in sequence — build config first (baseline profile + R8 flags), then compile-time type stability annotations, then targeted code fixes in the Dashboard composable. Each task ends with a compile check so failures are caught early.

**Tech Stack:** Kotlin 2.0, Jetpack Compose (Kotlin Compose plugin), Android Gradle Plugin, Room, R8/ProGuard, `profileinstaller:1.3.1`

---

## File Map

| File | Change |
|---|---|
| `app/build.gradle.kts` | Add `isShrinkResources`, `profileinstaller` dep, `composeCompiler {}` block |
| `gradle.properties` | Add `android.enableR8.fullMode=true` |
| `app/src/main/baseline-prof.txt` | **New** — ART pre-compilation class list |
| `app/src/main/java/com/example/ui/screens/PesaViewModel.kt` | `@Immutable` on `HomeUiState` (line 43) |
| `app/src/main/java/com/example/model/Models.kt` | `@Immutable` on `Transaction` (line 27) |
| `app/src/main/java/com/example/ui/screens/DashboardScreen.kt` | Hoist `currencyFormat`/`timeFormat`, add `key` to `item {}` calls |

---

## Task 1: Build Configuration

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `gradle.properties`
- Create: `app/src/main/baseline-prof.txt`

- [ ] **Step 1: Add `isShrinkResources` to the release build type in `app/build.gradle.kts`**

Find the `release` block (currently lines 41–46) and add one line:

```kotlin
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true     // ← ADD THIS LINE
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("debug")
    }
```

- [ ] **Step 2: Add the `composeCompiler {}` block to `app/build.gradle.kts`**

Insert this block directly after the closing `}` of the `android {}` block (after line 60, before the `secrets {}` block at line 62):

```kotlin
composeCompiler {
    enableStrongSkippingMode = true
}
```

- [ ] **Step 3: Add `profileinstaller` dependency to `app/build.gradle.kts`**

Inside the `dependencies {}` block, add after the `billing-ktx` line (currently line 108):

```kotlin
  implementation("com.android.billingclient:billing-ktx:7.0.0")
  implementation("androidx.profileinstaller:profileinstaller:1.3.1")  // ← ADD THIS LINE
```

- [ ] **Step 4: Add R8 full mode flag to `gradle.properties`**

Append to the end of `gradle.properties`:

```properties
android.enableR8.fullMode=true
```

- [ ] **Step 5: Create `app/src/main/baseline-prof.txt`**

Create this file at exactly this path: `app/src/main/baseline-prof.txt`

The Compose BOM already ships baseline profiles for the animation framework itself. This file only needs to list the app's own composable entry points so ART pre-compiles them too:

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

Each line uses the JVM internal name format (`L` prefix, `/` separators, `;` suffix). These map directly to the `package com.pesalytics.*` declarations in the Kotlin source files.

- [ ] **Step 6: Verify the build still compiles**

Run from the project root (`C:\Users\Dlein\OneDrive\Documents\OB\XF Vault\Pesalytics Budget and M-pesa Tracker`):

```
.\gradlew compileDebugKotlin
```

Expected output ends with: `BUILD SUCCESSFUL`

If you see `Unresolved reference: enableStrongSkippingMode`, your Kotlin Compose plugin version doesn't support this property name. Replace the `composeCompiler` block with nothing (remove it) and instead add to `gradle.properties`:
```properties
kotlin.compose.enableStrongSkippingMode=true
```
Then re-run the compile check.

- [ ] **Step 7: Commit**

```
git add app/build.gradle.kts gradle.properties app/src/main/baseline-prof.txt
git commit -m "build: add baseline profile, R8 full mode, shrinkResources, strong skipping"
```

---

## Task 2: Stable Type Annotations

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/PesaViewModel.kt:43`
- Modify: `app/src/main/java/com/example/model/Models.kt:27`

**Why:** `HomeUiState` contains `List<>` and `Map<>` fields. Kotlin `List` and `Map` are mutable interfaces — Compose treats them as unstable and re-runs the entire `DashboardScreen` on every StateFlow emission, even during animations. `@Immutable` tells the Compose compiler "this type's public properties never change after construction — skip recomposition if the reference is the same." The ViewModel always creates a new `HomeUiState` copy; it never mutates fields in place, so `@Immutable` is semantically correct.

- [ ] **Step 1: Add `@Immutable` to `HomeUiState` in `PesaViewModel.kt`**

Add the import and annotation. The class currently starts at line 43:

```kotlin
import androidx.compose.runtime.Immutable   // ADD — after existing imports

@Immutable                                  // ADD — directly above the data class
data class HomeUiState(
    val transactions: List<Transaction> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val currentBalance: Double = 0.0,
    val isBalanceVisible: Boolean = true,
    val currentBudgetLimit: Double = 0.0,
    val hasBudget: Boolean = false,
    val budgets: List<com.pesalytics.model.Budget> = emptyList(),
    val categorySpent: Map<String, Double> = emptyMap()
)
```

- [ ] **Step 2: Add `@Immutable` to `Transaction` in `Models.kt`**

`Transaction` fields are all `val` primitives and `String` — the Compose compiler should infer stability, but R8 full mode (enabled in Task 1) can break inference by rewriting Kotlin metadata. Making it explicit guarantees it survives minification.

Add the import and annotation. The class currently starts at line 27:

```kotlin
import androidx.compose.runtime.Immutable   // ADD — after existing imports

@Immutable                                  // ADD — directly above @Entity
@Entity(
    tableName = "transactions",
    indices = [Index(value = ["remoteRef", "isFeeTransaction"], unique = true)]
)
@Serializable
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    // ... rest of fields unchanged ...
)
```

Note: `@Immutable` is a Compose runtime annotation only — it has no effect on Room's behaviour. Room reads `@Entity` and `@PrimaryKey` independently.

- [ ] **Step 3: Verify compile**

```
.\gradlew compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```
git add app/src/main/java/com/example/ui/screens/PesaViewModel.kt
git add app/src/main/java/com/example/model/Models.kt
git commit -m "perf: mark HomeUiState and Transaction @Immutable for Compose skip optimization"
```

---

## Task 3: DashboardScreen Composition Fixes

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/DashboardScreen.kt`

- [ ] **Step 1: Add two top-level format vals at the top of `DashboardScreen.kt`**

Insert these two lines directly after the last `import` statement and before the first `@OptIn` or `@Composable` annotation (around line 75, before the `@OptIn(ExperimentalMaterial3Api::class)` line):

```kotlin
private val currencyFormat = java.text.NumberFormat.getInstance().apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}
private val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
```

Both are created once when the class is first loaded (on the UI thread), then reused. `NumberFormat` and `SimpleDateFormat` are not thread-safe, but these are only ever called from Compose composables which run on the main thread — this is safe.

- [ ] **Step 2: Replace `formatCurrency()` body to use the hoisted val**

Find `formatCurrency` at lines 1049–1054:

```kotlin
// BEFORE
fun formatCurrency(amount: Double): String {
    return NumberFormat.getInstance().apply { 
        minimumFractionDigits = 2
        maximumFractionDigits = 2 
    }.format(amount)
}
```

Replace with:

```kotlin
// AFTER
fun formatCurrency(amount: Double): String = currencyFormat.format(amount)
```

The `NumberFormat` import at the top of the file (`import java.text.NumberFormat`) can be removed if no longer used elsewhere — check with a compile run.

- [ ] **Step 3: Replace the `SimpleDateFormat` inside `TransactionItem`**

Find lines 1003–1004 inside `TransactionItem`:

```kotlin
// BEFORE
val format = SimpleDateFormat("HH:mm", Locale.getDefault())
val timeStr = format.format(Date(transaction.timestamp))
```

Replace with:

```kotlin
// AFTER
val timeStr = timeFormat.format(Date(transaction.timestamp))
```

- [ ] **Step 4: Add `key` parameters to every `item {}` call in the Dashboard `LazyColumn`**

The `LazyColumn` starts at line 387. Replace all seven `item {` / `item(` calls with keyed versions. The full set of changes inside the `LazyColumn` block:

```kotlin
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "greeting") {            // ← was: item {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                    // ... unchanged content ...
                }
            }

            item(key = "month-selector") {      // ← was: item {
                // ... unchanged content ...
            }

            item(key = "hero-card") {           // ← was: item {
                Box(modifier = Modifier.animateItem()) {
                    HeroCard(uiState = uiState, onToggleVisibility = { viewModel.toggleBalanceVisibility() })
                }
            }

            item(key = "quick-nav") {           // ← was: item {
                Row(
                    modifier = Modifier.fillMaxWidth().animateItem(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // ... QuickNavButtons unchanged ...
                }
            }

            if (uiState.hasBudget) {
                item(key = "budget-progress") { // ← was: item {
                    // ... budget card unchanged ...
                }
            }

            item(key = "recent-header") {       // ← was: item {
                Row(
                    modifier = Modifier.fillMaxWidth().animateItem(),
                    // ...
                }
            }

            item(key = "recent-content") {      // ← was: item {
                AnimatedContent(
                    // ... unchanged ...
                )
            }
        }
```

Only the `item(` opening line changes — all content inside each block stays exactly the same.

- [ ] **Step 5: Verify compile**

```
.\gradlew compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`

If you see unused import warnings for `NumberFormat` or `SimpleDateFormat`, remove those import lines at the top of `DashboardScreen.kt`.

- [ ] **Step 6: Commit**

```
git add app/src/main/java/com/example/ui/screens/DashboardScreen.kt
git commit -m "perf: hoist format objects and key LazyColumn items in DashboardScreen"
```

---

## Task 4: Build Release APK and Verify

**Files:** None modified — build only.

- [ ] **Step 1: Build the release APK**

```
.\gradlew assembleRelease
```

Expected output ends with:
```
BUILD SUCCESSFUL in Xs
```

The APK is produced at: `app/build/outputs/apk/release/app-release.apk`

- [ ] **Step 2: Copy to the `release/` folder**

```powershell
Copy-Item "app\build\outputs\apk\release\app-release.apk" `
          "release\pesalytics-release-v1.4.0-optimized.apk"
```

- [ ] **Step 3: Install on a physical device and verify animations**

Connect an Android device via USB (or use `adb` over Wi-Fi). Install:

```
adb install -r release\pesalytics-release-v1.4.0-optimized.apk
```

Check each animation that was previously sluggish:

| Animation | How to trigger | Expected |
|---|---|---|
| Hero card balance crossfade | Tap the eye icon | Smooth fade + slide, no stutter |
| Month chip selection | Tap any month chip | Instant highlight, no jank |
| `animateItem()` on hero/quick-nav | First launch with no transactions, then sync | Cards slide in smoothly |
| Budget card appear/disappear | Set then clear a budget limit | Smooth slide, no layout flash |
| LazyColumn scroll | Scroll through transaction list | 60fps, no GC pause hitches |
| Spring press on `clickableScale` buttons | Press and hold Settings/Subscription cards | Smooth scale-down spring |
| Screen navigation | Navigate between tabs | No frame drop on entry/exit |

- [ ] **Step 4: Commit release artifact reference**

```
git add release/pesalytics-release-v1.4.0-optimized.apk
git commit -m "release: v1.4.0-optimized — smooth animations, baseline profile, R8 full mode"
```

---

## Troubleshooting

**`composeCompiler { enableStrongSkippingMode = true }` fails to compile:**
The Kotlin Compose plugin version in this project doesn't expose this property. Remove the `composeCompiler {}` block and add to `gradle.properties` instead:
```properties
kotlin.compose.enableStrongSkippingMode=true
```

**R8 strips something it shouldn't after enabling full mode:**
Add a keep rule to `app/proguard-rules.pro`. The most common culprits with full mode:
```proguard
-keep class com.pesalytics.** { *; }
```
If that's too broad, narrow it to the failing class shown in the crash log.

**`profileinstaller` version conflict:**
If Gradle reports a dependency conflict, align the version with your Compose BOM. Check the BOM's bill of materials for the `profileinstaller` version it pins and use that instead of `1.3.1`.
