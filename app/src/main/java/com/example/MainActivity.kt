package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ai.configureAppCheck
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.data.model.Channel
import com.example.data.model.ThemePalette
import com.example.data.repository.CatalogRepository
import com.example.data.repository.LocalSettingsRepository
import com.example.data.repository.UserProfileRepository
import com.example.ui.CatalogUiState
import com.example.ui.TiweeViewModel
import com.example.ui.components.GlobalSearchOverlay
import com.example.ui.components.TiweeTopBar
import com.example.ui.components.VoiceTranscribeOverlay
import com.example.ui.navigation.AllChannelsRoute
import com.example.ui.navigation.CategoryBrowseRoute
import com.example.ui.navigation.CategoryChannelsRoute
import com.example.ui.navigation.CountryBrowseRoute
import com.example.ui.navigation.CountryChannelsRoute
import com.example.ui.navigation.MenuRoute
import com.example.ui.navigation.SavedChannelsRoute
import com.example.ui.navigation.SettingsRoute
import com.example.ui.player.PlayerScreen
import com.example.ui.screens.AllChannelsScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CategoryBrowseScreen
import com.example.ui.screens.CategoryChannelsScreen
import com.example.ui.screens.CountryBrowseScreen
import com.example.ui.screens.CountryChannelsScreen
import com.example.ui.screens.MenuScreen
import com.example.ui.screens.SavedChannelsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.LocalTiweeColors
import com.example.ui.theme.TiweeLiveRed
import com.example.ui.theme.TiweePurple
import com.example.ui.theme.TiweeTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureAppCheck(this, intent)
    enableEdgeToEdge()
    setContent {
      AppNavigation()
    }
  }
}

private data class TopLevelNavItem(
  val label: String,
  val icon: ImageVector,
  val route: Any,
  val testTag: String,
)

@Composable
fun AppNavigation() {
  val currentUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

  if (currentUser == null) {
    TiweeTheme(palette = ThemePalette.DARK) {
      AuthScreen(onAuthSuccess = {})
    }
  } else {
    val signedInUser = currentUser!!
    key(signedInUser.uid) {
      AuthenticatedTiweeApp(user = signedInUser)
    }
  }
}

