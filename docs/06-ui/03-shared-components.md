# 03 — Shared Components

Reference table for every component in `core/ui-kit/components` and the iOS
file it replaces. Implementation sketches below.

| Component | iOS file | Notes |
|---|---|---|
| `LoadingStateView` | `Design/SharedStateViews.swift` | Spinner + caption |
| `EmptyStateView` | `Design/SharedStateViews.swift` | Icon + title + message + optional CTA |
| `ErrorStateView` | `Design/SharedStateViews.swift` | Same shape as empty but with retry |
| `SignInPromptView` | `Shared/Views/SignInPromptView.swift` | Wrapper around above with auth CTA |
| `RussianPhoneField` | `Features/Auth/RussianPhoneFormatter.swift` | Formatter + input |
| `OtpDigitBox` | `Features/Auth/AuthFlow.swift` | 6-box code entry |
| `CitySearchField` | `Shared/Views/CitySearchField.swift` | Google Places autocomplete |
| `RegionPickerField` | `Shared/Views/RegionPickerField.swift` | Russian region list picker |
| `OrderAddressLabel` | `Shared/Views/OrderAddressLabel.swift` | City + address line |
| `EquipmentHeroImage` | `Shared/Views/EquipmentHeroImage.swift` | First photo + status badge |
| `PhoneActionButton` | `Shared/Views/PhoneActionButton.swift` | Call + copy |
| `OrderStatusBadge` | (inline in `OrderCardView.swift`) | Status chip with status-aware color |
| `DepositStatusBadge` | (inline) | Tiny pill in garage card |
| `BrandIconTile` | (inline in `StartAuthView`, `VerifyOTPView`) | 72×72 brand-blue rounded square with white icon |

## LoadingStateView

```kotlin
@Composable
fun LoadingStateView(@StringRes titleRes: Int, paddingValues: PaddingValues = PaddingValues()) {
    Column(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(stringResource(titleRes), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
```

## EmptyStateView

```kotlin
@Composable
fun EmptyStateView(
    @StringRes titleRes: Int,
    @StringRes messageRes: Int? = null,
    @StringRes actionTitleRes: Int? = null,
    icon: ImageVector = Icons.Outlined.Inbox,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, modifier = Modifier.size(48.dp),
             tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
        messageRes?.let {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(it), style = MaterialTheme.typography.bodyMedium,
                 color = MaterialTheme.colorScheme.onSurfaceVariant,
                 textAlign = TextAlign.Center)
        }
        if (onAction != null && actionTitleRes != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAction) { Text(stringResource(actionTitleRes)) }
        }
    }
}
```

## ErrorStateView

```kotlin
@Composable
fun ErrorStateView(error: ApiError, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.ErrorOutline, null, modifier = Modifier.size(48.dp),
             tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.error_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(error.message, style = MaterialTheme.typography.bodyMedium,
             color = MaterialTheme.colorScheme.onSurfaceVariant,
             textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}
```

## SignInPromptView

```kotlin
@Composable
fun SignInPromptView(
    icon: ImageVector,
    @StringRes titleRes: Int,
    @StringRes messageRes: Int,
    onSignIn: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, modifier = Modifier.size(48.dp),
             tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(titleRes), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(messageRes), style = MaterialTheme.typography.bodyMedium,
             color = MaterialTheme.colorScheme.onSurfaceVariant,
             textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onSignIn,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(percent = 50),
        ) {
            Text(stringResource(R.string.sign_in), style = MaterialTheme.typography.titleMedium)
        }
    }
}
```

## CitySearchField (Google Places)

iOS uses MapKit's `MKLocalSearchCompleter` for city autocomplete. Android
equivalent: Google Places SDK with `findAutocompletePredictions`:

```kotlin
class CityAutocompleteState(context: Context) {
    private val client = Places.createClient(context)
    private val token = AutocompleteSessionToken.newInstance()

    var query by mutableStateOf("")
    var predictions by mutableStateOf<List<AutocompletePrediction>>(emptyList())

    suspend fun search(query: String) {
        if (query.isEmpty()) { predictions = emptyList(); return }
        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(token)
            .setQuery(query)
            .setCountries("RU")
            .setTypesFilter(listOf("(cities)"))
            .build()
        val response = client.findAutocompletePredictions(request).await()
        predictions = response.autocompletePredictions
    }
}
```

Compose UI: `OutlinedTextField` + `DropdownMenu` of predictions.

Pre-requisite: enable Places API in Google Cloud, ship the API key in
`AndroidManifest.xml` `<meta-data android:name="com.google.android.geo.API_KEY">`.

## RegionPickerField

A picker over the list of 89 Russian regions (Republics, Krais, Oblasts,
Federal cities, Autonomous Districts/Okrugs). Hard-code the list in
`core/ui-kit/data/RussianRegions.kt`:

```kotlin
val RussianRegions = listOf(
    "Москва", "Санкт-Петербург", "Севастополь",
    "Республика Адыгея", "Республика Алтай", "Республика Башкортостан",
    // … (port from iOS RegionPickerField.swift)
)
```

UI: `ExposedDropdownMenuBox` with a search filter inside.

## EquipmentHeroImage

```kotlin
@Composable
fun EquipmentHeroImage(
    photoUrl: String?,
    depositStatus: DepositStatus?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.aspectRatio(1.5f).clip(RoundedCornerShape(12.dp))) {
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            placeholder = painterResource(R.drawable.placeholder_equipment),
            error = painterResource(R.drawable.placeholder_equipment),
        )
        if (depositStatus == DepositStatus.PAID) {
            DepositBadge(R.string.deposit_paid, color = SuccessGreen, modifier = Modifier
                .align(Alignment.TopEnd).padding(8.dp))
        } else if (depositStatus == DepositStatus.PENDING) {
            DepositBadge(R.string.deposit_pending, color = WarningAmber, modifier = Modifier
                .align(Alignment.TopEnd).padding(8.dp))
        }
    }
}
```

## PhoneActionButton

```kotlin
@Composable
fun PhoneActionButton(phone: String, name: String? = null) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = phone, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = {
            ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
        }) {
            Icon(Icons.Default.Phone, contentDescription = stringResource(R.string.call))
        }
        IconButton(onClick = { clipboard.setText(AnnotatedString(phone)) }) {
            Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.copy))
        }
    }
}
```

## OrderStatusBadge

```kotlin
@Composable
fun OrderStatusBadge(status: OrderStatus) {
    val (bg, fg) = statusColors(status)
    Surface(color = bg, contentColor = fg, shape = RoundedCornerShape(50)) {
        Text(
            stringResource(status.titleRes),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun statusColors(status: OrderStatus): Pair<Color, Color> = when (status) {
    OrderStatus.OPEN        -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
    OrderStatus.PENDING     -> Color(0xFFFFF3E0) to Color(0xFFE65100)
    OrderStatus.ACCEPTED    -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
    OrderStatus.IN_PROGRESS -> Color(0xFFE0F2F1) to Color(0xFF00695C)
    OrderStatus.COMPLETED   -> Color(0xFFE0E0E0) to Color(0xFF424242)
    OrderStatus.CANCELLED   -> Color(0xFFFFEBEE) to Color(0xFFC62828)
    OrderStatus.EXPIRED     -> Color(0xFFFFEBEE) to Color(0xFFC62828)
    OrderStatus.CLOSED      -> Color(0xFFE0E0E0) to Color(0xFF424242)
}
```

(Map source: `SpecTechIOS/Shared/Views/OrderStatusColor.swift` — replicate
the exact light/dark color choices.)
