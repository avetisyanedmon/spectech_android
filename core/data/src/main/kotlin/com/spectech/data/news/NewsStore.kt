package com.spectech.data.news

import com.spectech.domain.error.ApiError
import com.spectech.domain.model.NewsItem
import com.spectech.network.endpoints.NewsApi
import com.spectech.network.http.ApiClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * In-memory cache of the public news feed. Mirrors iOS `NewsStore`. Held as
 * a singleton so navigation between tabs doesn't refetch — pull-to-refresh
 * is the only path to a live reload.
 *
 * No persistence: the feed is read-only and small, and a stale list is fine
 * to recompute from the network on cold start. If we ever want offline news
 * we'll layer DataStore here without changing the public API.
 */
@Singleton
class NewsStore @Inject constructor(
    private val api: ApiClient,
) {

    private val _items = MutableStateFlow<List<NewsItem>>(emptyList())
    val items: StateFlow<List<NewsItem>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Serialises concurrent fetches (e.g. tab-switch + pull-to-refresh) so we
    // don't race on the StateFlows mid-flight.
    private val fetchMutex = Mutex()

    suspend fun fetch() {
        fetchMutex.withLock {
            _isLoading.value = true
            try {
                val envelope = api.send<List<NewsItem>>(NewsApi.Fetch)
                _items.value = envelope.data
                _error.value = null
            } catch (e: ApiError) {
                Timber.tag(TAG).w(e, "News fetch failed")
                _error.value = e.message
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "News fetch failed")
                _error.value = e.localizedMessage ?: e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    companion object {
        private const val TAG = "NewsStore"
    }
}
