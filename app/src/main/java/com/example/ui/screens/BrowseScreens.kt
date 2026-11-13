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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.data.model.Channel
import com.example.data.model.Country
import com.example.data.repository.CatalogRepository
import com.example.ui.ChannelHealthScanState
import com.example.ui.components.ChannelCard
import com.example.ui.components.ChannelHealthScanBanner
import com.example.ui.components.CleanOfflineChip
import com.example.ui.components.GenreStyleRegistry
import com.example.ui.theme.LocalTiweeColors
import com.example.ui.theme.TiweePurple
import kotlinx.coroutines.launch

private enum class CategorySortMode {
  AZ,
  ZA,
  COUNT,
}

@Composable
fun CountryBrowseScreen(
  countries: List<Country>,
  channels: List<Channel>,
  onSelectCountry: (Country) -> Unit,
) {
  val extColors = LocalTiweeColors.current
  val gridState = rememberLazyGridState()
  var searchQuery by remember { mutableStateOf("") }

  val countsByCountry = remember(channels) {
    val map = HashMap<String, Int>()
    channels.forEach { ch ->
      if (ch.country.isNotBlank()) {
        val code = ch.country.uppercase()
        map[code] = (map[code] ?: 0) + 1
      }
    }
    map
  }

  val displayedCountries = remember(countries, countsByCountry, searchQuery) {
    val trimmed = searchQuery.trim()
    val featuredOrder = CatalogRepository.FEATURED_SOUTH_ASIA_CODES
    countries
      .mapNotNull { country ->
        val code = country.code.uppercase()
        val count = countsByCountry[code] ?: 0
        if (count <= 0 && code !in featuredOrder) null else country to count
      }
      .filter { (country, _) ->
        trimmed.isEmpty() ||
          country.name.contains(trimmed, ignoreCase = true) ||
          country.code.contains(trimmed, ignoreCase = true)
      }
      .sortedWith(
        compareBy<Pair<Country, Int>> { (country, _) ->
          val idx = featuredOrder.indexOf(country.code.uppercase())
          if (idx >= 0) idx else Int.MAX_VALUE
        }.thenByDescending { it.second }
      )
  }

  Column(modifier = Modifier.fillMaxSize()) {
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text("Search countries or ISO code...") },
      leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(
            onClick = { searchQuery = "" },
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
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
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .testTag("country_search_input"),
    )

    LazyVerticalGrid(
      state = gridState,
      columns = GridCells.Adaptive(minSize = 158.dp),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier
        .fillMaxSize()
        .testTag("country_grid"),
    ) {
      items(
        items = displayedCountries,
        key = { it.first.code },
        contentType = { "country_card" },
      ) { (country, count) ->
        val isFeatured = country.code.uppercase() in CatalogRepository.FEATURED_SOUTH_ASIA_CODES
        Surface(
          onClick = { onSelectCountry(country) },
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(
            1.dp,
            if (isFeatured) TiweePurple.copy(alpha = 0.55f) else extColors.cardBorder,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("country_card_${country.code}"),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = country.flag,
                fontSize = 30.sp,
              )
              Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                if (isFeatured) {
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TiweePurple.copy(alpha = 0.32f),
                  ) {
                    Text(
                      text = "Featured",
                      style = MaterialTheme.typography.labelSmall,
                      color = Color.White,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                  }
                }
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = TiweePurple.copy(alpha = 0.2f),
                ) {
                  Text(
                    text = country.code.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                  )
                }
              }
            }
            Column {
              Text(
                text = country.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "$count channels",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun CountryChannelsScreen(
  countryCode: String,
  countryName: String,
  countryFlag: String,
  channels: List<Channel>,
  favoriteIds: Set<String>,
  onPlayChannel: (Channel) -> Unit,
  onToggleFavorite: (String) -> Unit,
  healthScanState: ChannelHealthScanState = ChannelHealthScanState.Idle,
  onScanBrokenChannels: ((List<Channel>, String) -> Unit)? = null,
  onCancelHealthScan: () -> Unit = {},
  onDismissHealthScan: () -> Unit = {},
) {
  val extColors = LocalTiweeColors.current
  val gridState = rememberLazyGridState()
  var searchQuery by remember { mutableStateOf("") }
  var sortAlphabetical by remember { mutableStateOf(false) }

  val filtered = remember(channels, countryCode, searchQuery, sortAlphabetical) {
    val trimmed = searchQuery.trim()
    val base = channels.filter { ch ->
      ch.country.equals(countryCode, ignoreCase = true) &&
        (trimmed.isEmpty() || ch.name.contains(trimmed, ignoreCase = true))
    }
    if (sortAlphabetical) {
      base.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    } else {
      base
    }
  }

  Column(modifier = Modifier.fillMaxSize()) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, extColors.cardBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
      Row(
        modifier = Modifier.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Text(text = countryFlag, fontSize = 32.sp)
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = countryName,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "${filtered.size} live channels available",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          if (onScanBrokenChannels != null && filtered.isNotEmpty()) {
            CleanOfflineChip(
              isScanning = healthScanState is ChannelHealthScanState.Scanning,
              onClick = { onScanBrokenChannels(filtered, countryName) },
            )
          }
          Surface(
            onClick = { sortAlphabetical = !sortAlphabetical },
            shape = RoundedCornerShape(12.dp),
            color = if (sortAlphabetical) TiweePurple.copy(alpha = 0.24f) else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("country_channels_sort_az"),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.SortByAlpha,
                contentDescription = "Sort A to Z",
                tint = if (sortAlphabetical) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "A–Z",
                style = MaterialTheme.typography.labelMedium,
                color = if (sortAlphabetical) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }

    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text("Search in $countryName...") },
      leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(
            onClick = { searchQuery = "" },
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
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
        .padding(horizontal = 16.dp, vertical = 6.dp)
        .testTag("country_channels_search_input"),
    )

    ChannelHealthScanBanner(
      scanState = healthScanState,
      onCancel = onCancelHealthScan,
      onDismiss = onDismissHealthScan,
    )

    if (filtered.isEmpty()) {
      EmptyChannelsState(message = "No matching channels found in $countryName.")
    } else {
      LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 110.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
          .fillMaxSize()
          .testTag("country_channels_grid"),
      ) {
        items(
          items = filtered,
          key = { it.id },
          contentType = { "channel_card" },
        ) { channel ->
          ChannelCard(
            channel = channel,
            isFavorite = channel.id in favoriteIds,
            onClick = { onPlayChannel(channel) },
            onToggleFavorite = { onToggleFavorite(channel.id) },
          )
        }
      }
    }
  }
}

@Composable
fun CategoryBrowseScreen(
  categories: List<Category>,
  channels: List<Channel>,
  onSelectCategory: (Category) -> Unit,
) {
  val extColors = LocalTiweeColors.current
  val gridState = rememberLazyGridState()
  val scope = rememberCoroutineScope()

  var searchQuery by remember { mutableStateOf("") }
  var sortMode by remember { mutableStateOf(CategorySortMode.COUNT) }

  val countsByCat = remember(channels) {
    val map = HashMap<String, Int>()
    channels.forEach { ch ->
      ch.categories.forEach { cat ->
        val key = cat.lowercase()
        map[key] = (map[key] ?: 0) + 1
      }
    }
    map
  }

  val displayed = remember(categories, countsByCat, searchQuery, sortMode) {
    val trimmed = searchQuery.trim()
    val list = categories
      .map { cat -> cat to (countsByCat[cat.id.lowercase()] ?: 0) }
      .filter { (cat, count) ->
        count > 0 &&
          (trimmed.isEmpty() || cat.name.contains(trimmed, ignoreCase = true))
      }
    when (sortMode) {
      CategorySortMode.AZ -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.first.name })
      CategorySortMode.ZA -> list.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.first.name })
      CategorySortMode.COUNT -> list.sortedByDescending { it.second }
    }
  }

  val letters = remember(displayed) {
    displayed.mapNotNull { it.first.name.firstOrNull()?.uppercaseChar() }.distinct().sorted()
  }

  Column(modifier = Modifier.fillMaxSize()) {
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text("Search categories...") },
      leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(
            onClick = { searchQuery = "" },
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
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
        .padding(horizontal = 16.dp, vertical = 6.dp)
        .testTag("category_search_input"),
    )

    // Sort controls + A-Z jump pills
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      CategorySortChip("Most Channels", sortMode == CategorySortMode.COUNT, "cat_sort_count") {
        sortMode = CategorySortMode.COUNT
      }
      CategorySortChip("A–Z", sortMode == CategorySortMode.AZ, "cat_sort_az") {
        sortMode = CategorySortMode.AZ
      }
      CategorySortChip("Z–A", sortMode == CategorySortMode.ZA, "cat_sort_za") {
        sortMode = CategorySortMode.ZA
      }
    }

    if (sortMode != CategorySortMode.COUNT && letters.isNotEmpty()) {
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        items(letters) { letter ->
          Surface(
            onClick = {
              val index = displayed.indexOfFirst {
                it.first.name.firstOrNull()?.uppercaseChar() == letter
              }
              if (index >= 0) {
                scope.launch { gridState.animateScrollToItem(index) }
              }
            },
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, extColors.cardBorder),
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Text(
              text = letter.toString(),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            )
          }
        }
      }
    }

    LazyVerticalGrid(
      state = gridState,
      columns = GridCells.Adaptive(minSize = 158.dp),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier
        .fillMaxSize()
        .testTag("category_grid"),
    ) {
      items(
        items = displayed,
        key = { it.first.id },
        contentType = { "category_card" },
      ) { (category, count) ->
        val visual = GenreStyleRegistry.visualForCategory(category.id)
        Surface(
          onClick = { onSelectCategory(category) },
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, extColors.cardBorder),
          modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("category_card_${category.id}"),
        ) {
          Column(
            modifier = Modifier
              .background(
                Brush.verticalGradient(
                  listOf(visual.accent.copy(alpha = 0.16f), Color.Transparent)
                )
              )
              .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(visual.accent.copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = visual.icon,
                contentDescription = category.name,
                tint = visual.accent,
                modifier = Modifier.size(22.dp),
              )
            }
            Column {
              Text(
                text = category.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "$count live channels",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun CategoryChannelsScreen(
  categoryId: String,
  categoryName: String,
  channels: List<Channel>,
  favoriteIds: Set<String>,
  onPlayChannel: (Channel) -> Unit,
  onToggleFavorite: (String) -> Unit,
  healthScanState: ChannelHealthScanState = ChannelHealthScanState.Idle,
  onScanBrokenChannels: ((List<Channel>, String) -> Unit)? = null,
  onCancelHealthScan: () -> Unit = {},
  onDismissHealthScan: () -> Unit = {},
) {
  val extColors = LocalTiweeColors.current
  val gridState = rememberLazyGridState()
  val visual = GenreStyleRegistry.visualForCategory(categoryId)
  var searchQuery by remember { mutableStateOf("") }
  var sortAlphabetical by remember { mutableStateOf(false) }

  val filtered = remember(channels, categoryId, searchQuery, sortAlphabetical) {
    val trimmed = searchQuery.trim()
    val base = channels.filter { ch ->
      ch.categories.any { it.equals(categoryId, ignoreCase = true) } &&
        (trimmed.isEmpty() || ch.name.contains(trimmed, ignoreCase = true))
    }
    if (sortAlphabetical) {
      base.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    } else {
      base
    }
  }

  Column(modifier = Modifier.fillMaxSize()) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, visual.accent.copy(alpha = 0.4f)),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
      Row(
        modifier = Modifier
          .background(
            Brush.horizontalGradient(
              listOf(visual.accent.copy(alpha = 0.2f), Color.Transparent)
            )
          )
          .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(visual.accent.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = visual.icon,
              contentDescription = categoryName,
              tint = visual.accent,
              modifier = Modifier.size(24.dp),
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = categoryName,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "${filtered.size} channels streaming live",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          if (onScanBrokenChannels != null && filtered.isNotEmpty()) {
            CleanOfflineChip(
              isScanning = healthScanState is ChannelHealthScanState.Scanning,
              onClick = { onScanBrokenChannels(filtered, categoryName) },
            )
          }
          Surface(
            onClick = { sortAlphabetical = !sortAlphabetical },
            shape = RoundedCornerShape(12.dp),
            color = if (sortAlphabetical) TiweePurple.copy(alpha = 0.24f) else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("category_channels_sort_az"),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.SortByAlpha,
                contentDescription = "Sort A to Z",
                tint = if (sortAlphabetical) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "A–Z",
                style = MaterialTheme.typography.labelMedium,
                color = if (sortAlphabetical) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }

    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text("Filter $categoryName channels...") },
      leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(
            onClick = { searchQuery = "" },
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
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
        .padding(horizontal = 16.dp, vertical = 6.dp)
        .testTag("category_channels_search_input"),
    )

    ChannelHealthScanBanner(
      scanState = healthScanState,
      onCancel = onCancelHealthScan,
      onDismiss = onDismissHealthScan,
    )

    if (filtered.isEmpty()) {
      EmptyChannelsState(message = "No matching channels in $categoryName.")
    } else {
      LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 110.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
          .fillMaxSize()
          .testTag("category_channels_grid"),
      ) {
        items(
          items = filtered,
          key = { it.id },
          contentType = { "channel_card" },
        ) { channel ->
          ChannelCard(
            channel = channel,
            isFavorite = channel.id in favoriteIds,
            onClick = { onPlayChannel(channel) },
            onToggleFavorite = { onToggleFavorite(channel.id) },
          )
        }
      }
    }
  }
}

@Composable
private fun CategorySortChip(
  label: String,
  selected: Boolean,
  testTag: String,
  onClick: () -> Unit,
) {
  val extColors = LocalTiweeColors.current
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(12.dp),
    color = if (selected) TiweePurple.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface,
    border = BorderStroke(
      1.dp,
      if (selected) TiweePurple.copy(alpha = 0.6f) else extColors.cardBorder,
    ),
    modifier = Modifier
      .minimumInteractiveComponentSize()
      .testTag(testTag),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
    )
  }
}

@Composable
internal fun EmptyChannelsState(
  message: String,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        imageVector = Icons.Rounded.LiveTv,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.size(48.dp),
      )
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
