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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.Category
import com.example.data.model.Channel
import com.example.ui.ChannelHealthScanState
import com.example.ui.components.ChannelCard
import com.example.ui.components.ChannelHealthScanBanner
import com.example.ui.components.CleanOfflineChip
import com.example.ui.components.GenreFilterChips
import com.example.ui.theme.LocalTiweeColors
import com.example.ui.theme.TiweeLiveRed
import com.example.ui.theme.TiweePurple

@Composable
fun AllChannelsScreen(
  channels: List<Channel>,
  categories: List<Category>,
  selectedCategoryId: String?,
  onSelectCategory: (String?) -> Unit,
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

  val filtered = remember(channels, selectedCategoryId, searchQuery, sortAlphabetical) {
    val trimmed = searchQuery.trim()
    val base = channels.filter { ch ->
      val catMatch =
        selectedCategoryId == null ||
          ch.categories.any { it.equals(selectedCategoryId, ignoreCase = true) }
      val queryMatch =
        trimmed.isEmpty() ||
          ch.name.contains(trimmed, ignoreCase = true) ||
          ch.country.equals(trimmed, ignoreCase = true) ||
          ch.altNames.any { it.contains(trimmed, ignoreCase = true) }
      catMatch && queryMatch
    }
    if (sortAlphabetical) {
      base.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    } else {
      base
    }
  }

  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search ${channels.size} live channels...") },
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
          .weight(1f)
          .testTag("all_channels_search_input"),
      )

      Surface(
        onClick = { sortAlphabetical = !sortAlphabetical },
        shape = RoundedCornerShape(16.dp),
        color = if (sortAlphabetical) TiweePurple.copy(alpha = 0.24f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
          1.dp,
          if (sortAlphabetical) TiweePurple.copy(alpha = 0.6f) else extColors.cardBorder,
        ),
        modifier = Modifier
          .height(54.dp)
          .minimumInteractiveComponentSize()
          .testTag("all_channels_sort_az"),
      ) {
        Box(
          modifier = Modifier.padding(horizontal = 14.dp),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.SortByAlpha,
            contentDescription = "Sort A to Z",
            tint = if (sortAlphabetical) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      if (onScanBrokenChannels != null && filtered.isNotEmpty()) {
        CleanOfflineChip(
          isScanning = healthScanState is ChannelHealthScanState.Scanning,
          onClick = { onScanBrokenChannels(filtered, "Live TV") },
          modifier = Modifier.height(54.dp),
        )
      }
    }

    GenreFilterChips(
      categories = categories,
      selectedCategoryId = selectedCategoryId,
      onSelectCategory = onSelectCategory,
      channels = channels,
    )

    ChannelHealthScanBanner(
      scanState = healthScanState,
      onCancel = onCancelHealthScan,
      onDismiss = onDismissHealthScan,
    )

    if (filtered.isEmpty()) {
      EmptyChannelsState(message = "No live channels match your filters.")
    } else {
      LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 110.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
          .fillMaxSize()
          .testTag("all_channels_grid"),
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
fun SavedChannelsScreen(
  favoriteChannels: List<Channel>,
  favoriteIds: Set<String>,
  onPlayChannel: (Channel) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onClearFavorites: () -> Unit,
  onDiscoverChannels: () -> Unit,
  healthScanState: ChannelHealthScanState = ChannelHealthScanState.Idle,
  onScanBrokenChannels: ((List<Channel>, String) -> Unit)? = null,
  onCancelHealthScan: () -> Unit = {},
  onDismissHealthScan: () -> Unit = {},
) {
  val extColors = LocalTiweeColors.current
  val gridState = rememberLazyGridState()
  var searchQuery by remember { mutableStateOf("") }
  var showClearConfirm by remember { mutableStateOf(false) }

  val filtered = remember(favoriteChannels, searchQuery) {
    val trimmed = searchQuery.trim()
    if (trimmed.isEmpty()) {
      favoriteChannels
    } else {
      favoriteChannels.filter { ch ->
        ch.name.contains(trimmed, ignoreCase = true) ||
          ch.categories.any { it.contains(trimmed, ignoreCase = true) }
      }
    }
  }

  if (showClearConfirm) {
    AlertDialog(
      onDismissRequest = { showClearConfirm = false },
      title = { Text("Remove all favorites?") },
      text = { Text("This will remove all ${favoriteChannels.size} saved channels from your synced Favorites list.") },
      confirmButton = {
        Button(
          onClick = {
            onClearFavorites()
            showClearConfirm = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = TiweeLiveRed),
          modifier = Modifier.testTag("confirm_clear_favorites_button"),
        ) {
          Text("Remove All")
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearConfirm = false }) {
          Text("Cancel")
        }
      },
    )
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
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(extColors.favoriteRose.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Favorite,
              contentDescription = null,
              tint = extColors.favoriteRose,
              modifier = Modifier.size(22.dp),
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Saved Channels",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "${favoriteChannels.size} channels synced to cloud",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        if (favoriteChannels.isNotEmpty()) {
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onScanBrokenChannels != null) {
              CleanOfflineChip(
                isScanning = healthScanState is ChannelHealthScanState.Scanning,
                onClick = { onScanBrokenChannels(favoriteChannels, "Favorites") },
              )
            }
            TextButton(
              onClick = { showClearConfirm = true },
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("clear_all_favorites_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.DeleteSweep,
                contentDescription = null,
                tint = TiweeLiveRed,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Clear",
                style = MaterialTheme.typography.labelLarge,
                color = TiweeLiveRed,
              )
            }
          }
        }
      }
    }

    ChannelHealthScanBanner(
      scanState = healthScanState,
      onCancel = onCancelHealthScan,
      onDismiss = onDismissHealthScan,
    )

    if (favoriteChannels.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Box(
            modifier = Modifier
              .size(72.dp)
              .clip(RoundedCornerShape(22.dp))
              .background(extColors.favoriteRose.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Favorite,
              contentDescription = null,
              tint = extColors.favoriteRose,
              modifier = Modifier.size(34.dp),
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "No favorites saved yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Tap the heart icon on any live channel card to pin it here for instant access.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(20.dp))
          Button(
            onClick = onDiscoverChannels,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TiweePurple),
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("discover_channels_cta"),
          ) {
            Icon(Icons.Rounded.Explore, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Discover Channels")
          }
        }
      }
    } else {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search saved channels...") },
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
          .testTag("saved_channels_search_input"),
      )

      if (filtered.isEmpty()) {
        EmptyChannelsState(message = "No saved channels match \"$searchQuery\".")
      } else {
        LazyVerticalGrid(
          state = gridState,
          columns = GridCells.Adaptive(minSize = 110.dp),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier
            .fillMaxSize()
            .testTag("saved_channels_grid"),
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
}
