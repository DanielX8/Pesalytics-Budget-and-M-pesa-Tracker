# Pesalytics Development Guidelines

These rules MUST be followed every time you make updates or changes to this repository.

## 1. Design Language & UI Components
- **Top Bars**: NEVER use the default Material 3 `TopAppBar`. ALWAYS use the custom `com.pesalytics.ui.components.PesalyticsTopBar` for all screens to maintain consistent branding and navigation.
- **Backgrounds**: Always set the `containerColor` of your `Scaffold` or root layout to `MaterialTheme.colorScheme.background`.
- **Colors**: Use the app's official semantic colors from the theme (e.g., `AccentGreenDark`, `ExpenseRed`, `IncomeGreen`) for text and icons instead of generic or hardcoded colors.
- **Typography & Shapes**: Follow the Material 3 typography guidelines configured in the app's theme. Use standard rounded corners (`RoundedCornerShape`) matching existing cards.
- **Empty States**: Use consistent empty states with a faded icon (`alpha = 0.5f`) and text using `MaterialTheme.colorScheme.onSurfaceVariant`.

## 2. Code Consistency & Architecture
- **Package Names**: Always use `com.pesalytics.*` for imports and references, never `com.example.*`.
- **View Models**: Do not introduce legacy state management flows if a newer Room DB or Flow implementation exists (e.g., use `AppNotificationEntity` via `NotificationDao` instead of in-memory lists).
- **Verification**: Ensure all UI state dependencies (like `collectAsStateWithLifecycle()`) have explicit generic types if inference fails.

## 3. General Updates
- Before introducing a new UI component, check the `com.pesalytics.ui.components` package to see if a reusable component already exists.
- Ensure all new screens are properly registered in `Destinations.kt` and wired up in `MainActivity.kt`.

## 4. Testing & Verification
- When adding new business logic (like recurring bills rollover), always ensure there is a clear way to verify the behavior, and default to writing a small unit test for complex date/math logic.

## 5. Dependency Injection
- Follow the established pattern for providing dependencies (e.g., passing the Repository to the ViewModel via ViewModelProvider/Factory) rather than instantiating classes directly inside the UI layers.
