# Changelog

All notable changes to Pesalytics are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [1.4.0] — 2026-06-16 · Fixes, Settings/Subscription Redesign & Polish 🎨

### Fixed
- **Budget limit could not be edited** — editing the overall/category limit silently created
  duplicate `budgets` rows (auto-generated PK + REPLACE), so the displayed value never changed.
  `addOrUpdateBudget` now reuses the existing row's id (true upsert), and **migration 10→11**
  de-duplicates existing rows and adds a unique index on `(category, monthYear)`.
- **SMS parsing was capped** at 500 transactions / 1000 scanned messages. The entire M-PESA
  inbox is now parsed, with a live sync progress indicator (Dashboard + Settings).
- **Settings version footer** was hardcoded to "Version 1.0.0" — now reads `BuildConfig.VERSION_NAME`.
- **"Delete All Data" did nothing** — now wired with a confirmation dialog that clears all tables + prefs.
- **Inaccurate premium claims** removed from the Subscription screen ("Cloud sync & backup",
  "Smart categorization") and replaced with the app's real offline feature set.
- **Per-category budget data was fake** — category "spent" and "Best Month"/"Avg Saved" now
  use real transaction calculations instead of hardcoded placeholders.

### Added
- **Needs vs Wants setting** — a new Settings sub-screen lets you classify each spending category
  as a Need or Want; the Analytics breakdown now respects your choices (keyword list is only a default seed).
- **Onboarding nicknames** — expanded from 5 to 100 names (50 masculine, 50 feminine); "Generate"
  alternates gender each tap and a new "Previous" control steps back through suggestions.
- **Wired previously dead buttons** — Restore Purchases, Rate App, Share App, Refer a Friend, Tip Jar,
  Privacy Policy & Terms of Service (URLs centralized in `AppLinks.kt`).
- Redesigned the **budget limit editor** as a styled bottom sheet (global + per-category).

### Improved
- **Settings & Subscription screens fully redesigned** with premium visual polish and motion matching
  the Dashboard/Analytics language: colored glow shadows, the brand hero gradient on the Plan/Upgrade
  cards, spring press-feedback, and animated sync state. The 882-line Settings monolith was decomposed
  into focused section components.
- **Color consistency** — replaced off-brand slate/indigo status colors and a neon footer accent with
  the brand palette (semantic AccentGreen / WarningOrange / ExpenseRed tokens) across both screens.

### Review-pass fixes (post first build)
- **Duplicate transactions fixed** — removing the SMS cap made full-inbox syncs slow enough that
  overlapping syncs (the Dashboard re-syncs on every visit) could double-insert. Added a guard against
  concurrent syncs, a unique index on `transactions(remoteRef, isFeeTransaction)`, and **migration
  11→12** that de-duplicates any existing rows.
- **Sync feedback** — a short "Syncing…" toast now fires when a sync starts (alongside the top progress line).
- **Needs vs Wants** — centered the title, added a clear Need-vs-Want explainer, and an **Add Category**
  flow (name + Need/Want) so users can classify categories that aren't in their history yet.
- **Settings colors** — the section titles, Plan card "Premium" badge, and the Sync card now use the
  readable hero-card green (the deep forest green is reserved for the Goals/Budget screens). The Premium
  badge is now a white pill so it's clearly visible on the gradient.
- **Onboarding nicknames** are fun finance personas again (Budget Boss, Coin Master, Ledger Lord…),
  still 50/50 with gender alternation + Previous.
- **Notification permission timing** — notification channels are now created lazily (on first
  notification) instead of at app launch, so the notification permission prompt no longer fires
  early during onboarding. Both prompts (SMS + notifications) now appear together after the final
  onboarding slide.

---

## [1.3.0] — 2026-06-09 · Feature Release 🚀

### Added
- **Google Play Billing & Subscriptions** — Full in-app purchase infrastructure with `SubscriptionManager`.
- **Premium Tiers** — Free, Trial, and Premium tiers with Yearly (KES 2,000) and Lifetime (KES 9,999) options.
- **Promo Codes** — System to redeem promo codes for lifetime access, free trials, or tier extensions.
- **Data Export** — Export transactions to CSV or PDF (Premium feature).
- **Data Management** — Added "Delete All Data" option for GDPR compliance.
- **Budget Deletion** — Users can now delete specific budgets in the Budget Planner.

### Improved
- **Accessibility** — Replaced `TrendingUp`/`Down` with `ArrowUpward`/`Downward` and added `semantics` modifiers for screen readers on the Dashboard.
- **Haptic Feedback** — Long-press to copy M-PESA references now triggers a confirming haptic vibration.
- **Settings UI** — Dynamic badges displaying the user's current subscription tier.

