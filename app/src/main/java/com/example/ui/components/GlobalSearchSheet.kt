package com.example.ui.components

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Channel
import com.example.ui.theme.LocalTiweeColors
import com.example.ui.theme.TiweePurple

@Composable
fun GlobalSearchOverlay(
  query: String,
  onQueryChange: (String) -> Unit,
  channels: List<Channel>,
  favoriteIds: Set<String>,
  onPlayChannel: (Channel) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onViewAllChannels: () -> Unit,
  onTranscribeClick: (() -> Unit)? = null,
  onDismiss: () -> Unit,
) {
  val extColors = LocalTiweeColors.current
  val quickTags = remember {
    listOf("India", "Pakistan", "Bangladesh", "News", "Sports", "Movies", "Music", "Kids", "Documentary")
  }
  val tagsListState = rememberLazyListState()
  val resultsListState = rememberLazyListState()

  BackHandler(onBack = onDismiss)

  val results = remember(channels, query) {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) {
      channels.take(24)
    } else {
      val countryAliasCode = when (trimmed.lowercase()) {
        "india" -> "IN"
        "pakistan" -> "PK"
        "bangladesh" -> "BD"
        else -> null
      }
      channels.filter { ch ->
        (countryAliasCode != null && ch.country.equals(countryAliasCode, ignoreCase = true)) ||
          ch.name.contains(trimmed, ignoreCase = true) ||
          ch.country.equals(trimmed, ignoreCase = true) ||
          ch.categories.any { it.contains(trimmed, ignoreCase = true) } ||
          ch.altNames.any { it.contains(trimmed, ignoreCase = true) } ||
          (ch.description?.contains(trimmed, ignoreCase = true) == true)
      }.take(60)
    }
  }

  Surface(
    color = MaterialTheme.colorScheme.background.copy(alpha = 0.97f),
    modifier = Modifier
      .fillMaxSize()
      .testTag("global_search_overlay"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "Global Channel Search",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onBackground,
        )
        IconButton(
          onClick = onDismiss,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("global_search_close_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Close search",
            tint = MaterialTheme.colorScheme.onBackground,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search by channel, country code, or genre...") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (query.isNotEmpty()) {
              IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
              }
            }
            if (onTranscribeClick != null) {
              IconButton(
                onClick = onTranscribeClick,
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("search_transcribe_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Mic,
                  contentDescription = "Transcribe voice to search",
                  tint = MaterialTheme.colorScheme.secondary,
                )
              }
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
          .testTag("global_search_input"),
      )

      Spacer(modifier = Modifier.height(10.dp))

      LazyRow(
        state = tagsListState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(quickTags, key = { it }, contentType = { "search_chip" }) { tag ->
          val active = query.equals(tag, ignoreCase = true)
          Surface(
            onClick = { onQueryChange(if (active) "" else tag) },
            shape = RoundedCornerShape(12.dp),
            color = if (active) TiweePurple else MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, if (active) TiweePurple else extColors.cardBorder),
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("search_chip_${tag.lowercase()}"),
          ) {
            Text(
              text = tag,
              style = MaterialTheme.typography.labelMedium,
              color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = if (query.isBlank()) "Popular live channels (${channels.size} total)" else "${results.size} matches found",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(modifier = Modifier.height(8.dp))

      LazyColumn(
        state = resultsListState,
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.weight(1f),
      ) {
        items(
          items = results,
          key = { "search_${it.id}" },
          contentType = { "search_channel_row" },
        ) { channel ->
          val context = LocalContext.current
          val isFav = channel.id in favoriteIds
          var logoFailed by remember(channel.logo) { mutableStateOf(channel.logo.isNullOrBlank()) }
          val logoRequest = remember(context, channel.logo) {
            channel.logo?.takeIf { it.isNotBlank() }?.let { url ->
              ImageRequest.Builder(context)
                .data(url)
                .size(128)
                .crossfade(false)
                .build()
            }
          }
          Surface(
            onClick = {
              onPlayChannel(channel)
              onDismiss()
            },
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, extColors.cardBorder),
            modifier = Modifier
              .fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("search_result_${channel.id}"),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
              ) {
                if (logoFailed || logoRequest == null) {
                  Icon(
                    imageVector = Icons.Rounded.LiveTv,
                    contentDescription = null,
                    tint = TiweePurple,
                  )
                } else {
                  AsyncImage(
                    model = logoRequest,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Fit,
                    onError = { logoFailed = true },
                    modifier = Modifier
                      .fillMaxSize()
                      .padding(6.dp),
                  )
                }
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = channel.name,
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                val meta = remember(channel.id) {
                  buildString {
                    if (channel.country.isNotBlank()) append(channel.country.uppercase())
                    val cat = channel.categories.firstOrNull()
                    if (!cat.isNullOrBlank()) {
                      if (isNotEmpty()) append(" • ")
                      append(cat.replaceFirstChar { it.uppercaseChar() })
                    }
                    val q = channel.streams.firstOrNull()?.quality
                    if (!q.isNullOrBlank()) {
                      if (isNotEmpty()) append(" • ")
                      append(q.uppercase())
                    }
                  }.ifBlank { "Live TV" }
                }
                Text(
                  text = meta,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }

              IconButton(
                onClick = { onToggleFavorite(channel.id) },
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Icon(
                  imageVector = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                  contentDescription = if (isFav) "Remove favorite" else "Add favorite",
                  tint = if (isFav) extColors.favoriteRose else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Play channel",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp),
              )
            }
          }
        }
      }

      Button(
        onClick = {
          onDismiss()
          onViewAllChannels()
        },
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TiweePurple),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("global_search_view_all_button"),
      ) {
        Text("Browse All Channels Grid")
      }
    }
  }
}
