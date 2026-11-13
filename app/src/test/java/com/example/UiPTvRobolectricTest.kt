package com.example

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Category
import com.example.data.model.Channel
import com.example.data.model.Country
import com.example.data.model.StreamSource
import com.example.ui.ChannelHealthScanState
import com.example.ui.player.PlayerControlsOverlay
import com.example.ui.player.PlayerErrorCard
import com.example.ui.screens.AllChannelsScreen
import com.example.ui.screens.CountryBrowseScreen
import com.example.ui.screens.MenuScreen
import com.example.ui.theme.TiweeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp")
class UiPTvRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private val sampleChannels = listOf(
    Channel(
      id = "dd_news_in",
      name = "DD News",
      country = "IN",
      categories = listOf("news"),
      streams = listOf(StreamSource("https://stream.example.org/ddnews.m3u8", quality = "1080p")),
    ),
    Channel(
      id = "ptv_news_pk",
      name = "PTV News",
      country = "PK",
      categories = listOf("news"),
      streams = listOf(StreamSource("https://stream.example.org/ptv.m3u8", quality = "720p")),
    ),
    Channel(
      id = "btv_world_bd",
      name = "BTV World",
      country = "BD",
      categories = listOf("general"),
      streams = listOf(StreamSource("https://stream.example.org/btv.m3u8", quality = "720p")),
    ),
  )

  private val sampleCountries = listOf(
    Country(code = "US", name = "United States", flag = "🇺🇸"),
    Country(code = "IN", name = "India", flag = "🇮🇳"),
    Country(code = "PK", name = "Pakistan", flag = "🇵🇰"),
    Country(code = "BD", name = "Bangladesh", flag = "🇧🇩"),
  )

  private val sampleCategories = listOf(
    Category(id = "news", name = "News"),
    Category(id = "general", name = "General"),
  )

  @Test
  fun appNameResource_matchesUiPTvPlayerProPlus() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("UiPTv Player Pro+", context.getString(R.string.app_name))
  }

  @Test
  fun menuScreen_displaysSouthAsiaFeaturedCountriesAndDispatchesClick() {
    var selectedCountryCode: String? = null

    composeTestRule.setContent {
      TiweeTheme {
        MenuScreen(
          allChannels = sampleChannels,
          countries = sampleCountries,
          categories = sampleCategories,
          selectedCategoryId = null,
          categoryFilteredChannels = sampleChannels,
          favoriteChannels = emptyList(),
          recentChannels = emptyList(),
          favoriteIds = emptySet(),
          countryCount = sampleCountries.size,
          categoryCount = sampleCategories.size,
          onNavigateCountries = {},
          onNavigateCategories = {},
          onNavigateFavorites = {},
          onNavigateAllChannels = {},
          onSelectCountry = { selectedCountryCode = it.code },
          onSelectCategoryFilter = {},
          onSelectCategory = { _, _ -> },
          onPlayChannel = {},
          onToggleFavorite = {},
          onClearRecentHistory = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("LIVE GLOBAL TV").assertDoesNotExist()
    composeTestRule.onNodeWithTag("menu_transcribe_card").assertDoesNotExist()

    composeTestRule.onNodeWithTag("menu_screen_grid")
      .performScrollToNode(hasTestTag("menu_featured_country_in"))
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("India, Pakistan & Bangladesh").assertIsDisplayed()
    composeTestRule.onNodeWithTag("menu_featured_country_in").assertIsDisplayed()
    composeTestRule.onNodeWithTag("menu_featured_country_pk").assertIsDisplayed()
    composeTestRule.onNodeWithTag("menu_featured_country_bd").assertIsDisplayed()

    composeTestRule.onNodeWithTag("menu_featured_country_pk").performClick()
    composeTestRule.waitForIdle()
    assertEquals("PK", selectedCountryCode)
  }

  @Test
  fun countryBrowseScreen_pinsIndiaPakistanAndBangladesh() {
    var selectedCountry: Country? = null

    composeTestRule.setContent {
      TiweeTheme {
        CountryBrowseScreen(
          countries = sampleCountries,
          channels = sampleChannels,
          onSelectCountry = { selectedCountry = it },
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("country_card_IN").assertIsDisplayed()
    composeTestRule.onNodeWithTag("country_card_PK").assertIsDisplayed()
    composeTestRule.onNodeWithTag("country_card_BD").assertIsDisplayed()

    composeTestRule.onNodeWithTag("country_card_BD").performClick()
    composeTestRule.waitForIdle()
    assertEquals("BD", selectedCountry?.code)
  }

  @Test
  fun allChannelsScreen_rendersChannelsTogglesFavoriteAndShowsScanBanners() {
    var toggledFavoriteId: String? = null
    var scanInvoked = false

    composeTestRule.setContent {
      TiweeTheme {
        AllChannelsScreen(
          channels = sampleChannels,
          categories = sampleCategories,
          selectedCategoryId = null,
          onSelectCategory = {},
          favoriteIds = setOf("dd_news_in"),
          onPlayChannel = {},
          onToggleFavorite = { toggledFavoriteId = it },
          healthScanState = ChannelHealthScanState.Failed(
            scopeLabel = "Live TV",
            message = "No stream responded. Check your internet connection.",
          ),
          onScanBrokenChannels = { _, _ -> scanInvoked = true },
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("channel_health_failed_banner").assertIsDisplayed()
    composeTestRule.onNodeWithTag("channel_card_dd_news_in").assertIsDisplayed()
    composeTestRule.onNodeWithTag("channel_card_ptv_news_pk").assertIsDisplayed()

    composeTestRule.onNodeWithTag("favorite_toggle_ptv_news_pk").performClick()
    composeTestRule.waitForIdle()
    assertEquals("ptv_news_pk", toggledFavoriteId)

    composeTestRule.onNodeWithTag("clean_offline_channels_button").performClick()
    composeTestRule.waitForIdle()
    assertTrue(scanInvoked)
  }

  @Test
  fun playerControlsOverlay_onCompactPortraitWidth_keepsAllControlsVisible() {
    var playPauseClicked = false
    var reloadClicked = false

    composeTestRule.setContent {
      TiweeTheme {
        Box(modifier = Modifier.size(width = 340.dp, height = 640.dp)) {
          PlayerControlsOverlay(
            visible = true,
            title = "DD News",
            subtitle = "IN • News",
            logoUrl = null,
            isLive = true,
            isPlaying = true,
            isBuffering = false,
            isMuted = false,
            volume = 0.8f,
            useFillMode = false,
            isLandscape = false,
            allowFullscreenToggle = true,
            streams = sampleChannels.first().streams,
            sourceIndex = 0,
            showSourcesMenu = false,
            isFavorite = false,
            sleepRemainingMinutes = null,
            onShowSourcesMenuChange = {},
            onSelectSource = {},
            onTogglePlayPause = { playPauseClicked = true },
            onSeekToLiveEdge = {},
            onToggleMute = {},
            onVolumeChange = {},
            onReload = { reloadClicked = true },
            onToggleResizeMode = {},
            onToggleLandscape = {},
            onToggleFavorite = {},
            onBack = {},
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("player_play_pause_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("player_live_edge_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("player_mute_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("player_volume_slider").assertIsDisplayed()
    composeTestRule.onNodeWithTag("player_reload_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("player_resize_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("player_fullscreen_button").assertIsDisplayed()

    composeTestRule.onNodeWithTag("player_play_pause_button").performClick()
    composeTestRule.onNodeWithTag("player_reload_button").performClick()
    composeTestRule.waitForIdle()
    assertTrue(playPauseClicked)
    assertTrue(reloadClicked)
  }

  @Test
  fun playerErrorCard_showsAutoRemovalNoteAndTriggersRetry() {
    var retried = false

    composeTestRule.setContent {
      TiweeTheme {
        PlayerErrorCard(
          title = "Offline Channel",
          sourceCount = 1,
          errorDetail = "HTTP 404 Not Found",
          autoRemovedOnError = true,
          onRetry = { retried = true },
          onBack = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("player_error_card").assertIsDisplayed()
    composeTestRule.onNodeWithText("Stream Offline or Restricted").assertIsDisplayed()
    composeTestRule.onNodeWithText(
      text = "Removed from your active channel list.",
      substring = true,
    ).assertIsDisplayed()

    composeTestRule.onNodeWithTag("player_retry_button").performClick()
    composeTestRule.waitForIdle()
    assertTrue(retried)
  }
}
