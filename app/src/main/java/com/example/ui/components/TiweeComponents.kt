package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.LocalMovies
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Weekend
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Channel
import com.example.ui.ChannelHealthScanState
import com.example.ui.theme.LocalTiweeColors
import com.example.ui.theme.TiweeAmber
import com.example.ui.theme.TiweeCyan
import com.example.ui.theme.TiweeEmerald
import com.example.ui.theme.TiweeFavoriteRose
import com.example.ui.theme.TiweeLiveRed
import com.example.ui.theme.TiweePurple

@Immutable
data class GenreVisual(
  val icon: ImageVector,
  val accent: Color,
)

@Immutable
data class MenuCardSpec(
  val title: String,
  val categoryId: String?,
  val visual: GenreVisual,
)

object GenreStyleRegistry {
  val menuCardDefaults: List<MenuCardSpec> =
    listOf(
      MenuCardSpec("Live TV", null, GenreVisual(Icons.Rounded.LiveTv, TiweeLiveRed)),
      MenuCardSpec("Movies", "movies", GenreVisual(Icons.Rounded.LocalMovies, TiweeAmber)),
      MenuCardSpec("Series", "series", GenreVisual(Icons.Rounded.Movie, Color(0xFF8B5CF6))),
      MenuCardSpec("Animation", "animation", GenreVisual(Icons.Rounded.Animation, Color(0xFFF97316))),
      MenuCardSpec("Music", "music", GenreVisual(Icons.Rounded.MusicNote, Color(0xFFEC4899))),
      MenuCardSpec("Auto", "auto", GenreVisual(Icons.Rounded.DirectionsCar, Color(0xFFEF4444))),
      MenuCardSpec("Sport", "sports", GenreVisual(Icons.Rounded.SportsSoccer, TiweeEmerald)),
      MenuCardSpec("News", "news", GenreVisual(Icons.Rounded.Newspaper, Color(0xFF3B82F6))),
      MenuCardSpec("Cooking", "cooking", GenreVisual(Icons.Rounded.Restaurant, TiweeFavoriteRose)),
      MenuCardSpec("Kids", "kids", GenreVisual(Icons.Rounded.ChildCare, Color(0xFFEAB308))),
      MenuCardSpec("Education", "education", GenreVisual(Icons.Rounded.MenuBook, Color(0xFF6366F1))),
      MenuCardSpec("Business", "business", GenreVisual(Icons.Rounded.BusinessCenter, Color(0xFF14B8A6))),
      MenuCardSpec("Relaxation", "relax", GenreVisual(Icons.Rounded.Spa, Color(0xFF2DD4BF))),
      MenuCardSpec("Entertainment", "entertainment", GenreVisual(Icons.Rounded.TheaterComedy, TiweePurple)),
      MenuCardSpec("Lifestyle", "lifestyle", GenreVisual(Icons.Rounded.Weekend, Color(0xFFA855F7))),
      MenuCardSpec("Science", "science", GenreVisual(Icons.Rounded.Science, TiweeCyan)),
      MenuCardSpec("Comedy", "comedy", GenreVisual(Icons.Rounded.SentimentVerySatisfied, Color(0xFFD946EF))),
      MenuCardSpec("Family", "family", GenreVisual(Icons.Rounded.FamilyRestroom, Color(0xFF38BDF8))),
      MenuCardSpec("Shop", "shop", GenreVisual(Icons.Rounded.ShoppingBag, Color(0xFFFB7185))),
    )

