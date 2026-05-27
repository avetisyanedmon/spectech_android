# 01 — Design Tokens

iOS doesn't use a formal design system — colors and dimensions are
hard-coded inline. Extract them into Compose `Theme` tokens here.

## Colors

| Token | iOS source | Hex |
|---|---|---|
| `brandBlue` | `Color(red: 0.25, green: 0.48, blue: 0.95)` (in `AuthFlow.swift`, `ProfileView.swift`, etc.) | `#407AF2` |
| `successGreen` | "paid deposit" badge | `#34C759` (iOS system green) |
| `warningAmber` | "pending deposit" badge | `#FF9500` (iOS system orange) |
| `destructiveRed` | logout button, error text | `#FF3B30` (iOS system red) |
| `secondaryText` | `.secondary` foreground style | `#8E8E93` (iOS secondary) |
| `surfaceGroupedBackground` | `Color(.systemGroupedBackground)` | `#F2F2F7` light / `#000000` dark |
| `surfaceCard` | `Color(.systemBackground)` | `#FFFFFF` light / `#1C1C1E` dark |
| `surfaceFieldFill` | `Color(.systemGray6)` | `#F2F2F7` light / `#1C1C1E` dark |
| `separator` | `Color(.separator)` | `#3C3C43` @ 36% alpha |

Codify in `core/ui-kit/theme/Color.kt`:

```kotlin
val BrandBlue = Color(0xFF407AF2)
val SuccessGreen = Color(0xFF34C759)
val WarningAmber = Color(0xFFFF9500)
val DestructiveRed = Color(0xFFFF3B30)

val LightColors = lightColorScheme(
    primary = BrandBlue,
    secondary = BrandBlue,
    error = DestructiveRed,
    background = Color(0xFFF2F2F7),
    surface = Color.White,
    onSurface = Color(0xFF1C1C1E),
    onPrimary = Color.White,
    outline = Color(0x5C3C3C43),
)

val DarkColors = darkColorScheme(
    primary = BrandBlue,
    secondary = BrandBlue,
    error = DestructiveRed,
    background = Color(0xFF000000),
    surface = Color(0xFF1C1C1E),
    onSurface = Color.White,
    onPrimary = Color.White,
    outline = Color(0x5C3C3C43),
)
```

## Typography

iOS uses default system font with the following weights/sizes (gleaned
from the source):

| Use case | iOS spec | Compose equivalent |
|---|---|---|
| Headline | `.system(size: 28, weight: .bold)` | `Typography.headlineLarge.copy(fontWeight = FontWeight.Bold)` |
| Title | `.system(size: 20, weight: .semibold)` | `titleLarge` |
| Body | `.body` | `bodyLarge` |
| Subhead | `.subheadline` | `bodyMedium` |
| Footnote | `.footnote` | `bodySmall` |
| Button | `.system(size: 17, weight: .semibold)` | `labelLarge` |
| Caption | `.system(size: 10, weight: .bold)` (badge counters) | `labelSmall` |

Use Material 3's typography scale and customize where the iOS source has
explicit overrides.

## Spacing

iOS uses ad-hoc paddings everywhere. Standardize:

| Token | dp |
|---|---|
| `Space.xs` | 4 |
| `Space.sm` | 8 |
| `Space.md` | 12 |
| `Space.lg` | 16 |
| `Space.xl` | 20 |
| `Space.xxl` | 28 |

Match the iOS values precisely on screens where pixel-perfect parity
matters (auth, profile card). Elsewhere, snap to the token scale.

## Corner radii

| Use | iOS | Android |
|---|---|---|
| Field background | `cornerRadius: 12` | 12 dp |
| Card | `cornerRadius: 16` | 16 dp |
| Brand icon tile | `cornerRadius: 18` | 18 dp |
| Capsule button | `Capsule()` | full height (`50%`) |
| OTP digit | `cornerRadius: 12` | 12 dp |

## Elevation

iOS uses background fills with no shadows on cards. Android Material 3 cards
default to a tonal elevation; use `CardDefaults.cardColors(containerColor =
MaterialTheme.colorScheme.surface)` with `elevation = CardDefaults.cardElevation(0.dp)`
to mimic the iOS flat look.

## Iconography

iOS uses **SF Symbols**. The closest Android equivalent is **Material Icons
Extended** (`androidx.compose.material:material-icons-extended`).

Common mappings (full table in [05-images-and-icons.md](05-images-and-icons.md)):

| SF Symbol | Material Icon |
|---|---|
| `shippingbox.fill` | `Icons.Outlined.Inventory2` |
| `headphones.circle.fill` | `Icons.Outlined.SupportAgent` |
| `bell` | `Icons.Outlined.Notifications` |
| `person.crop.circle` | `Icons.Outlined.AccountCircle` |
| `plus` | `Icons.Default.Add` |
| `slider.horizontal.3` | `Icons.Default.Tune` |
| `mail.stack` | `Icons.Outlined.Inventory2` (no exact match) |
| `text.badge.checkmark` | `Icons.Outlined.AssignmentTurnedIn` |
| `doc.text.magnifyingglass` | `Icons.Outlined.ContentPasteSearch` |
| `door.garage.closed` | (custom drawable — see icons folder) |
| `newspaper` | `Icons.Outlined.Newspaper` |
| `rectangle.portrait.and.arrow.right` | `Icons.AutoMirrored.Outlined.Logout` |
