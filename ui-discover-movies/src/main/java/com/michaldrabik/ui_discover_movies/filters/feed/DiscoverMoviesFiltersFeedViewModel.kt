package com.michaldrabik.ui_discover_movies.filters.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michaldrabik.repository.settings.SettingsRepository
import com.michaldrabik.ui_base.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import com.michaldrabik.ui_base.viewmodel.ChannelsDelegate
import com.michaldrabik.ui_base.viewmodel.DefaultChannelsDelegate
import com.michaldrabik.ui_discover_movies.filters.feed.DiscoverMoviesFiltersFeedUiEvent.ApplyFilters
import com.michaldrabik.ui_discover_movies.filters.feed.DiscoverMoviesFiltersFeedUiEvent.CloseFilters
import com.michaldrabik.ui_model.DiscoverFeed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class DiscoverMoviesFiltersFeedViewModel @Inject constructor(
  private val settingsRepository: SettingsRepository,
) : ViewModel(),
  ChannelsDelegate by DefaultChannelsDelegate() {

  private val feedOrderState = MutableStateFlow<DiscoverFeed?>(null)
  private val loadingState = MutableStateFlow(false)

  init {
    loadFilters()
  }

  private fun loadFilters() {
    viewModelScope.launch {
      feedOrderState.value = settingsRepository.filters.discoverMoviesFeed
    }
  }

  fun saveFeedOrder(feedOrder: DiscoverFeed) {
    viewModelScope.launch {
      if (feedOrder == feedOrderState.value) {
        eventChannel.send(CloseFilters)
        return@launch
      }
      settingsRepository.filters.discoverMoviesFeed = feedOrder
      eventChannel.send(ApplyFilters)
    }
  }

  val uiState = combine(
    feedOrderState,
    loadingState,
  ) { s1, s2 ->
    DiscoverMoviesFiltersFeedUiState(
      feedOrder = s1,
      isLoading = s2,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
    initialValue = DiscoverMoviesFiltersFeedUiState(),
  )
}