  fun visualForCategory(categoryId: String): GenreVisual {
    return when (categoryId.lowercase()) {
      "news" -> GenreVisual(Icons.Rounded.Newspaper, Color(0xFF3B82F6))
      "sports", "sport" -> GenreVisual(Icons.Rounded.SportsSoccer, TiweeEmerald)
      "movies", "movie" -> GenreVisual(Icons.Rounded.LocalMovies, TiweeAmber)
      "series", "classic" -> GenreVisual(Icons.Rounded.Movie, Color(0xFF8B5CF6))
      "music" -> GenreVisual(Icons.Rounded.MusicNote, Color(0xFFEC4899))
      "animation" -> GenreVisual(Icons.Rounded.Animation, Color(0xFFF97316))
      "kids" -> GenreVisual(Icons.Rounded.ChildCare, Color(0xFFEAB308))
      "comedy" -> GenreVisual(Icons.Rounded.SentimentVerySatisfied, Color(0xFFD946EF))
      "science" -> GenreVisual(Icons.Rounded.Science, TiweeCyan)
      "education" -> GenreVisual(Icons.Rounded.MenuBook, Color(0xFF6366F1))
      "documentary" -> GenreVisual(Icons.Rounded.AutoAwesome, Color(0xFF14B8A6))
      "cooking" -> GenreVisual(Icons.Rounded.Restaurant, TiweeFavoriteRose)
      "auto", "automotive" -> GenreVisual(Icons.Rounded.DirectionsCar, Color(0xFFEF4444))
      "business", "legislative" -> GenreVisual(Icons.Rounded.BusinessCenter, Color(0xFF14B8A6))
      "relax", "relaxation" -> GenreVisual(Icons.Rounded.Spa, Color(0xFF2DD4BF))
      "entertainment" -> GenreVisual(Icons.Rounded.TheaterComedy, TiweePurple)
      "lifestyle", "travel" -> GenreVisual(Icons.Rounded.Weekend, Color(0xFFA855F7))
      "family", "culture" -> GenreVisual(Icons.Rounded.FamilyRestroom, Color(0xFF38BDF8))
      "shop", "shopping" -> GenreVisual(Icons.Rounded.ShoppingBag, Color(0xFFFB7185))
      else -> GenreVisual(Icons.Rounded.Tv, TiweePurple)
    }
  }
}