---

## [1.2.1] — 2026-06-03 · Patch Release 🛠️

### Bug Fixes

- **Lint / Play Store compliance** — Added `uses-feature android.hardware.telephony required=false`
  to `AndroidManifest.xml`. The app is fully functional on any Safaricom/M-PESA SIM-capable device,
  while non-telephony devices are no longer blocked from the Play Store listing.

- **App startup hang on fresh install (Bug 2)** — The ViewModel `init` block was waiting for
  a non-empty transaction list using `first { isNotEmpty() }`, which suspended indefinitely on a
  brand-new install. Replaced with `collect {}` so the analytics engine initialises the moment the
  first transaction arrives.

- **Bill edits silently reset (Bug 3)** — `updateBill()` was calling `repository.insertBill()`
  instead of `repository.updateBill()`. This caused edits to create a new database row, losing the
  original bill ID and `lastPaidDate`.

- **False budget alerts with wrong amount (Bug 4)** — The daily worker was calling
  `getDailyExpense()` over the full month range (wrong function), producing inflated spend figures.
  Fixed to use `getMonthlyExpense()`. Alerts are now also fully guarded — they only fire when the
  user has actually configured a budget.

- **Deprecated Material icons compiler warnings (Bug 6)** — Replaced all 7 usages of
  `Icons.Filled.ArrowBack`, `Icons.Filled.ReceiptLong`, and `Icons.Filled.TrendingDown` with their
  `Icons.AutoMirrored.Filled.*` equivalents across `BillsScreen`, `BudgetPlannerScreen`,
  `DashboardScreen`, and `FinancialGoalsScreen`.

- **Room migration parameter name mismatch (Bug 7)** — Both `MIGRATION_8_9` and `MIGRATION_9_10`
  used parameter name `database` instead of `db`, mismatching the supertype signature. Renamed to
  eliminate compiler warnings.

- **"Due This Week" total included paid bills (Bug 8)** — The weekly bills summary was summing
  all bills due this week regardless of payment status. Now only unpaid bills contribute to the
  total, so tapping "Mark as Paid" immediately deducts the correct amount from the displayed figure.

- **Monthly report worker date drift (Bug 9)** — The `PeriodicWorkRequest` with a 30-day interval
  drifts by 1–3 days every month (months are 28–31 days). Replaced with a self-rescheduling
  `OneTimeWorkRequest` chain: `MonthlyReportWorker` now computes the exact millisecond delay to
  00:00 on the 1st of next month and re-enqueues itself after each run.

---

## [1.2.0] — 2026-05-28 · Feature Release

### Added
- **Financial Goals** — Savings and debt-payoff goal tracker with progress bars, contributions,
  smart monthly recommendations, and colour/icon personalisation.
- **Weekly & Monthly Report Workers** — Automated push notifications summarising spend vs budget
  each week and full income/expense/savings breakdown each month.
- **Fuliza Overdraft Tracking** — M-PESA Fuliza usage is detected from SMS and surfaced in
  transaction detail sheets with a running outstanding balance.
- **Circular Theme Reveal** — Light/dark mode toggle now animates with a Material You radial
  reveal centred on the toggle button.
- **Notification Centre** — Dropdowns in the top app bar surface all in-app budget alerts,
  weekly summaries, and goal reminders with one-tap dismiss.
- **Frequency Gating** — Prevents duplicate daily/weekly/monthly worker notifications from firing
  multiple times within the same window.

### Improved
- Analytics screen rebuilt with category delta comparisons (month-over-month).
- Bills screen now shows a "Due This Week" summary card above the bill list.
- Budget Planner added per-category limits with progress rings.
- Dashboard hero card upgraded with gradient background, animated balance, and radial glow overlay.

---

## [1.1.0] — 2026-05-26 · Feature Release

### Added
- **PatternEngine** — Analyses transaction history to surface spending patterns, top categories,
  and average daily/monthly spend.
- **Bills Tracker** — Add recurring bills with due-date reminders and "Mark as Paid" workflow.
- **Analytics Screen** — Visualise income vs expense with bar charts and category breakdowns.
- **Package rename** — Application ID changed to `com.Pesalytics.xmqs`.
- Room database migrated to v9 with Fuliza columns; v10 adds `savedAmount` to goals.

### Fixed
- ProGuard/R8 keep rules added to prevent release build crashes on minified builds.

---

## [1.0.0] — 2026-05-24 · Initial Release

- M-PESA SMS parsing and transaction import.
- Dashboard with balance card, recent activity feed, and month selector.
- Manual transaction entry.
- Budget Planner with overall monthly limit.
- Dark / Light theme support.
