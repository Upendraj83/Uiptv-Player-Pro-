package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CatalogData
import com.example.data.model.Category
import com.example.data.model.Channel
import com.example.data.model.Country
import com.example.data.model.ParentalSettings
import com.example.data.model.ThemePalette
import com.example.data.repository.CatalogRepository
import com.example.data.repository.LocalSettingsRepository
import com.example.data.repository.StreamValidator
import com.example.data.repository.UserProfileRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger

sealed interface CatalogUiState {
  data class Loading(val message: String = "Loading live TV catalog...") : CatalogUiState
  data class Success(
    val channels: List<Channel>,
    val categories: List<Category>,
    val countries: List<Country>,
  ) : CatalogUiState
  data class Error(val message: String) : CatalogUiState
}

sealed interface ChannelHealthScanState {
  data object Idle : ChannelHealthScanState
  data class Scanning(
    val scopeLabel: String,
    val checked: Int,
    val total: Int,
    val removed: Int,
  ) : ChannelHealthScanState
  data class Completed(
    val scopeLabel: String,
    val checked: Int,
    val removed: Int,
  ) : ChannelHealthScanState
  data class Failed(
    val scopeLabel: String,
    val message: String,
  ) : ChannelHealthScanState
}

class TiweeViewModel(
  private val catalogRepository: CatalogRepository,
  private val userProfileRepository: UserProfileRepository,
  private val localSettingsRepository: LocalSettingsRepository,
  val currentUserId: String,
  val userEmail: String?,
  val userDisplayName: String?,
  val userPhotoUrl: String?,
) : ViewModel() {

  private val _catalogState = MutableStateFlow<CatalogUiState>(CatalogUiState.Loading())
  val catalogState: StateFlow<CatalogUiState> = _catalogState.asStateFlow()

  private val _theme = MutableStateFlow(ThemePalette.DARK)
  val theme: StateFlow<ThemePalette> = _theme.asStateFlow()

  private val _favorites = MutableStateFlow<List<String>>(emptyList())
  val favorites: StateFlow<List<String>> = _favorites.asStateFlow()

  private val _recentChannelIds = MutableStateFlow<List<String>>(emptyList())
  val recentChannelIds: StateFlow<List<String>> = _recentChannelIds.asStateFlow()

  private val _allowAdultChannels = MutableStateFlow(false)
  private val _pin = MutableStateFlow<String?>(null)

  val parentalSettings: StateFlow<ParentalSettings> =
    combine(_allowAdultChannels, _pin) { allow, pin ->
      ParentalSettings(allowAdultChannels = allow, pin = pin)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ParentalSettings())

  private val _isCloudSynced = MutableStateFlow(false)
  val isCloudSynced: StateFlow<Boolean> = _isCloudSynced.asStateFlow()

  private val _cloudSyncError = MutableStateFlow<String?>(null)
  val cloudSyncError: StateFlow<String?> = _cloudSyncError.asStateFlow()

  private val _activeChannel = MutableStateFlow<Channel?>(null)
  val activeChannel: StateFlow<Channel?> = _activeChannel.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
  val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()

  private val _sleepRemainingMinutes = MutableStateFlow<Int?>(null)
  val sleepRemainingMinutes: StateFlow<Int?> = _sleepRemainingMinutes.asStateFlow()

  private val _selectedCategoryId = MutableStateFlow<String?>(null)
  val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

  private val _brokenChannelIds = MutableStateFlow<Set<String>>(emptySet())
  val brokenChannelCount: StateFlow<Int> =
    _brokenChannelIds
      .map { it.size }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

  private val _healthScanState = MutableStateFlow<ChannelHealthScanState>(ChannelHealthScanState.Idle)
  val healthScanState: StateFlow<ChannelHealthScanState> = _healthScanState.asStateFlow()

  private var sleepTimerJob: Job? = null
  private var healthScanJob: Job? = null

  val filteredChannels: StateFlow<List<Channel>> =
    combine(_catalogState, _allowAdultChannels, _brokenChannelIds) { state, allowAdult, brokenIds ->
      when (state) {
        is CatalogUiState.Success ->
          state.channels.filter { ch ->
            (allowAdult || !ch.isNsfw) && ch.id !in brokenIds
          }
        else -> emptyList()
      }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val categoryFilteredChannels: StateFlow<List<Channel>> =
    combine(filteredChannels, _selectedCategoryId) { channels, categoryId ->
      if (categoryId.isNullOrBlank()) {
        channels
      } else {
        channels.filter { ch -> ch.categories.any { it.equals(categoryId, ignoreCase = true) } }
      }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val recentlyViewedChannels: StateFlow<List<Channel>> =
    combine(filteredChannels, _recentChannelIds) { channels, recentIds ->
      if (recentIds.isEmpty() || channels.isEmpty()) {
        emptyList()
      } else {
        val byId = channels.associateBy { it.id }
        recentIds.mapNotNull { byId[it] }.take(5)
      }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val favoriteChannels: StateFlow<List<Channel>> =
    combine(filteredChannels, _favorites) { channels, favIds ->
      if (favIds.isEmpty() || channels.isEmpty()) {
        emptyList()
      } else {
        val favSet = favIds.toHashSet()
        channels.filter { it.id in favSet }
      }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  init {
    loadCatalog(forceRefresh = false)
    observeLocalSettings()
    initAndObserveCloudProfile()
  }

  private fun observeLocalSettings() {
    viewModelScope.launch {
      localSettingsRepository.parentalPinFlow.collect { storedPin ->
        _pin.value = storedPin
      }
    }
    viewModelScope.launch {
      localSettingsRepository.brokenChannelIdsFlow.collect { storedBroken ->
        _brokenChannelIds.value = storedBroken
      }
    }
  }

  private fun initAndObserveCloudProfile() {
    viewModelScope.launch {
      try {
        userProfileRepository.ensureProfileExists(userEmail, userDisplayName, userPhotoUrl)
        _isCloudSynced.value = true
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(TAG, "Cloud profile initialization note: ${e.message}", e)
        _cloudSyncError.value = e.localizedMessage
      }

      userProfileRepository
        .observeProfile(currentUserId)
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val rootError = firestoreError ?: error
          Log.w(TAG, "Error observing user profile", rootError)
          _cloudSyncError.value = rootError.message
        }
        .collect { profile ->
          if (profile != null) {
            _isCloudSynced.value = true
            _cloudSyncError.value = null
            _theme.value = ThemePalette.fromValue(profile.theme)
            _favorites.value = profile.favorites
            _recentChannelIds.value = profile.recentlyViewed
            _allowAdultChannels.value = profile.allowAdultChannels
          }
        }
    }
  }

  fun loadCatalog(forceRefresh: Boolean = false) {
    viewModelScope.launch {
      _catalogState.value = CatalogUiState.Loading(
        if (forceRefresh) "Refreshing live IPTV streams..." else "Loading channels, categories & streams..."
      )
      try {
        val data: CatalogData =
          catalogRepository.fetchAndAssembleCatalog(forceRefresh = forceRefresh) { msg ->
            _catalogState.value = CatalogUiState.Loading(msg)
          }
        _catalogState.value =
          CatalogUiState.Success(
            channels = data.channels,
            categories = data.categories,
            countries = data.countries,
          )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Failed to load catalog", e)
        _catalogState.value =
          CatalogUiState.Error(e.localizedMessage ?: "Failed to load IPTV catalog. Check your internet connection.")
      }
    }
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun selectCategory(categoryId: String?) {
    _selectedCategoryId.value = categoryId
  }

  fun playChannel(channel: Channel?) {
    _activeChannel.value = channel
    if (channel != null) {
      val updated = (listOf(channel.id) + _recentChannelIds.value.filter { it != channel.id }).take(30)
      _recentChannelIds.value = updated
      viewModelScope.launch {
        runCatching {
          userProfileRepository.updatePreferences(recentlyViewed = updated)
        }.onFailure { Log.w(TAG, "Failed to sync recents: ${it.message}") }
      }
    }
  }

  fun clearRecentlyViewed() {
    _recentChannelIds.value = emptyList()
    viewModelScope.launch {
      runCatching {
        userProfileRepository.updatePreferences(recentlyViewed = emptyList())
      }.onFailure { Log.w(TAG, "Failed to clear recents: ${it.message}") }
    }
  }

  fun toggleFavorite(channelId: String) {
    val current = _favorites.value
    val next =
      if (channelId in current) {
        current.filter { it != channelId }
      } else {
        (current + channelId).take(200)
      }
    _favorites.value = next
    viewModelScope.launch {
      runCatching {
        userProfileRepository.updatePreferences(favorites = next)
      }.onFailure { Log.w(TAG, "Failed to sync favorites: ${it.message}") }
    }
  }

  fun clearFavorites() {
    _favorites.value = emptyList()
    viewModelScope.launch {
      runCatching {
        userProfileRepository.updatePreferences(favorites = emptyList())
      }.onFailure { Log.w(TAG, "Failed to clear favorites: ${it.message}") }
    }
  }

  fun toggleTheme() {
    val next = if (_theme.value == ThemePalette.DARK) ThemePalette.AMOLED else ThemePalette.DARK
    _theme.value = next
    viewModelScope.launch {
      runCatching {
        userProfileRepository.updatePreferences(theme = next)
      }.onFailure { Log.w(TAG, "Failed to sync theme: ${it.message}") }
    }
  }

  fun setAllowAdultChannels(allow: Boolean) {
    _allowAdultChannels.value = allow
    viewModelScope.launch {
      runCatching {
        userProfileRepository.updatePreferences(allowAdultChannels = allow)
      }.onFailure { Log.w(TAG, "Failed to sync parental setting: ${it.message}") }
    }
  }

  fun setParentalPin(pin: String?) {
    _pin.value = pin
    viewModelScope.launch {
      localSettingsRepository.setParentalPin(pin)
    }
  }

  fun setSleepTimerMinutes(minutes: Int?) {
    sleepTimerJob?.cancel()
    if (minutes == null || minutes <= 0) {
      _sleepTimerMinutes.value = null
      _sleepRemainingMinutes.value = null
      return
    }
    _sleepTimerMinutes.value = minutes
    _sleepRemainingMinutes.value = minutes
    sleepTimerJob =
      viewModelScope.launch {
        val targetTimeMs = System.currentTimeMillis() + minutes * 60_000L
        while (true) {
          val remainingMs = targetTimeMs - System.currentTimeMillis()
          if (remainingMs <= 0L) {
            _activeChannel.value = null
            _sleepTimerMinutes.value = null
            _sleepRemainingMinutes.value = null
            break
          }
          val remainingMins = ((remainingMs + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
          _sleepRemainingMinutes.value = remainingMins
          delay(1000L)
        }
      }
  }

  fun markChannelBroken(channelId: String) {
    if (channelId.isBlank() || channelId in _brokenChannelIds.value) return
    _brokenChannelIds.value = _brokenChannelIds.value + channelId
    viewModelScope.launch {
      localSettingsRepository.addBrokenChannels(listOf(channelId))
    }
  }

  fun markChannelPlayable(channelId: String) {
    if (channelId !in _brokenChannelIds.value) return
    _brokenChannelIds.value = _brokenChannelIds.value - channelId
    viewModelScope.launch {
      localSettingsRepository.removeBrokenChannel(channelId)
    }
  }

  fun restoreBrokenChannels() {
    _brokenChannelIds.value = emptySet()
    _healthScanState.value = ChannelHealthScanState.Idle
    viewModelScope.launch {
      localSettingsRepository.clearBrokenChannels()
    }
  }

  fun scanAndRemoveBrokenChannels(
    channels: List<Channel>,
    scopeLabel: String,
  ) {
    val batch = channels.take(MAX_HEALTH_SCAN_BATCH)
    if (batch.isEmpty()) {
      _healthScanState.value = ChannelHealthScanState.Completed(scopeLabel, 0, 0)
      return
    }
    healthScanJob?.cancel()
    healthScanJob = viewModelScope.launch {
      val total = batch.size
      val checkedCounter = AtomicInteger(0)
      val reachableCounter = AtomicInteger(0)
      val removedCounter = AtomicInteger(0)
      _healthScanState.value = ChannelHealthScanState.Scanning(
        scopeLabel = scopeLabel,
        checked = 0,
        total = total,
        removed = 0,
      )

      val newlyBroken = java.util.Collections.synchronizedList(mutableListOf<String>())
      val client = StreamValidator.createProbeClient()
      val semaphore = Semaphore(HEALTH_SCAN_CONCURRENCY)

      try {
        coroutineScope {
          batch.map { channel ->
            async(Dispatchers.IO) {
              semaphore.withPermit {
                val reachable = StreamValidator.isChannelReachable(client, channel)
                if (reachable) {
                  reachableCounter.incrementAndGet()
                } else {
                  newlyBroken.add(channel.id)
                  removedCounter.incrementAndGet()
                }
                val checked = checkedCounter.incrementAndGet()
                if (checked % 3 == 0 || checked == total) {
                  _healthScanState.value = ChannelHealthScanState.Scanning(
                    scopeLabel = scopeLabel,
                    checked = checked,
                    total = total,
                    removed = removedCounter.get(),
                  )
                }
              }
            }
          }.awaitAll()
        }

        // Guard against wiping channels if device is completely offline
        if (total >= 4 && reachableCounter.get() == 0) {
          _healthScanState.value = ChannelHealthScanState.Failed(
            scopeLabel = scopeLabel,
            message = "Unable to reach any streams (check internet connection). No channels were removed.",
          )
          return@launch
        }

        if (newlyBroken.isNotEmpty()) {
          _brokenChannelIds.value = _brokenChannelIds.value + newlyBroken
          localSettingsRepository.addBrokenChannels(newlyBroken)
        }

        _healthScanState.value = ChannelHealthScanState.Completed(
          scopeLabel = scopeLabel,
          checked = total,
          removed = newlyBroken.size,
        )
      } catch (e: CancellationException) {
        if (newlyBroken.isNotEmpty() && reachableCounter.get() > 0) {
          _brokenChannelIds.value = _brokenChannelIds.value + newlyBroken
          localSettingsRepository.addBrokenChannels(newlyBroken)
        }
        _healthScanState.value = ChannelHealthScanState.Idle
        throw e
      } finally {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
      }
    }
  }

  fun cancelHealthScan() {
    healthScanJob?.cancel()
    healthScanJob = null
    _healthScanState.value = ChannelHealthScanState.Idle
  }

  fun dismissHealthScan() {
    if (_healthScanState.value !is ChannelHealthScanState.Scanning) {
      _healthScanState.value = ChannelHealthScanState.Idle
    }
  }

  private companion object {
    const val TAG = "TiweeViewModel"
    const val HEALTH_SCAN_CONCURRENCY = 16
    const val MAX_HEALTH_SCAN_BATCH = 250
  }
}
