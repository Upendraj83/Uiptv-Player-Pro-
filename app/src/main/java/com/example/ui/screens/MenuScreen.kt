package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Category
import com.example.data.model.Channel
import com.example.data.model.Country
import com.example.data.repository.CatalogRepository
import com.example.ui.components.ChannelCard
import com.example.ui.components.GenreFilterChips
import com.example.ui.components.GenreStyleRegistry
import com.example.ui.components.MenuCardSpec
import com.example.ui.theme.LocalTiweeColors
import com.example.ui.theme.TiweeAmber
import com.example.ui.theme.TiweeCyan
import com.example.ui.theme.TiweeFavoriteRose
import com.example.ui.theme.TiweeLiveRed
import com.example.ui.theme.TiweePurple

@Composable
fun MenuScreen(
  allChannels: List<Channel>,
  countries: List<Country>,
  categories: List<Category>,
  selectedCategoryId: String?,
  categoryFilteredChannels: List<Channel>,
  favoriteChannels: List<Channel>,
  recentChannels: List<Channel>,
  favoriteIds: Set<String>,
  countryCount: Int,
  categoryCount: Int,
  onNavigateCountries: () -> Unit,
  onNavigateCategories: () -> Unit,
  onNavigateFavorites: () -> Unit,
  onNavigateAllChannels: () -> Unit,
  onSelectCountry: (Country) -> Unit,
  onSelectCategoryFilter: (String?) -> Unit,
  onSelectCategory: (id: String, name: String) -> Unit,
  onPlayChannel: (Channel) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onClearRecentHistory: () -> Unit,
) {
  val extColors = LocalTiweeColors.current
  val gridState = rememberLazyGridState()
  var genreSearch by remember { mutableStateOf("") }
  var sortAlphabetical by remember { mutableStateOf(false) }

  val topFavorites = remember(favoriteChannels) { favoriteChannels.take(8) }
  val topRecents = remember(recentChannels) { recentChannels.take(5) }
  val topCategoryChannels = remember(categoryFilteredChannels) { categoryFilteredChannels.take(15) }

  val categoryCounts = remember(allChannels) {
    val map = HashMap<String, Int>()
    allChannels.forEach { ch ->
      ch.categories.forEach { cat ->
        val key = cat.lowercase()
        map[key] = (map[key] ?: 0) + 1
      }
    }
    map
  }

  val featuredCountriesWithCount = remember(allChannels, countries) {
    val countryByCode = countries.associateBy { it.code.uppercase() }
    val counts = HashMap<String, Int>(4)
    allChannels.forEach { ch ->
      val code = ch.country.uppercase()
      if (code in CatalogRepository.FEATURED_SOUTH_ASIA_CODES) {
        counts[code] = (counts[code] ?: 0) + 1
      }
    }
    CatalogRepository.FEATURED_SOUTH_ASIA_COUNTRIES.map { defaultCountry ->
      val resolved = countryByCode[defaultCountry.code] ?: defaultCountry
      resolved to (counts[defaultCountry.code] ?: 0)
    }
  }

  val displayedGenres = remember(genreSearch, sortAlphabetical) {
    val trimmed = genreSearch.trim()
    val base = if (trimmed.isEmpty()) {
      GenreStyleRegistry.menuCardDefaults
    } else {
      GenreStyleRegistry.menuCardDefaults.filter { spec ->
        spec.title.contains(trimmed, ignoreCase = true)
      }
    }
    if (sortAlphabetical) {
      base.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
    } else {
      base
    }
  }

  LazyVerticalGrid(
    state = gridState,
    columns = GridCells.Adaptive(minSize = 152.dp),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier
      .fillMaxSize()
      .testTag("menu_screen_grid"),
  ) {
    // 3 Top Quick Action Tiles (Countries, Categories, Favorites)
    item(key = "menu_quick_actions", span = { GridItemSpan(maxLineSpan) }, contentType = "quick_actions") {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        QuickActionTile(
          title = "Countries",
          subtitle = "$countryCount regions",
          icon = Icons.Rounded.Public,
          accent = TiweeCyan,
          testTag = "menu_tile_countries",
          onClick = onNavigateCountries,
          modifier = Modifier.weight(1f),
        )
        QuickActionTile(
          title = "Categories",
          subtitle = "$categoryCount genres",
          icon = Icons.Rounded.Category,
          accent = TiweeAmber,
          testTag = "menu_tile_categories",
          onClick = onNavigateCategories,
          modifier = Modifier.weight(1f),
        )
        QuickActionTile(
          title = "Favorites",
          subtitle = "${favoriteIds.size} saved",
          icon = Icons.Rounded.Favorite,
          accent = TiweeFavoriteRose,
          testTag = "menu_tile_favorites",
          onClick = onNavigateFavorites,
          modifier = Modifier.weight(1f),
        )
      }
    }

    // Featured Regions: India, Pakistan & Bangladesh
    item(key = "menu_featured_countries", span = { GridItemSpan(maxLineSpan) }, contentType = "featured_countries") {
      Column(modifier = Modifier.fillMaxWidth()) {
        ShelfHeader(
          title = "India, Pakistan & Bangladesh",
          icon = Icons.Rounded.Public,
          iconTint = TiweeCyan,
          actionLabel = "All Countries",
          actionTestTag = "menu_view_all_countries",
          onAction = onNavigateCountries,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          featuredCountriesWithCount.forEach { (country, count) ->
            FeaturedCountryTile(
              country = country,
              channelCount = count,
              onClick = { onSelectCountry(country) },
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }

    // Favorites Quick Shelf (if any saved)
    if (topFavorites.isNotEmpty()) {
      item(key = "menu_favorites_shelf", span = { GridItemSpan(maxLineSpan) }, contentType = "channel_shelf") {
        Column(modifier = Modifier.fillMaxWidth()) {
          ShelfHeader(
            title = "Favorites",
            icon = Icons.Rounded.Favorite,
            iconTint = extColors.favoriteRose,
            actionLabel = "View all (${favoriteChannels.size})",
            actionTestTag = "menu_view_all_favorites",
            onAction = onNavigateFavorites,
          )
          Spacer(modifier = Modifier.height(10.dp))
          LazyRow(
            state = rememberLazyListState(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            items(
              items = topFavorites,
              key = { "fav_${it.id}" },
              contentType = { "channel_card" },
            ) { channel ->
              ChannelCard(
                channel = channel,
                isFavorite = true,
                onClick = { onPlayChannel(channel) },
                onToggleFavorite = { onToggleFavorite(channel.id) },
                modifier = Modifier.width(118.dp),
              )
            }
          }
        }
      }
    }

    // Recently Viewed Shelf
    item(key = "menu_recents_shelf", span = { GridItemSpan(maxLineSpan) }, contentType = "channel_shelf") {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Rounded.History,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Recently Viewed",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onBackground,
            )
          }
          if (recentChannels.isNotEmpty()) {
            TextButton(
              onClick = onClearRecentHistory,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("clear_recent_history_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Clear History",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (recentChannels.isEmpty()) {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            border = BorderStroke(1.dp, extColors.cardBorder),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(
                modifier = Modifier
                  .size(34.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(TiweePurple.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Rounded.LiveTv,
                  contentDescription = null,
                  tint = TiweePurple,
                  modifier = Modifier.size(18.dp),
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "No recently watched channels",
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "Watch any channel and your last 5 streams appear here.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        } else {
          LazyRow(
            state = rememberLazyListState(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            items(
              items = topRecents,
              key = { "recent_${it.id}" },
              contentType = { "channel_card" },
            ) { channel ->
              ChannelCard(
                channel = channel,
                isFavorite = channel.id in favoriteIds,
                onClick = { onPlayChannel(channel) },
                onToggleFavorite = { onToggleFavorite(channel.id) },
                modifier = Modifier.width(118.dp),
              )
            }
          }
        }
      }
    }

    // Live Channels Filtered by Clickable Genre Chips
    item(key = "menu_live_shelf", span = { GridItemSpan(maxLineSpan) }, contentType = "channel_shelf") {
      Column(modifier = Modifier.fillMaxWidth()) {
        ShelfHeader(
          title = "Live Channels",
          icon = Icons.Rounded.LiveTv,
          iconTint = TiweeLiveRed,
          actionLabel = "View all (${categoryFilteredChannels.size})",
          actionTestTag = "menu_view_all_channels",
          onAction = onNavigateAllChannels,
        )
        Spacer(modifier = Modifier.height(6.dp))
        GenreFilterChips(
          categories = categories,
          selectedCategoryId = selectedCategoryId,
          onSelectCategory = onSelectCategoryFilter,
          channels = allChannels,
          contentPadding = PaddingValues(vertical = 4.dp),
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (topCategoryChannels.isEmpty()) {
          Text(
            text = "No channels available in this genre.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          LazyRow(
            state = rememberLazyListState(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.testTag("menu_filtered_channels_row"),
          ) {
            items(
              items = topCategoryChannels,
              key = { "live_${it.id}" },
              contentType = { "channel_card" },
            ) { channel ->
              ChannelCard(
                channel = channel,
                isFavorite = channel.id in favoriteIds,
                onClick = { onPlayChannel(channel) },
                onToggleFavorite = { onToggleFavorite(channel.id) },
                modifier = Modifier.width(118.dp),
              )
            }
          }
        }
      }
    }

    // TV Genres Section Header + Filter
    item(key = "menu_genres_header", span = { GridItemSpan(maxLineSpan) }, contentType = "genres_header") {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "TV Genres & Categories",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Surface(
            onClick = { sortAlphabetical = !sortAlphabetical },
            shape = RoundedCornerShape(12.dp),
            color = if (sortAlphabetical) TiweePurple.copy(alpha = 0.24f) else MaterialTheme.colorScheme.surface,
            border = BorderStroke(
              1.dp,
              if (sortAlphabetical) TiweePurple.copy(alpha = 0.55f) else extColors.cardBorder,
            ),
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("menu_sort_az_button"),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.SortByAlpha,
                contentDescription = "Sort A to Z",
                tint = if (sortAlphabetical) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "A–Z",
                style = MaterialTheme.typography.labelMedium,
                color = if (sortAlphabetical) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = genreSearch,
          onValueChange = { genreSearch = it },
          placeholder = {
            Text(
              text = "Filter genres...",
              style = MaterialTheme.typography.bodyMedium,
            )
          },
          leadingIcon = {
            Icon(
              imageVector = Icons.Rounded.Search,
              contentDescription = null,
            )
          },
          trailingIcon = {
            if (genreSearch.isNotEmpty()) {
              IconButton(
                onClick = { genreSearch = "" },
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Clear,
                  contentDescription = "Clear filter",
                )
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TiweePurple,
            unfocusedBorderColor = extColors.cardBorder,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("menu_genre_search_input"),
        )
      }
    }

    // Genre Cards
    items(
      items = displayedGenres,
      key = { it.title },
      contentType = { "genre_card" },
    ) { spec ->
      val count =
        if (spec.categoryId == null) {
          allChannels.size
        } else {
          categoryCounts[spec.categoryId.lowercase()] ?: 0
        }
      GenreHighlightCard(
        spec = spec,
        channelCount = count,
        onClick = {
          if (spec.categoryId == null) {
            onNavigateAllChannels()
          } else {
            onSelectCategory(spec.categoryId, spec.title)
          }
        },
      )
    }
  }
}

@Composable
private fun QuickActionTile(
  title: String,
  subtitle: String,
  icon: ImageVector,
  accent: Color,
  testTag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val extColors = LocalTiweeColors.current
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, extColors.cardBorder),
    modifier = modifier
      .minimumInteractiveComponentSize()
      .testTag(testTag),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Box(
        modifier = Modifier
          .size(32.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(accent.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = accent,
          modifier = Modifier.size(18.dp),
        )
      }
      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun ShelfHeader(
  title: String,
  icon: ImageVector,
  iconTint: Color,
  actionLabel: String,
  actionTestTag: String,
  onAction: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = iconTint,
        modifier = Modifier.size(20.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
      )
    }
    TextButton(
      onClick = onAction,
      modifier = Modifier
        .minimumInteractiveComponentSize()
        .testTag(actionTestTag),
    ) {
      Text(
        text = actionLabel,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary,
      )
      Icon(
        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.size(18.dp),
      )
    }
  }
}

@Composable
private fun GenreHighlightCard(
  spec: MenuCardSpec,
  channelCount: Int,
  onClick: () -> Unit,
) {
  val extColors = LocalTiweeColors.current
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, extColors.cardBorder),
    modifier = Modifier
      .fillMaxWidth()
      .minimumInteractiveComponentSize()
      .testTag("menu_genre_card_${spec.categoryId ?: "all"}"),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.linearGradient(
            listOf(
              spec.visual.accent.copy(alpha = 0.16f),
              Color.Transparent,
            )
          )
        )
        .padding(11.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(spec.visual.accent.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = spec.visual.icon,
            contentDescription = spec.title,
            tint = spec.visual.accent,
            modifier = Modifier.size(20.dp),
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = spec.title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "$channelCount ch.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun FeaturedCountryTile(
  country: Country,
  channelCount: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, TiweeCyan.copy(alpha = 0.35f)),
    modifier = modifier
      .minimumInteractiveComponentSize()
      .testTag("menu_featured_country_${country.code.lowercase()}"),
  ) {
    Column(
      modifier = Modifier
        .background(
          Brush.verticalGradient(
            listOf(TiweeCyan.copy(alpha = 0.14f), Color.Transparent)
          )
        )
        .padding(horizontal = 10.dp, vertical = 9.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = country.flag,
          style = MaterialTheme.typography.titleMedium,
        )
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = TiweePurple.copy(alpha = 0.28f),
        ) {
          Text(
            text = country.code.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
          )
        }
      }
      Column {
        Text(
          text = country.name,
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "$channelCount live",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
        )
      }
    }
  }
}
