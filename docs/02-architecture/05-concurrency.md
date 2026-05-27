# 05 — Concurrency

## Core mapping

| iOS construct | Kotlin equivalent |
|---|---|
| `async func foo() throws -> Bar` | `suspend fun foo(): Bar` (throws idiomatically) |
| `await foo()` | `foo()` (already suspending) |
| `Task { … }` (detached from any actor) | `viewModelScope.launch { … }` (in a VM) or `lifecycleScope.launch` (in an Activity) |
| `Task { @MainActor in … }` | `viewModelScope.launch { withContext(Dispatchers.Main) { … } }`, but `viewModelScope` already defaults to `Dispatchers.Main.immediate` |
| `Task.detached(priority: .background)` | `CoroutineScope(SupervisorJob() + Dispatchers.IO).launch` (rare; prefer scoped) |
| `try? await foo()` | `runCatching { foo() }.getOrNull()` |
| `withThrowingTaskGroup` | `coroutineScope { … awaitAll() }` over `async { … }` |
| `Task.sleep(nanoseconds: …)` | `delay(millis)` |
| `@MainActor class Foo` | nothing implicit, but `viewModelScope` already runs on Main; explicitly `withContext(Dispatchers.Main) { … }` if needed |
| `Task` cancellation propagation | Structured concurrency: cancelling the parent cancels children |
| `CancellationError` | `CancellationException` (always re-throw, never swallow) |
| `URLError.cancelled` | `CancellationException` (Ktor maps OkHttp cancellation to this) |

## Where work runs

- **`viewModelScope`** — every API call from a ViewModel. Cancelled when the
  ViewModel is cleared.
- **`Dispatchers.Main.immediate`** — the default for `viewModelScope`; matches
  iOS `@MainActor`. State updates after a network call land back on the main
  thread without an explicit `withContext`.
- **`Dispatchers.IO`** — for `Ktor` calls and the JPEG resize work. The Ktor
  engine handles its own thread pool; you don't usually need to wrap
  `client.get { … }` manually. But the JPEG encode step in `ImageEncoder` is
  CPU-bound, so wrap it explicitly:
  ```kotlin
  suspend fun jpegData(bitmap: Bitmap): ByteArray? = withContext(Dispatchers.Default) {
      // resize + compress loop
  }
  ```
- **`Dispatchers.Default`** — CPU-bound work (resize, hashing). Use for the
  HMAC signature computation if you find it shows up in traces; for normal
  payload sizes it's negligible.

## Concurrent uploads

iOS uses `withThrowingTaskGroup` to upload multiple photos in parallel:

```swift
uploadedPhotoURLs = try await withThrowingTaskGroup(of: String.self) { group in
    for image in images {
        group.addTask { try await equipmentService.uploadPhoto(image) }
    }
    var urls: [String] = []
    for try await url in group { urls.append(url) }
    return urls
}
```

Kotlin equivalent:

```kotlin
suspend fun uploadAll(images: List<Bitmap>): List<String> = coroutineScope {
    images.map { async { equipmentRepo.uploadPhoto(it) } }.awaitAll()
}
```

`awaitAll` throws on the first failure; ongoing children are cancelled.

## Cancellation rules

1. **Never** catch `CancellationException` (`is CancellationException`) without
   re-throwing. iOS does this with `catch is CancellationError { /* ignore */ }`
   — the Kotlin equivalent is:
   ```kotlin
   try { … } catch (e: CancellationException) { throw e } catch (e: Exception) { … }
   ```
2. **Always** use the lifecycle-aware scope (`viewModelScope`, `lifecycleScope`)
   for UI-driven coroutines.
3. Use `Mutex` (or `Channel(capacity = 1)`) if you need to serialize an action
   that must not overlap (e.g. "Pay deposit" button taps). iOS uses
   `isPayInFlight` booleans — port that directly with `mutableStateOf(false)`.

## Pattern: load state machine

```kotlin
suspend fun load(forceRefresh: Boolean) {
    if (!forceRefresh) _state.value = RemoteState.Loading
    try {
        val data = ordersRepo.fetchOrders(OrderScope.MINE)
        _state.value = when {
            data.isEmpty() -> RemoteState.Empty(R.string.no_orders)
            else -> RemoteState.Loaded(data)
        }
    } catch (e: CancellationException) {
        throw e   // pull-to-refresh cancelled mid-flight — leave state alone
    } catch (e: ApiError) {
        _state.value = RemoteState.Failed(e)
    } catch (e: Exception) {
        _state.value = RemoteState.Failed(ApiError(message = e.localizedMessage ?: "Unknown error"))
    }
}
```

This is a direct port of the pattern repeated across iOS:

```swift
do {
    let orders = try await ordersService.fetchOrders(scope: .mine)
    state = orders.isEmpty ? .empty("…") : .loaded(orders)
} catch is CancellationError { /* keep state */ }
  catch let urlError as URLError where urlError.code == .cancelled { /* keep state */ }
  catch let error as APIError { state = .failed(error) }
  catch { state = .failed(APIError(message: error.localizedDescription)) }
```

## Pull-to-refresh

iOS uses SwiftUI's built-in `.refreshable`. Compose has Material 3
`PullToRefreshBox`:

```kotlin
val pull = rememberPullToRefreshState()
PullToRefreshBox(
    isRefreshing = state is RemoteState.Loading,
    onRefresh = { viewModel.load(forceRefresh = true) },
    state = pull,
) {
    // content
}
```
