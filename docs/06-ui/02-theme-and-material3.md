# 02 — Theme & Material 3

## Setup

```kotlin
@Composable
fun SpecTechTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val typography = SpecTechTypography
    val shapes = SpecTechShapes

    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        shapes = shapes,
        content = content,
    )
}
```

The token values are in [01-design-tokens.md](01-design-tokens.md).

## Dark mode

iOS uses `Color(.systemBackground)` and friends so it follows the system
appearance automatically. Match this with `isSystemInDarkTheme()` and let the
user override via Profile (per-app dark mode toggle is intentionally NOT
in the iOS app — leave it on the platform).

## Material 3 components used

| Compose component | Replaces iOS pattern |
|---|---|
| `Scaffold` | `NavigationView`/manual padding |
| `TopAppBar` | `.toolbar { … }` |
| `BottomAppBar` / `NavigationBar` | `TabView` bar |
| `NavigationBarItem` | `Tab(…)` |
| `Card` | inline `RoundedRectangle` + `.fill` |
| `OutlinedTextField` | `TextField` + custom background |
| `FilterChip` | category chips in filter sheets |
| `AssistChip` | status badges |
| `ModalBottomSheet` | `.sheet { … }` |
| `AlertDialog` | `.confirmationDialog` |
| `LinearProgressIndicator` / `CircularProgressIndicator` | `ProgressView()` |
| `LazyColumn` | `ScrollView { LazyVStack { … } }` / `List` |
| `LazyVerticalGrid` | photo grids in equipment detail |
| `HorizontalPager` | photo carousels |
| `PullToRefreshBox` | `.refreshable { … }` |

## Custom components that need to be built

| iOS view | Android equivalent | File |
|---|---|---|
| `LoadingStateView` | `LoadingStateView` composable | `core/ui-kit/components/LoadingStateView.kt` |
| `EmptyStateView` | `EmptyStateView` composable | `core/ui-kit/components/EmptyStateView.kt` |
| `ErrorStateView` | `ErrorStateView` composable | `core/ui-kit/components/ErrorStateView.kt` |
| `OTPDigitBox` | `OtpDigitBox` composable | `features/auth/ui/OtpDigitBox.kt` |
| `RussianPhoneField` | `RussianPhoneField` composable | `core/ui-kit/components/RussianPhoneField.kt` |
| `CitySearchField` | `CitySearchField` composable (Google Places) | `core/ui-kit/components/CitySearchField.kt` |
| `RegionPickerField` | `RegionPickerField` composable | `core/ui-kit/components/RegionPickerField.kt` |
| `OrderAddressLabel` | `OrderAddressLabel` composable | `core/ui-kit/components/OrderAddressLabel.kt` |
| `EquipmentHeroImage` | `EquipmentHeroImage` composable | `core/ui-kit/components/EquipmentHeroImage.kt` |
| `PhoneActionButton` | `PhoneActionButton` composable | `core/ui-kit/components/PhoneActionButton.kt` |
| `SignInPromptView` | `SignInPromptView` composable | `core/ui-kit/components/SignInPromptView.kt` |
| Field style modifier | `Modifier.specTechField()` | `core/ui-kit/theme/Modifiers.kt` |

## Field styling helper

iOS's `FieldStyleModifier`:

```swift
.padding(.horizontal, 16)
.padding(.vertical, 14)
.background(RoundedRectangle(cornerRadius: 12, style: .continuous).fill(Color(.systemGray6)))
```

Compose:

```kotlin
fun Modifier.specTechField(): Modifier = this
    .clip(RoundedCornerShape(12.dp))
    .background(MaterialTheme.colorScheme.surfaceVariant)
    .padding(horizontal = 16.dp, vertical = 14.dp)
```

Use on plain `BasicTextField` wrappers so the look matches iOS without
fighting `OutlinedTextField`'s opinionated visuals.

## Status & category color map

See [06-status-and-category-mapping.md](06-status-and-category-mapping.md)
for the per-status badge color table (from iOS `OrderStatusColor.swift`).

## Edge-to-edge

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { SpecTechTheme { SpecTechApp() } }
}
```

Compose handles inset padding automatically with `Scaffold` since AGP 8.0.
Use `WindowInsets.safeDrawing` if you need custom insets.

## Status bar / nav bar colors

`enableEdgeToEdge()` defaults are fine. If you need a tinted status bar to
match the iOS brand-blue accent above auth views, wrap with
`Modifier.statusBarsPadding()` and a `Surface` of the right color in those
specific screens. iOS uses transparent status bars throughout.
