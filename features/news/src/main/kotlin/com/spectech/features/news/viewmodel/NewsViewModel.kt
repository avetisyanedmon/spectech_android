package com.spectech.features.news.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spectech.data.news.NewsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Thin wrapper around [NewsStore] — the store holds the cached items so
 * navigating away and back doesn't refetch. Pull-to-refresh in the screen
 * goes through [refresh].
 */
@HiltViewModel
class NewsViewModel @Inject constructor(
    private val store: NewsStore,
) : ViewModel() {

    val items = store.items
    val isLoading = store.isLoading
    val error = store.error

    fun loadInitial() {
        // Only kick a fetch on the very first composition; pull-to-refresh
        // covers subsequent reloads.
        if (store.items.value.isNotEmpty()) return
        viewModelScope.launch { store.fetch() }
    }

    fun refresh() {
        viewModelScope.launch { store.fetch() }
    }
}
