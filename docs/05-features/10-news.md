# 10 — News

iOS sources:
- `SpecTechIOS/Scene/Tabs/News/NewsView.swift`
- `SpecTechIOS/Scene/Tabs/News/NewsStore.swift`
- `SpecTechIOS/Scene/Tabs/News/Component/NewsCardView.swift`
- `SpecTechIOS/Scene/Tabs/News/Model/NewsItem.swift`

A simple read-only feed available to all users (no auth required).

## Endpoint

`GET /news` → returns `NewsItem[]`.

## Model

```kotlin
@Serializable
data class NewsItem(
    val id: String,
    val title: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    @Contextual val createdAt: Instant,
)
```

## Store

```kotlin
@Singleton
class NewsStore @Inject constructor(private val api: ApiClient) {
    private val _items = MutableStateFlow<List<NewsItem>>(emptyList())
    val items: StateFlow<List<NewsItem>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    suspend fun fetch() {
        _isLoading.value = true
        _error.value = null
        runCatching { api.send<List<NewsItem>>(NewsApi.Fetch).data }
            .onSuccess { _items.value = it }
            .onFailure { _error.value = it.localizedMessage }
        _isLoading.value = false
    }
}
```

## Screen

`LazyColumn` of `NewsCard` items. Each card:
- Hero image via Coil `AsyncImage` (or placeholder if `imageUrl == null`)
- Title (semibold)
- Description (truncated to ~3 lines)
- Date (relative — "2 hours ago" / "3 days ago" using `kotlinx-datetime`)
- If `videoUrl != null` → small overlay icon on the image; tap opens video
  URL in a Chrome Custom Tab

```kotlin
@Composable
fun NewsScreen(viewModel: NewsViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val pull = rememberPullToRefreshState()

    LaunchedEffect(Unit) { if (items.isEmpty()) viewModel.fetch() }

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = { viewModel.fetch() },
        state = pull,
    ) {
        when {
            error != null && items.isEmpty() -> ErrorStateView(
                error = ApiError(message = error!!),
                onRetry = { viewModel.fetch() }
            )
            items.isEmpty() && !isLoading -> EmptyStateView(R.string.news_empty)
            else -> LazyColumn { items(items, key = { it.id }) { NewsCard(it) } }
        }
    }
}
```

## Date formatting

iOS uses `RelativeDateTimeFormatter`. Android equivalent:

```kotlin
fun Instant.relativeFromNow(): String {
    val now = Clock.System.now()
    val diff = now - this
    return when {
        diff < 1.minutes -> "just now"
        diff < 1.hours   -> "${diff.inWholeMinutes} min ago"
        diff < 1.days    -> "${diff.inWholeHours} hr ago"
        diff < 7.days    -> "${diff.inWholeDays} days ago"
        else             -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                              .format(Date(toEpochMilliseconds()))
    }
}
```

Localize the strings via plural resources for the Russian translations.