@Composable
private fun AuthenticatedTiweeApp(user: FirebaseUser) {
  val context = LocalContext.current
  val appContext = context.applicationContext
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val databaseId = remember(appContext) {
    appContext.getString(R.string.firestore_database_id)
  }

  val viewModel: TiweeViewModel = viewModel(
    key = user.uid,
    factory = viewModelFactory {
      initializer {
        val db = FirebaseFirestore.getInstance(databaseId)
        TiweeViewModel(
          catalogRepository = CatalogRepository(appContext),
          userProfileRepository = UserProfileRepository(db, Firebase.auth),
          localSettingsRepository = LocalSettingsRepository(appContext),
          currentUserId = user.uid,
          userEmail = user.email,
          userDisplayName = user.displayName,
          userPhotoUrl = user.photoUrl?.toString(),
        )
      }
    },
  )

  val themePalette by viewModel.theme.collectAsStateWithLifecycle()
  val catalogState by viewModel.catalogState.collectAsStateWithLifecycle()
  val filteredChannels by viewModel.filteredChannels.collectAsStateWithLifecycle()
  val favoriteChannels by viewModel.favoriteChannels.collectAsStateWithLifecycle()
  val recentChannels by viewModel.recentlyViewedChannels.collectAsStateWithLifecycle()
  val favorites by viewModel.favorites.collectAsStateWithLifecycle()
  val parentalSettings by viewModel.parentalSettings.collectAsStateWithLifecycle()
  val isCloudSynced by viewModel.isCloudSynced.collectAsStateWithLifecycle()
  val cloudSyncError by viewModel.cloudSyncError.collectAsStateWithLifecycle()
  val activeChannel by viewModel.activeChannel.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val sleepRemainingMinutes by viewModel.sleepRemainingMinutes.collectAsStateWithLifecycle()
  val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
  val categoryFilteredChannels by viewModel.categoryFilteredChannels.collectAsStateWithLifecycle()
  val healthScanState by viewModel.healthScanState.collectAsStateWithLifecycle()
  val brokenChannelCount by viewModel.brokenChannelCount.collectAsStateWithLifecycle()

  val favoriteIds = remember(favorites) { favorites.toHashSet() }
  var isSearchOpen by remember { mutableStateOf(false) }
  var isTranscribeOpen by remember { mutableStateOf(false) }

  val onPlayChannel: (Channel) -> Unit = remember(viewModel) { { channel -> viewModel.playChannel(channel) } }
  val onToggleFavorite: (String) -> Unit = remember(viewModel) { { channelId -> viewModel.toggleFavorite(channelId) } }
  val onSelectCategoryFilter: (String?) -> Unit = remember(viewModel) { { categoryId -> viewModel.selectCategory(categoryId) } }
  val onScanBrokenChannels: (List<Channel>, String) -> Unit = remember(viewModel) {
    { list, label -> viewModel.scanAndRemoveBrokenChannels(list, label) }
  }

  TiweeTheme(palette = themePalette) {
    val extColors = LocalTiweeColors.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    val topLevelItems = remember {
      listOf(
        TopLevelNavItem("Home", Icons.Rounded.Home, MenuRoute, "nav_home"),
        TopLevelNavItem("Countries", Icons.Rounded.Public, CountryBrowseRoute, "nav_countries"),
        TopLevelNavItem("Categories", Icons.Rounded.Category, CategoryBrowseRoute, "nav_categories"),
        TopLevelNavItem("All TV", Icons.Rounded.LiveTv, AllChannelsRoute, "nav_all_tv"),
        TopLevelNavItem("Favorites", Icons.Rounded.Favorite, SavedChannelsRoute, "nav_favorites"),
        TopLevelNavItem("Settings", Icons.Rounded.Settings, SettingsRoute, "nav_settings"),
      )
    }

    fun navigateTopLevel(route: Any) {
      navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
          saveState = true
        }
        launchSingleTop = true
        restoreState = true
      }
    }

    val isTopLevel = currentDest?.let { dest ->
      dest.hasRoute<MenuRoute>() ||
        dest.hasRoute<CountryBrowseRoute>() ||
        dest.hasRoute<CategoryBrowseRoute>() ||
        dest.hasRoute<AllChannelsRoute>() ||
        dest.hasRoute<SavedChannelsRoute>() ||
        dest.hasRoute<SettingsRoute>()
    } ?: true

    val topBarTitle: String? = when {
      currentDest?.hasRoute<MenuRoute>() == true -> null
      currentDest?.hasRoute<CountryBrowseRoute>() == true -> "Countries"
      currentDest?.hasRoute<CountryChannelsRoute>() == true ->
        backStackEntry?.toRoute<CountryChannelsRoute>()?.name ?: "Country"
      currentDest?.hasRoute<CategoryBrowseRoute>() == true -> "Categories"
      currentDest?.hasRoute<CategoryChannelsRoute>() == true ->
        backStackEntry?.toRoute<CategoryChannelsRoute>()?.name ?: "Category"
      currentDest?.hasRoute<AllChannelsRoute>() == true -> "All Live TV"
      currentDest?.hasRoute<SavedChannelsRoute>() == true -> "Favorites"
      currentDest?.hasRoute<SettingsRoute>() == true -> "Settings"
      else -> null
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(MaterialTheme.colorScheme.background, extColors.backgroundEnd)
          )
        ),
    ) {
      when (val state = catalogState) {
        is CatalogUiState.Loading -> {
          CatalogLoadingView(message = state.message)
        }

        is CatalogUiState.Error -> {
          CatalogErrorView(
            message = state.message,
            onRetry = { viewModel.loadCatalog(forceRefresh = true) },
          )
        }

        is CatalogUiState.Success -> {
          BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 840.dp

            Row(modifier = Modifier.fillMaxSize()) {
              if (useRail) {
                NavigationRail(
                  containerColor = MaterialTheme.colorScheme.surface,
                  contentColor = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.fillMaxHeight(),
                ) {
                  Spacer(modifier = Modifier.height(16.dp))
                  topLevelItems.forEach { item ->
                    val selected = isRouteSelected(currentDest, item.route)
                    NavigationRailItem(
                      selected = selected,
                      onClick = { navigateTopLevel(item.route) },
                      icon = { Icon(item.icon, contentDescription = item.label) },
                      label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                      colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.secondary,
                        selectedTextColor = MaterialTheme.colorScheme.secondary,
                        indicatorColor = TiweePurple.copy(alpha = 0.22f),
                      ),
                      modifier = Modifier.testTag(item.testTag),
                    )
                  }
                }
              }

              Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                  TiweeTopBar(
                    title = topBarTitle,
                    canGoBack = !isTopLevel,
                    isCloudSynced = isCloudSynced,
                    userDisplayName = viewModel.userDisplayName,
                    userPhotoUrl = viewModel.userPhotoUrl,
                    onBack = { navController.popBackStack() },
                    onSearchClick = { isSearchOpen = true },
                    onSettingsClick = { navigateTopLevel(SettingsRoute) },
                    onTranscribeClick = { isTranscribeOpen = true },
                    showSettingsButton = currentDest?.hasRoute<SettingsRoute>() != true,
                  )
                },
                bottomBar = {
                  if (!useRail) {
                    NavigationBar(
                      containerColor = MaterialTheme.colorScheme.surface,
                      windowInsets = WindowInsets.navigationBars,
                    ) {
                      topLevelItems.take(5).forEach { item ->
                        val selected = isRouteSelected(currentDest, item.route)
                        NavigationBarItem(
                          selected = selected,
                          onClick = { navigateTopLevel(item.route) },
                          icon = { Icon(item.icon, contentDescription = item.label) },
                          label = {
                            Text(
                              text = item.label,
                              style = MaterialTheme.typography.labelSmall,
                              maxLines = 1,
                            )
                          },
                          colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.secondary,
                            selectedTextColor = MaterialTheme.colorScheme.secondary,
                            indicatorColor = TiweePurple.copy(alpha = 0.22f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                          ),
                          modifier = Modifier.testTag(item.testTag),
                        )
                      }
                    }
                  }
                },
                modifier = Modifier.weight(1f),
              ) { innerPadding ->
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                ) {
                  NavHost(
                    navController = navController,
                    startDestination = MenuRoute,
                  ) {
                    composable<MenuRoute> {
                      MenuScreen(
                        allChannels = filteredChannels,
                        countries = state.countries,
                        categories = state.categories,
                        selectedCategoryId = selectedCategoryId,
                        categoryFilteredChannels = categoryFilteredChannels,
                        favoriteChannels = favoriteChannels,
                        recentChannels = recentChannels,
                        favoriteIds = favoriteIds,
                        countryCount = state.countries.size,
                        categoryCount = state.categories.size,
                        onNavigateCountries = { navigateTopLevel(CountryBrowseRoute) },
                        onNavigateCategories = { navigateTopLevel(CategoryBrowseRoute) },
                        onNavigateFavorites = { navigateTopLevel(SavedChannelsRoute) },
                        onNavigateAllChannels = { navigateTopLevel(AllChannelsRoute) },
                        onSelectCountry = { country ->
                          navController.navigate(
                            CountryChannelsRoute(
                              code = country.code,
                              name = country.name,
                              flag = country.flag,
                            )
                          )
                        },
                        onSelectCategoryFilter = onSelectCategoryFilter,
                        onSelectCategory = { catId, catName ->
                          navController.navigate(CategoryChannelsRoute(id = catId, name = catName))
                        },
                        onPlayChannel = onPlayChannel,
                        onToggleFavorite = onToggleFavorite,
                        onClearRecentHistory = { viewModel.clearRecentlyViewed() },
                      )
                    }

                    composable<CountryBrowseRoute> {
                      CountryBrowseScreen(
                        countries = state.countries,
                        channels = filteredChannels,
                        onSelectCountry = { country ->
                          navController.navigate(
                            CountryChannelsRoute(
                              code = country.code,
                              name = country.name,
                              flag = country.flag,
                            )
                          )
                        },
                      )
                    }

                    composable<CountryChannelsRoute> { entry ->
                      val route = entry.toRoute<CountryChannelsRoute>()
                      CountryChannelsScreen(
                        countryCode = route.code,
                        countryName = route.name,
                        countryFlag = route.flag,
                        channels = filteredChannels,
                        favoriteIds = favoriteIds,
                        onPlayChannel = onPlayChannel,
                        onToggleFavorite = onToggleFavorite,
                        healthScanState = healthScanState,
                        onScanBrokenChannels = onScanBrokenChannels,
                        onCancelHealthScan = viewModel::cancelHealthScan,
                        onDismissHealthScan = viewModel::dismissHealthScan,
                      )
                    }

                    composable<CategoryBrowseRoute> {
                      CategoryBrowseScreen(
                        categories = state.categories,
                        channels = filteredChannels,
                        onSelectCategory = { cat ->
                          navController.navigate(CategoryChannelsRoute(id = cat.id, name = cat.name))
                        },
                      )
                    }

                    composable<CategoryChannelsRoute> { entry ->
                      val route = entry.toRoute<CategoryChannelsRoute>()
                      CategoryChannelsScreen(
                        categoryId = route.id,
                        categoryName = route.name,
                        channels = filteredChannels,
                        favoriteIds = favoriteIds,
                        onPlayChannel = onPlayChannel,
                        onToggleFavorite = onToggleFavorite,
                        healthScanState = healthScanState,
                        onScanBrokenChannels = onScanBrokenChannels,
                        onCancelHealthScan = viewModel::cancelHealthScan,
                        onDismissHealthScan = viewModel::dismissHealthScan,
                      )
                    }

                    composable<AllChannelsRoute> {
                      AllChannelsScreen(
                        channels = filteredChannels,
                        categories = state.categories,
                        selectedCategoryId = selectedCategoryId,
                        onSelectCategory = onSelectCategoryFilter,
                        favoriteIds = favoriteIds,
                        onPlayChannel = onPlayChannel,
                        onToggleFavorite = onToggleFavorite,
                        healthScanState = healthScanState,
                        onScanBrokenChannels = onScanBrokenChannels,
                        onCancelHealthScan = viewModel::cancelHealthScan,
                        onDismissHealthScan = viewModel::dismissHealthScan,
                      )
                    }

                    composable<SavedChannelsRoute> {
                      SavedChannelsScreen(
                        favoriteChannels = favoriteChannels,
                        favoriteIds = favoriteIds,
                        onPlayChannel = onPlayChannel,
                        onToggleFavorite = onToggleFavorite,
                        onClearFavorites = { viewModel.clearFavorites() },
                        onDiscoverChannels = { navigateTopLevel(AllChannelsRoute) },
                        healthScanState = healthScanState,
                        onScanBrokenChannels = onScanBrokenChannels,
                        onCancelHealthScan = viewModel::cancelHealthScan,
                        onDismissHealthScan = viewModel::dismissHealthScan,
                      )
                    }

                    composable<SettingsRoute> {
                      SettingsScreen(
                        userDisplayName = viewModel.userDisplayName,
                        userEmail = viewModel.userEmail,
                        userPhotoUrl = viewModel.userPhotoUrl,
                        isCloudSynced = isCloudSynced,
                        cloudSyncError = cloudSyncError,
                        themePalette = themePalette,
                        favoriteCount = favoriteChannels.size,
                        sleepRemainingMinutes = sleepRemainingMinutes,
                        parentalSettings = parentalSettings,
                        channelCount = filteredChannels.size,
                        countryCount = state.countries.size,
                        categoryCount = state.categories.size,
                        brokenChannelCount = brokenChannelCount,
                        healthScanState = healthScanState,
                        onNavigateFavorites = { navigateTopLevel(SavedChannelsRoute) },
                        onToggleTheme = { viewModel.toggleTheme() },
                        onSetSleepTimer = { viewModel.setSleepTimerMinutes(it) },
                        onSetAllowAdultChannels = { viewModel.setAllowAdultChannels(it) },
                        onSetParentalPin = { viewModel.setParentalPin(it) },
                        onRefreshCatalog = { viewModel.loadCatalog(forceRefresh = true) },
                        onScanBrokenChannels = {
                          val featuredFirst = filteredChannels.sortedBy { ch ->
                            val idx = CatalogRepository.FEATURED_SOUTH_ASIA_CODES.indexOf(ch.country.uppercase())
                            if (idx >= 0) idx else Int.MAX_VALUE
                          }
                          viewModel.scanAndRemoveBrokenChannels(featuredFirst, "Featured & Live TV")
                        },
                        onCancelHealthScan = viewModel::cancelHealthScan,
                        onDismissHealthScan = viewModel::dismissHealthScan,
                        onRestoreBrokenChannels = viewModel::restoreBrokenChannels,
                        onSignOut = { signOut(credentialManager, scope) },
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      if (isSearchOpen) {
        GlobalSearchOverlay(
          query = searchQuery,
          onQueryChange = { viewModel.setSearchQuery(it) },
          channels = filteredChannels,
          favoriteIds = favoriteIds,
          onPlayChannel = onPlayChannel,
          onToggleFavorite = onToggleFavorite,
          onViewAllChannels = { navigateTopLevel(AllChannelsRoute) },
          onTranscribeClick = {
            isSearchOpen = false
            isTranscribeOpen = true
          },
          onDismiss = { isSearchOpen = false },
        )
      }

      if (isTranscribeOpen) {
        VoiceTranscribeOverlay(
          onUseForChannelSearch = { transcript ->
            viewModel.setSearchQuery(transcript)
            isTranscribeOpen = false
            isSearchOpen = true
          },
          onDismiss = { isTranscribeOpen = false },
        )
      }

      activeChannel?.let { channel ->
        val subtitle =
          listOfNotNull(
            channel.country.takeIf { it.isNotBlank() }?.uppercase(),
            channel.categories.firstOrNull()?.replaceFirstChar { it.uppercaseChar() },
          ).joinToString(" • ").ifBlank { null }

        PlayerScreen(
          streams = channel.streams,
          title = channel.name,
          streamKey = channel.id,
          subtitle = subtitle,
          logoUrl = channel.logo,
          isLive = true,
          isFavorite = channel.id in favoriteIds,
          sleepRemainingMinutes = sleepRemainingMinutes,
          onToggleFavorite = { viewModel.toggleFavorite(channel.id) },
          onStreamFailed = { viewModel.markChannelBroken(channel.id) },
          onStreamReady = { viewModel.markChannelPlayable(channel.id) },
          onBack = { viewModel.playChannel(null) },
          modifier = Modifier.fillMaxSize(),
        )
      }
    }
  }
}

private fun isRouteSelected(
  destination: androidx.navigation.NavDestination?,
  route: Any,
): Boolean {
  if (destination == null) return false
  return when (route) {
    is MenuRoute -> destination.hasRoute<MenuRoute>()
    is CountryBrowseRoute ->
      destination.hasRoute<CountryBrowseRoute>() || destination.hasRoute<CountryChannelsRoute>()
    is CategoryBrowseRoute ->
      destination.hasRoute<CategoryBrowseRoute>() || destination.hasRoute<CategoryChannelsRoute>()
    is AllChannelsRoute -> destination.hasRoute<AllChannelsRoute>()
    is SavedChannelsRoute -> destination.hasRoute<SavedChannelsRoute>()
    is SettingsRoute -> destination.hasRoute<SettingsRoute>()
    else -> false
  }
}

@Composable
private fun CatalogLoadingView(message: String) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp)
      .testTag("catalog_loading_view"),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF161D27),
        shadowElevation = 12.dp,
        modifier = Modifier.size(88.dp),
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_launcher_fg),
          contentDescription = "UiPTv Player Pro+",
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp)),
        )
      }
      Text(
        text = "UiPTv Player Pro+",
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground,
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      LinearProgressIndicator(
        color = TiweePurple,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
          .width(220.dp)
          .height(6.dp)
          .clip(RoundedCornerShape(999.dp)),
      )
    }
  }
}

@Composable
private fun CatalogErrorView(
  message: String,
  onRetry: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp)
      .testTag("catalog_error_view"),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.ErrorOutline,
        contentDescription = null,
        tint = TiweeLiveRed,
        modifier = Modifier.size(48.dp),
      )
      Text(
        text = "Unable to Load Live TV Catalog",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      Button(
        onClick = onRetry,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TiweePurple),
        modifier = Modifier.testTag("catalog_retry_button"),
      ) {
        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Retry Connection")
      }
    }
  }
}