@Composable
fun TiweeTopBar(
  title: String?,
  canGoBack: Boolean,
  isCloudSynced: Boolean,
  userDisplayName: String?,
  userPhotoUrl: String?,
  onBack: () -> Unit,
  onSearchClick: () -> Unit,
  onSettingsClick: () -> Unit,
  onTranscribeClick: (() -> Unit)? = null,
  showSettingsButton: Boolean = true,
) {
  val extColors = LocalTiweeColors.current

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(horizontal = 14.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f),
    ) {
      if (canGoBack) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, extColors.cardBorder),
        ) {
          IconButton(
            onClick = onBack,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("top_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = "Navigate back",
              tint = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
        Spacer(modifier = Modifier.width(10.dp))
      } else {
        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
              Brush.linearGradient(listOf(TiweePurple, Color(0xFF6D28D9)))
            ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.LiveTv,
            contentDescription = "UiPTv Player Pro+ logo",
            tint = Color.White,
            modifier = Modifier.size(19.dp),
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
      }

      Column {
        Text(
          text = title ?: "UiPTv Player Pro+",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (title == null) {
          Text(
            text = "Live IPTV",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
          )
        }
      }
    }

    Spacer(modifier = Modifier.width(6.dp))

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      if (onTranscribeClick != null) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, extColors.cardBorder),
        ) {
          IconButton(
            onClick = onTranscribeClick,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("top_transcribe_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Mic,
              contentDescription = "Transcribe audio",
              tint = MaterialTheme.colorScheme.secondary,
            )
          }
        }
      }

      // Search Quick Button
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extColors.cardBorder),
      ) {
        IconButton(
          onClick = onSearchClick,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("top_search_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = "Search channels",
            tint = MaterialTheme.colorScheme.onSurface,
          )
        }
      }

      // Cloud Sync Avatar / Settings button
      if (showSettingsButton) {
        Surface(
          onClick = onSettingsClick,
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(
            1.dp,
            if (isCloudSynced) extColors.syncedEmerald.copy(alpha = 0.45f) else extColors.cardBorder,
          ),
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("top_settings_button"),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
          ) {
            if (!userPhotoUrl.isNullOrBlank()) {
              var avatarFailed by remember(userPhotoUrl) { mutableStateOf(false) }
              if (!avatarFailed) {
                AsyncImage(
                  model = userPhotoUrl,
                  contentDescription = userDisplayName ?: "Account",
                  contentScale = ContentScale.Crop,
                  onError = { avatarFailed = true },
                  modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape),
                )
              } else {
                Icon(
                  imageVector = Icons.Rounded.Settings,
                  contentDescription = "Settings",
                  tint = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.size(20.dp),
                )
              }
            } else {
              Icon(
                imageVector = if (isCloudSynced) Icons.Rounded.CloudDone else Icons.Rounded.Settings,
                contentDescription = "Settings",
                tint = if (isCloudSynced) extColors.syncedEmerald else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
              )
            }
            if (isCloudSynced) {
              Spacer(modifier = Modifier.width(6.dp))
              Box(
                modifier = Modifier
                  .size(7.dp)
                  .clip(CircleShape)
                  .background(extColors.syncedEmerald)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun ChannelCard(
  channel: Channel,
  isFavorite: Boolean,
  onClick: () -> Unit,
  onToggleFavorite: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val extColors = LocalTiweeColors.current
  val context = LocalContext.current
  val bestQuality = channel.streams.firstOrNull()?.quality
  var showPlaceholder by remember(channel.logo) { mutableStateOf(channel.logo.isNullOrBlank()) }
  val imageRequest = remember(context, channel.logo) {
    channel.logo?.takeIf { it.isNotBlank() }?.let { url ->
      ImageRequest.Builder(context)
        .data(url)
        .size(160)
        .crossfade(false)
        .build()
    }
  }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, extColors.cardBorder),
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .testTag("channel_card_${channel.id}"),
  ) {
    Column(
      modifier = Modifier.padding(6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(1.55f)
          .clip(RoundedCornerShape(10.dp))
          .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center,
      ) {
        if (showPlaceholder || imageRequest == null) {
          ChannelLogoPlaceholder(name = channel.name)
        }
        if (imageRequest != null) {
          AsyncImage(
            model = imageRequest,
            contentDescription = channel.name,
            contentScale = ContentScale.Fit,
            onSuccess = { showPlaceholder = false },
            onError = { showPlaceholder = true },
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 14.dp, vertical = 10.dp),
          )
        }

        // Favorite Heart (Top-Left)
        IconButton(
          onClick = onToggleFavorite,
          modifier = Modifier
            .align(Alignment.TopStart)
            .minimumInteractiveComponentSize()
            .testTag("favorite_toggle_${channel.id}"),
        ) {
          Box(
            modifier = Modifier
              .size(26.dp)
              .clip(CircleShape)
              .background(Color.Black.copy(alpha = 0.58f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
              contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
              tint = if (isFavorite) extColors.favoriteRose else Color.White.copy(alpha = 0.8f),
              modifier = Modifier.size(14.dp),
            )
          }
        }

        // Quality Badge (Top-Right)
        if (!bestQuality.isNullOrBlank()) {
          Box(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(4.dp)
              .clip(RoundedCornerShape(5.dp))
              .background(Color.Black.copy(alpha = 0.7f)),
          ) {
            Text(
              text = bestQuality.uppercase(),
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            )
          }
        }

        // Country Pill (Bottom-Left)
        if (channel.country.isNotBlank()) {
          Box(
            modifier = Modifier
              .align(Alignment.BottomStart)
              .padding(4.dp)
              .clip(RoundedCornerShape(5.dp))
              .background(TiweePurple.copy(alpha = 0.25f)),
          ) {
            Text(
              text = channel.country.uppercase(),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            )
          }
        }

        // 18+ Badge (Bottom-Right)
        if (channel.isNsfw) {
          Box(
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(4.dp)
              .clip(RoundedCornerShape(5.dp))
              .background(extColors.liveRed.copy(alpha = 0.85f)),
          ) {
            Text(
              text = "18+",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = channel.name,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
      )

      val subtitle = remember(channel.id) {
        channel.categories.firstOrNull()?.replaceFirstChar { it.uppercaseChar() }
          ?: if (channel.streams.size > 1) "${channel.streams.size} sources" else "Live TV"
      }
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
private fun ChannelLogoPlaceholder(name: String) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier.padding(6.dp),
  ) {
    Icon(
      imageVector = Icons.Rounded.Tv,
      contentDescription = null,
      tint = TiweePurple.copy(alpha = 0.65f),
      modifier = Modifier.size(22.dp),
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = name,
      style = MaterialTheme.typography.labelSmall,
      color = Color.White.copy(alpha = 0.55f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
fun CleanOfflineChip(
  isScanning: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val extColors = LocalTiweeColors.current
  Surface(
    onClick = onClick,
    enabled = !isScanning,
    shape = RoundedCornerShape(12.dp),
    color = if (isScanning) TiweePurple.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
    border = BorderStroke(1.dp, extColors.cardBorder),
    modifier = modifier
      .minimumInteractiveComponentSize()
      .testTag("clean_offline_channels_button"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.CleaningServices,
        contentDescription = "Remove not working channels",
        tint = TiweeAmber,
        modifier = Modifier.size(16.dp),
      )
      Spacer(modifier = Modifier.width(5.dp))
      Text(
        text = if (isScanning) "Checking..." else "Clean",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

@Composable
fun ChannelHealthScanBanner(
  scanState: ChannelHealthScanState,
  onCancel: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val extColors = LocalTiweeColors.current
  when (scanState) {
    ChannelHealthScanState.Idle -> Unit

    is ChannelHealthScanState.Scanning -> {
      val progress = if (scanState.total > 0) scanState.checked.toFloat() / scanState.total else 0f
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, TiweePurple.copy(alpha = 0.45f)),
        modifier = modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp)
          .testTag("channel_health_scanning_banner"),
      ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Checking ${scanState.scopeLabel} streams (${scanState.checked}/${scanState.total})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "${scanState.removed} offline channel(s) detected",
                style = MaterialTheme.typography.labelSmall,
                color = TiweeAmber,
              )
            }
            TextButton(
              onClick = onCancel,
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Text("Stop", color = TiweeLiveRed)
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          LinearProgressIndicator(
            progress = { progress },
            color = TiweePurple,
            trackColor = extColors.cardBorder,
            modifier = Modifier
              .fillMaxWidth()
              .height(5.dp)
              .clip(RoundedCornerShape(999.dp)),
          )
        }
      }
    }

    is ChannelHealthScanState.Completed -> {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, TiweeEmerald.copy(alpha = 0.5f)),
        modifier = modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp)
          .testTag("channel_health_completed_banner"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
              imageVector = Icons.Rounded.CheckCircle,
              contentDescription = null,
              tint = TiweeEmerald,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = if (scanState.removed > 0) {
                "Removed ${scanState.removed} not working channel(s) from ${scanState.scopeLabel} (${scanState.checked} checked)."
              } else {
                "All ${scanState.checked} checked channels in ${scanState.scopeLabel} responded."
              },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = "Dismiss scan summary",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
    }

    is ChannelHealthScanState.Failed -> {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, TiweeLiveRed.copy(alpha = 0.5f)),
        modifier = modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp)
          .testTag("channel_health_failed_banner"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
              imageVector = Icons.Rounded.ErrorOutline,
              contentDescription = null,
              tint = TiweeLiveRed,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = scanState.message,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = "Dismiss scan warning",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
    }
  }
}
