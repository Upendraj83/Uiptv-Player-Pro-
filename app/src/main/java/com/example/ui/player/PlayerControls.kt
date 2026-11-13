package com.example.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.StreamSource
import com.example.ui.theme.TiweeFavoriteRose
import com.example.ui.theme.TiweeLiveRed
import com.example.ui.theme.TiweePurple

@Composable
internal fun PlayerBufferingIndicator(
  channelTitle: String,
  sourceIndex: Int,
  totalSources: Int,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = Color.Black.copy(alpha = 0.72f),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
    modifier = modifier.testTag("player_buffering_indicator"),
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
    ) {
      CircularProgressIndicator(
        color = TiweePurple,
        trackColor = Color.White.copy(alpha = 0.16f),
        strokeWidth = 3.5.dp,
        modifier = Modifier.size(40.dp),
      )
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = "Buffering HLS stream...",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = Color.White,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = if (totalSources > 1) {
          "$channelTitle • Source ${sourceIndex + 1}/$totalSources"
        } else {
          channelTitle
        },
        style = MaterialTheme.typography.bodySmall,
        color = Color.White.copy(alpha = 0.75f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
internal fun PlayerErrorCard(
  title: String,
  sourceCount: Int,
  errorDetail: String?,
  autoRemovedOnError: Boolean = false,
  onRetry: () -> Unit,
  onBack: (() -> Unit)?,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
    border = BorderStroke(1.dp, TiweeLiveRed.copy(alpha = 0.45f)),
    modifier = modifier
      .padding(24.dp)
      .testTag("player_error_card"),
  ) {
    Column(
      modifier = Modifier.padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Icon(
        imageVector = Icons.Rounded.ErrorOutline,
        contentDescription = "Playback error",
        tint = TiweeLiveRed,
        modifier = Modifier.size(44.dp),
      )
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = "Stream Offline or Restricted",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = buildString {
          append("Tried ${sourceCount.coerceAtLeast(1)} stream source(s) for $title.")
          if (!errorDetail.isNullOrBlank()) {
            append("\n$errorDetail")
          }
          if (autoRemovedOnError) {
            append("\nRemoved from your active channel list.")
          }
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(18.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
          onClick = onRetry,
          colors = ButtonDefaults.buttonColors(containerColor = TiweePurple),
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("player_retry_button"),
        ) {
          Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Retry")
        }
        if (onBack != null) {
          OutlinedButton(
            onClick = onBack,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("player_return_catalog_button"),
          ) {
            Text("Back", color = MaterialTheme.colorScheme.onSurface)
          }
        }
      }
    }
  }
}

@Composable
internal fun PlayerControlsOverlay(
  visible: Boolean,
  title: String,
  subtitle: String?,
  logoUrl: String?,
  isLive: Boolean,
  isPlaying: Boolean,
  isBuffering: Boolean,
  isMuted: Boolean,
  volume: Float,
  useFillMode: Boolean,
  isLandscape: Boolean,
  allowFullscreenToggle: Boolean,
  streams: List<StreamSource>,
  sourceIndex: Int,
  showSourcesMenu: Boolean,
  isFavorite: Boolean?,
  sleepRemainingMinutes: Int?,
  onShowSourcesMenuChange: (Boolean) -> Unit,
  onSelectSource: (Int) -> Unit,
  onTogglePlayPause: () -> Unit,
  onSeekToLiveEdge: () -> Unit,
  onToggleMute: () -> Unit,
  onVolumeChange: (Float) -> Unit,
  onReload: () -> Unit,
  onToggleResizeMode: () -> Unit,
  onToggleLandscape: () -> Unit,
  onToggleFavorite: (() -> Unit)?,
  onBack: (() -> Unit)?,
) {
  val livePulseTransition = rememberInfiniteTransition(label = "live_dot_pulse")
  val liveDotAlpha by livePulseTransition.animateFloat(
    initialValue = 1f,
    targetValue = 0.35f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 900),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "live_dot_alpha",
  )

  val currentStream = streams.getOrNull(sourceIndex)

  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(),
    exit = fadeOut(),
    modifier = Modifier.fillMaxSize(),
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      // Top header bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.TopCenter)
          .background(
            Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent))
          )
          .statusBarsPadding()
          .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f),
        ) {
          if (onBack != null) {
            IconButton(
              onClick = onBack,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("player_back_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Exit player",
                tint = Color.White,
              )
            }
          }

          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(Color.White.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
          ) {
            if (!logoUrl.isNullOrBlank()) {
              var logoFailed by remember(logoUrl) { mutableStateOf(false) }
              if (!logoFailed) {
                AsyncImage(
                  model = logoUrl,
                  contentDescription = title,
                  contentScale = ContentScale.Fit,
                  onError = { logoFailed = true },
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                )
              } else {
                Icon(Icons.Rounded.LiveTv, contentDescription = null, tint = Color.White)
              }
            } else {
              Icon(Icons.Rounded.LiveTv, contentDescription = null, tint = Color.White)
            }
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = title,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              if (isLive) {
                Surface(shape = RoundedCornerShape(6.dp), color = TiweeLiveRed) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  ) {
                    Box(
                      modifier = Modifier
                        .size(6.dp)
                        .alpha(if (isPlaying && !isBuffering) liveDotAlpha else 0.7f)
                        .clip(CircleShape)
                        .background(Color.White),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "LIVE",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      color = Color.White,
                    )
                  }
                }
              }

              val quality = currentStream?.quality
              if (!quality.isNullOrBlank()) {
                Surface(shape = RoundedCornerShape(6.dp), color = Color.White.copy(alpha = 0.18f)) {
                  Text(
                    text = quality.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  )
                }
              }

              if (!subtitle.isNullOrBlank()) {
                Text(
                  text = subtitle,
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White.copy(alpha = 0.75f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }

              if (sleepRemainingMinutes != null) {
                Surface(shape = RoundedCornerShape(6.dp), color = TiweePurple.copy(alpha = 0.75f)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Bedtime,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(11.dp),
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                      text = "${sleepRemainingMinutes}m",
                      style = MaterialTheme.typography.labelSmall,
                      color = Color.White,
                    )
                  }
                }
              }
            }
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (streams.size > 1) {
            Box {
              Surface(
                onClick = { onShowSourcesMenuChange(true) },
                shape = RoundedCornerShape(10.dp),
                color = Color.White.copy(alpha = 0.15f),
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("player_sources_button"),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = "Select stream source",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "${sourceIndex + 1}/${streams.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                  )
                }
              }

              DropdownMenu(
                expanded = showSourcesMenu,
                onDismissRequest = { onShowSourcesMenuChange(false) },
              ) {
                streams.forEachIndexed { idx, src ->
                  val label = buildString {
                    append("Source ${idx + 1}")
                    if (!src.quality.isNullOrBlank()) append(" (${src.quality.uppercase()})")
                    if (src.title.isNotBlank()) append(" • ${src.title}")
                  }
                  DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                      onSelectSource(idx)
                      onShowSourcesMenuChange(false)
                    },
                  )
                }
              }
            }
          }

          if (isFavorite != null && onToggleFavorite != null) {
            IconButton(
              onClick = onToggleFavorite,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("player_favorite_button"),
            ) {
              Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove from favorites" else "Save to favorites",
                tint = if (isFavorite) TiweeFavoriteRose else Color.White,
              )
            }
          }
        }
      }

      // Bottom transport & utility controls (responsive across portrait & landscape widths)
      BoxWithConstraints(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.BottomCenter)
          .background(
            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f)))
          )
          .navigationBarsPadding()
          .padding(horizontal = 16.dp, vertical = 14.dp),
      ) {
        val isCompactWidth = maxWidth < 430.dp
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          if (isCompactWidth) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              IconButton(
                onClick = onToggleMute,
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("player_mute_button"),
              ) {
                Icon(
                  imageVector = if (isMuted || volume == 0f) {
                    Icons.AutoMirrored.Rounded.VolumeOff
                  } else {
                    Icons.AutoMirrored.Rounded.VolumeUp
                  },
                  contentDescription = if (isMuted) "Unmute" else "Mute",
                  tint = Color.White,
                )
              }
              Slider(
                value = if (isMuted) 0f else volume,
                onValueChange = onVolumeChange,
                colors = SliderDefaults.colors(
                  thumbColor = TiweePurple,
                  activeTrackColor = TiweePurple,
                  inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                ),
                modifier = Modifier
                  .weight(1f)
                  .testTag("player_volume_slider"),
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier
                  .size(48.dp)
                  .clip(CircleShape)
                  .background(TiweePurple)
                  .testTag("player_play_pause_button"),
              ) {
                Icon(
                  imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                  contentDescription = if (isPlaying) "Pause stream" else "Play stream",
                  tint = Color.White,
                )
              }

              if (isLive) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  onClick = onSeekToLiveEdge,
                  shape = RoundedCornerShape(10.dp),
                  color = Color.White.copy(alpha = 0.14f),
                  modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("player_live_edge_button"),
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Sensors,
                      contentDescription = "Jump to live edge",
                      tint = Color.White,
                      modifier = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "LIVE",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      color = Color.White,
                    )
                  }
                }
              }

              if (!isCompactWidth) {
                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                  onClick = onToggleMute,
                  modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("player_mute_button"),
                ) {
                  Icon(
                    imageVector = if (isMuted || volume == 0f) {
                      Icons.AutoMirrored.Rounded.VolumeOff
                    } else {
                      Icons.AutoMirrored.Rounded.VolumeUp
                    },
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = Color.White,
                  )
                }

                Slider(
                  value = if (isMuted) 0f else volume,
                  onValueChange = onVolumeChange,
                  colors = SliderDefaults.colors(
                    thumbColor = TiweePurple,
                    activeTrackColor = TiweePurple,
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                  ),
                  modifier = Modifier
                    .width(92.dp)
                    .testTag("player_volume_slider"),
                )
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = onReload,
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("player_reload_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Refresh,
                  contentDescription = "Reload stream",
                  tint = Color.White,
                )
              }

              IconButton(
                onClick = onToggleResizeMode,
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("player_resize_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.AspectRatio,
                  contentDescription = if (useFillMode) "Fit video" else "Fill video",
                  tint = if (useFillMode) TiweePurple else Color.White,
                )
              }

              if (allowFullscreenToggle) {
                IconButton(
                  onClick = onToggleLandscape,
                  modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("player_fullscreen_button"),
                ) {
                  Icon(
                    imageVector = if (isLandscape) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                    contentDescription = if (isLandscape) "Exit landscape" else "Rotate to landscape",
                    tint = Color.White,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
