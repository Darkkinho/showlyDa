package com.michaldrabik.ui_settings.sections.general.cases

import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES.TIRAMISU
import androidx.appcompat.app.AppCompatDelegate
import com.michaldrabik.common.Config
import com.michaldrabik.common.Mode
import com.michaldrabik.common.dispatchers.CoroutineDispatchers
import com.michaldrabik.repository.settings.SettingsRepository
import com.michaldrabik.ui_base.common.AppCountry
import com.michaldrabik.ui_base.common.WidgetsProvider
import com.michaldrabik.ui_base.dates.AppDateFormat
import com.michaldrabik.ui_base.notifications.AnnouncementManager
import com.michaldrabik.ui_model.ProgressDateSelectionType
import com.michaldrabik.ui_model.ProgressNextEpisodeType
import com.michaldrabik.ui_model.Settings
import com.michaldrabik.ui_settings.helpers.AppLanguage
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class SettingsGeneralMainCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val settingsRepository: SettingsRepository,
  private val announcementManager: AnnouncementManager,
) {

  suspend fun getSettings(): Settings =
    withContext(dispatchers.IO) {
      settingsRepository.load()
    }

  suspend fun setRecentShowsAmount(amount: Int) {
    check(amount in Config.MY_SHOWS_RECENTS_OPTIONS)
    withContext(dispatchers.IO) {
      val settings = settingsRepository.load()
      settings.let {
        val new = it.copy(myRecentsAmount = amount)
        settingsRepository.update(new)
      }
    }
  }

  suspend fun enableSpecialSeasons(enable: Boolean) {
    val settings = settingsRepository.load()
    settings.let {
      val new = it.copy(specialSeasonsEnabled = enable)
      settingsRepository.update(new)
    }
  }

  fun isMoviesEnabled() = settingsRepository.isMoviesEnabled

  suspend fun enableMovies(enable: Boolean) {
    val newMode = if (!enable) Mode.SHOWS else settingsRepository.mode
    settingsRepository.run {
      isMoviesEnabled = enable
      mode = newMode
    }
    announcementManager.refreshMoviesAnnouncements()
  }

  fun isStreamingsEnabled() = settingsRepository.streamingsEnabled

  fun enableStreamings(enable: Boolean) {
    settingsRepository.run {
      streamingsEnabled = enable
    }
  }

  suspend fun getLanguage(): AppLanguage {
    if (Build.VERSION.SDK_INT >= TIRAMISU) {
      val locales = AppCompatDelegate.getApplicationLocales()
      if (!locales.isEmpty) {
        val locale = locales.get(0)!!.language
        val language = AppLanguage.fromCode(locale)
        if (settingsRepository.language != locale) {
          setLanguage(language)
        }
        return language
      }
    }
    return AppLanguage.fromCode(settingsRepository.language)
  }

  suspend fun setLanguage(language: AppLanguage) {
    settingsRepository.run {
      this.language = language.code
      val unused = AppLanguage
        .values()
        .filter { it.code != Config.DEFAULT_LANGUAGE && it != language }
        .map { it.code }
      clearUnusedTranslations(unused)
      clearLanguageLogs()
    }
  }

  fun getCountry() = AppCountry.fromCode(settingsRepository.country)

  fun setCountry(country: AppCountry) {
    settingsRepository.country = country.code
  }

  fun getProgressType() = settingsRepository.progressNextEpisodeType

  fun setProgressType(type: ProgressNextEpisodeType) {
    settingsRepository.progressNextEpisodeType = type
  }

  fun getDateSelectionType() = settingsRepository.progressDateSelectionType

  fun setDateSelectionType(type: ProgressDateSelectionType) {
    settingsRepository.progressDateSelectionType = type
  }

  fun getProgressUpcomingDays() = settingsRepository.progressUpcomingDays

  fun setProgressUpcomingDays(days: Long) {
    settingsRepository.progressUpcomingDays = days
    if (days == 0L) {
      settingsRepository.filters.progressShowsUpcoming = false
    }
  }

  fun setDateFormat(
    format: AppDateFormat,
    context: Context,
  ) {
    settingsRepository.dateFormat = format.name
    (context.applicationContext as WidgetsProvider).run {
      requestShowsWidgetsUpdate()
      requestMoviesWidgetsUpdate()
    }
  }

  fun getDateFormat() = AppDateFormat.valueOf(settingsRepository.dateFormat)

  fun setTabletsColumns(columns: Int) {
    settingsRepository.viewMode.tabletGridSpanSize = columns
  }

  fun getTabletsColumns(): Int = settingsRepository.viewMode.tabletGridSpanSize
}
