# Changelog

All notable changes to PesaSense are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

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
- **Package rename** — Application ID changed to `com.pesasense.xmqs`.
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
