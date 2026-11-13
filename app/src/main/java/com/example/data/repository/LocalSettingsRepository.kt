package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tiweeDataStore: DataStore<Preferences> by preferencesDataStore(name = "tiwee_local_settings")

class LocalSettingsRepository(private val context: Context) {
  private val appContext = context.applicationContext

  val parentalPinFlow: Flow<String?> =
    appContext.tiweeDataStore.data.map { prefs -> prefs[KEY_PARENTAL_PIN] }

  val brokenChannelIdsFlow: Flow<Set<String>> =
    appContext.tiweeDataStore.data.map { prefs -> prefs[KEY_BROKEN_CHANNELS].orEmpty() }

  suspend fun setParentalPin(pin: String?) {
    appContext.tiweeDataStore.edit { prefs ->
      if (pin.isNullOrBlank()) {
        prefs.remove(KEY_PARENTAL_PIN)
      } else {
        prefs[KEY_PARENTAL_PIN] = pin
      }
    }
  }

  suspend fun addBrokenChannels(channelIds: Collection<String>) {
    if (channelIds.isEmpty()) return
    appContext.tiweeDataStore.edit { prefs ->
      val current = prefs[KEY_BROKEN_CHANNELS].orEmpty()
      prefs[KEY_BROKEN_CHANNELS] = current + channelIds
    }
  }

  suspend fun removeBrokenChannel(channelId: String) {
    appContext.tiweeDataStore.edit { prefs ->
      val current = prefs[KEY_BROKEN_CHANNELS].orEmpty()
      if (channelId in current) {
        prefs[KEY_BROKEN_CHANNELS] = current - channelId
      }
    }
  }

  suspend fun clearBrokenChannels() {
    appContext.tiweeDataStore.edit { prefs ->
      prefs.remove(KEY_BROKEN_CHANNELS)
    }
  }

  private companion object {
    val KEY_PARENTAL_PIN = stringPreferencesKey("parental_pin")
    val KEY_BROKEN_CHANNELS = stringSetPreferencesKey("broken_channel_ids")
  }
}
