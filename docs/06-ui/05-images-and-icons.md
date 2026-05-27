# 05 — Images & Icons

## Coil setup (replaces SDWebImage)

iOS uses SDWebImageSwiftUI with 50 MB memory / 200 MB disk caps. Replicate
in `Application.onCreate`:

```kotlin
private fun configureCoil() {
    val loader = ImageLoader.Builder(this)
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizeBytes(50L * 1024 * 1024)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizeBytes(200L * 1024 * 1024)
                .build()
        }
        .crossfade(true)
        .build()
    Coil.setImageLoader(loader)
}
```

## Hero placeholder

Add `drawable/placeholder_equipment.xml` — a flat gray rectangle with a
small "image not available" icon centered. Coil shows this while loading.

## SF Symbol → Material Icon map

Used across iOS:

| SF Symbol | Material Icon (Compose `Icons.*`) | Notes |
|---|---|---|
| `shippingbox.fill` | `Icons.Outlined.Inventory2` | Auth hero icon |
| `headphones.circle.fill` | `Icons.Outlined.SupportAgent` | Support top-bar |
| `bell` | `Icons.Outlined.Notifications` | Notifications top-bar |
| `person.crop.circle` | `Icons.Outlined.AccountCircle` | Profile top-bar |
| `plus` | `Icons.Default.Add` | Create order |
| `slider.horizontal.3` | `Icons.Default.Tune` | Filters |
| `mail.stack` | custom drawable | Marketplace tab |
| `text.badge.checkmark` | `Icons.Outlined.AssignmentTurnedIn` | My Bids tab |
| `doc.text.magnifyingglass` | `Icons.Outlined.ContentPasteSearch` | My Orders tab |
| `door.garage.closed` | custom drawable (garage door SVG) | Garage tab |
| `newspaper` | `Icons.Outlined.Newspaper` | News tab |
| `arrow.left` | `Icons.AutoMirrored.Filled.ArrowBack` | Back button |
| `phone` | `Icons.Default.Phone` | Phone action |
| `doc.on.doc` | `Icons.Default.ContentCopy` | Copy phone |
| `rectangle.portrait.and.arrow.right` | `Icons.AutoMirrored.Outlined.Logout` | Logout |
| `xmark.circle.fill` | `Icons.Default.Cancel` | Clear input |
| `chevron.right` | `Icons.AutoMirrored.Default.KeyboardArrowRight` | List disclosure |
| `checkmark.circle.fill` | `Icons.Default.CheckCircle` | Success |
| `exclamationmark.triangle` | `Icons.Outlined.WarningAmber` | Warning |
| `info.circle` | `Icons.Outlined.Info` | Info |
| `pencil` | `Icons.Default.Edit` | Edit |
| `trash` | `Icons.Default.Delete` | Delete |
| `camera.fill` | `Icons.Default.AddAPhoto` | Photo picker |
| `photo.on.rectangle` | `Icons.Outlined.PhotoLibrary` | Gallery |

## Custom drawables to create

| Drawable | Source |
|---|---|
| `ic_garage_tab` | iOS uses `door.garage.closed` — recreate as a vector |
| `ic_marketplace_tab` | iOS uses `mail.stack` — recreate as a stack-of-cards vector |
| `ic_notification` | App notification small icon (single-color silhouette of the app logo) |
| `placeholder_equipment` | Coil placeholder for missing images |

Add as `app/src/main/res/drawable/*.xml` (vector drawables).

## App icon

iOS source: `SpecTechIOS/Resources/Assets.xcassets/AppIcon.appiconset`.

Android needs:
- `ic_launcher.xml` (adaptive icon)
- `ic_launcher_background.xml` (background layer)
- `ic_launcher_foreground.xml` (foreground layer)

The iOS icon is a solid brand-blue square with the SpecTech wordmark.
Replicate as an adaptive icon:
- Background: `BrandBlue` solid color
- Foreground: white wordmark or shipping-box silhouette, padded to fit the
  66dp adaptive safe zone

Generate via Image Asset Studio in Android Studio from the iOS PNG (the
`AppIcon.appiconset` already has a 1024×1024 marketing icon).

## Launch screen

iOS `LaunchScreen.storyboard` shows a centered logo. Android equivalent is
the Splash Screen API (`androidx.core.splashscreen`):

```xml
<style name="Theme.SpecTech.Splash" parent="Theme.SplashScreen">
    <item name="windowSplashScreenBackground">@color/brand_blue</item>
    <item name="windowSplashScreenAnimatedIcon">@drawable/ic_splash_logo</item>
    <item name="postSplashScreenTheme">@style/Theme.SpecTech</item>
</style>
```

In `MainActivity`:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    val splashScreen = installSplashScreen()
    super.onCreate(savedInstanceState)
    splashScreen.setKeepOnScreenCondition { sessionStore.isRestoring.value }
    // …
}
```

The splash stays until the session restore completes — matching iOS's
`LoadingStateView` overlay during `SessionStore.restore()`.

## Image upload

See [07-infrastructure/03-image-encoding-upload.md](../07-infrastructure/03-image-encoding-upload.md).
