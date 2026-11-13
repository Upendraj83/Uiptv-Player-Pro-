package com.example.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.StreamSource
import kotlinx.coroutines.delay

private const val PLAYER_TAG = "PlayerScreen"

private fun sanitizeHttpHeaderValue(raw: String?): String? {
  if (raw.isNullOrBlank()) return null
  val cleaned = buildString(raw.length) {
    for (ch in raw.trim()) {
      if (ch.code in 0x20..0x7E) append(ch)
    }
  }.trim()
  return cleaned.takeIf { it.isNotEmpty() }
}

private tailrec fun Context.findActivity(): Activity? =
  when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
  }

/**
 * Reusable Media3 ExoPlayer HLS/HTTP live streaming screen with multi-source failover,
 * buffering indicator, auto-hiding playback controls, volume/mute, resize mode, and rotation toggle.
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
  streams: List<StreamSource>,
  title: String,
  modifier: Modifier = Modifier,
  streamKey: String = streams.firstOrNull()?.url ?: title,
  subtitle: String? = null,
  logoUrl: String? = null,
  isLive: Boolean = true,
  isFavorite: Boolean? = null,
  sleepRemainingMinutes: Int? = null,
  autoHideControlsMillis: Long = 3500L,
  allowFullscreenToggle: Boolean = true,
  onToggleFavorite: (() -> Unit)? = null,
  onStreamFailed: (() -> Unit)? = null,
  onStreamReady: (() -> Unit)? = null,
  onBack: (() -> Unit)? = null,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val activity = remember(context) { context.findActivity() }

  var sourceIndex by remember(streamKey) { mutableIntStateOf(0) }
  var reloadCounter by remember(streamKey) { mutableIntStateOf(0) }
  var isBuffering by remember(streamKey) { mutableStateOf(true) }
  var isPlaying by remember(streamKey) { mutableStateOf(true) }
  var isMuted by remember { mutableStateOf(false) }
  var volume by remember { mutableFloatStateOf(1f) }
  var useFillMode by remember { mutableStateOf(false) }
  var isLandscape by remember { mutableStateOf(false) }
  var allSourcesFailed by remember(streamKey) { mutableStateOf(false) }
  var lastErrorMessage by remember(streamKey) { mutableStateOf<String?>(null) }
  var controlsVisible by remember { mutableStateOf(true) }
  var showSourcesMenu by remember { mutableStateOf(false) }

  val currentStream = streams.getOrNull(sourceIndex)

  if (onBack != null) {
    BackHandler {
      if (allowFullscreenToggle) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
      }
      onBack()
    }
  }

  DisposableEffect(activity, allowFullscreenToggle) {
    onDispose {
      if (allowFullscreenToggle) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
      }
    }
  }

  LaunchedEffect(controlsVisible, isBuffering, allSourcesFailed, showSourcesMenu, autoHideControlsMillis) {
    if (autoHideControlsMillis > 0L && controlsVisible && !isBuffering && !allSourcesFailed && !showSourcesMenu) {
      delay(autoHideControlsMillis)
      controlsVisible = false
    }
  }

  val exoPlayer = remember(context) {
    val renderersFactory = DefaultRenderersFactory(context)
      .setEnableDecoderFallback(true)
    ExoPlayer.Builder(context, renderersFactory)
      .setWakeMode(C.WAKE_MODE_LOCAL)
      .build()
      .apply {
        playWhenReady = true
      }
  }

  var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

  DisposableEffect(exoPlayer, lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
        Lifecycle.Event.ON_RESUME -> {
          if (!allSourcesFailed && streams.isNotEmpty()) {
            if (exoPlayer.playbackState == Player.STATE_IDLE) {
              exoPlayer.prepare()
            }
            exoPlayer.play()
          }
        }
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      playerViewRef?.player = null
      playerViewRef = null
      runCatching {
        exoPlayer.stop()
        exoPlayer.release()
      }.onFailure { err ->
        Log.w(PLAYER_TAG, "Failed releasing ExoPlayer", err)
      }
    }
  }

  DisposableEffect(exoPlayer, streamKey, streams.size) {
    val listener = object : Player.Listener {
      override fun onPlaybackStateChanged(playbackState: Int) {
        isBuffering = playbackState == Player.STATE_BUFFERING
        if (playbackState == Player.STATE_READY) {
          allSourcesFailed = false
          lastErrorMessage = null
          onStreamReady?.invoke()
        }
      }

      override fun onIsPlayingChanged(playing: Boolean) {
        isPlaying = playing
      }

      override fun onPlayerError(error: PlaybackException) {
        lastErrorMessage = error.errorCodeName
        val nextIndex = sourceIndex + 1
        if (nextIndex < streams.size) {
          sourceIndex = nextIndex
        } else {
          isBuffering = false
          allSourcesFailed = true
          onStreamFailed?.invoke()
        }
      }
    }
    exoPlayer.addListener(listener)
    onDispose { exoPlayer.removeListener(listener) }
  }

  LaunchedEffect(streamKey, sourceIndex, reloadCounter) {
    val stream = streams.getOrNull(sourceIndex)
    if (stream == null) {
      allSourcesFailed = true
      isBuffering = false
      lastErrorMessage = "No playable stream URL available."
      return@LaunchedEffect
    }
    allSourcesFailed = false
    lastErrorMessage = null
    isBuffering = true

    try {
      val trimmedUrl = stream.url.trim()
      val parsedUri = Uri.parse(trimmedUrl)
      val scheme = parsedUri.scheme?.lowercase()
      require(scheme == "http" || scheme == "https") {
        "Unsupported stream protocol: ${scheme ?: "unknown"}"
      }

      val safeUserAgent = sanitizeHttpHeaderValue(stream.userAgent) ?: "UiPTvPlayer/1.0 (Android)"
      val safeReferrer = sanitizeHttpHeaderValue(stream.referrer)
      val headers = buildMap {
        put("User-Agent", safeUserAgent)
        if (safeReferrer != null) put("Referer", safeReferrer)
      }

      val httpFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(10_000)
        .setReadTimeoutMs(12_000)
        .setUserAgent(safeUserAgent)
        .setDefaultRequestProperties(headers)

      val isHlsUrl = trimmedUrl.contains(".m3u8", ignoreCase = true)

      val mediaItemBuilder = MediaItem.Builder().setUri(parsedUri)
      if (isHlsUrl) {
        mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
      }
      val mediaItem = mediaItemBuilder.build()

      val mediaSource =
        if (isHlsUrl) {
          HlsMediaSource.Factory(httpFactory)
            .setAllowChunklessPreparation(true)
            .createMediaSource(mediaItem)
        } else {
          DefaultMediaSourceFactory(httpFactory).createMediaSource(mediaItem)
        }

      exoPlayer.stop()
      exoPlayer.clearMediaItems()
      exoPlayer.setMediaSource(mediaSource)
      exoPlayer.prepare()
      exoPlayer.playWhenReady = true
    } catch (err: Throwable) {
      Log.w(PLAYER_TAG, "Failed preparing stream source $sourceIndex: ${stream.url}", err)
      lastErrorMessage = err.localizedMessage ?: "Unable to open stream"
      val nextIndex = sourceIndex + 1
      if (nextIndex < streams.size) {
        sourceIndex = nextIndex
      } else {
        isBuffering = false
        allSourcesFailed = true
        onStreamFailed?.invoke()
      }
    }
  }

  LaunchedEffect(isMuted, volume) {
    exoPlayer.volume = if (isMuted) 0f else volume.coerceIn(0f, 1f)
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black)
      .testTag("player_screen"),
  ) {
    AndroidView(
      factory = { ctx ->
        PlayerView(ctx).apply {
          useController = false
          keepScreenOn = true
          setShutterBackgroundColor(android.graphics.Color.BLACK)
          player = exoPlayer
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
          )
          playerViewRef = this
        }
      },
      update = { view ->
        playerViewRef = view
        if (view.player !== exoPlayer) {
          view.player = exoPlayer
        }
        view.resizeMode =
          if (useFillMode) AspectRatioFrameLayout.RESIZE_MODE_ZOOM
          else AspectRatioFrameLayout.RESIZE_MODE_FIT
      },
      onRelease = { view ->
        view.player = null
        if (playerViewRef === view) {
          playerViewRef = null
        }
      },
      modifier = Modifier.fillMaxSize(),
    )

    // Dedicated video tap surface above AndroidView so sibling overlay/error semantics remain unmerged
    Box(
      modifier = Modifier
        .fillMaxSize()
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
        ) {
          controlsVisible = !controlsVisible
        },
    )

    if (isBuffering && !allSourcesFailed) {
      PlayerBufferingIndicator(
        channelTitle = currentStream?.title?.ifBlank { title } ?: title,
        sourceIndex = sourceIndex,
        totalSources = streams.size,
        modifier = Modifier.align(Alignment.Center),
      )
    }

    PlayerControlsOverlay(
      visible = controlsVisible || allSourcesFailed,
      title = title,
      subtitle = subtitle,
      logoUrl = logoUrl,
      isLive = isLive,
      isPlaying = isPlaying,
      isBuffering = isBuffering,
      isMuted = isMuted,
      volume = volume.coerceIn(0f, 1f),
      useFillMode = useFillMode,
      isLandscape = isLandscape,
      allowFullscreenToggle = allowFullscreenToggle,
      streams = streams,
      sourceIndex = sourceIndex,
      showSourcesMenu = showSourcesMenu,
      isFavorite = isFavorite,
      sleepRemainingMinutes = sleepRemainingMinutes,
      onShowSourcesMenuChange = { showSourcesMenu = it },
      onSelectSource = { idx -> sourceIndex = idx },
      onTogglePlayPause = {
        if (exoPlayer.isPlaying) {
          exoPlayer.pause()
        } else if (allSourcesFailed || exoPlayer.playbackState == Player.STATE_IDLE) {
          reloadCounter += 1
        } else {
          exoPlayer.play()
        }
      },
      onSeekToLiveEdge = {
        if (allSourcesFailed || exoPlayer.playbackState == Player.STATE_IDLE) {
          reloadCounter += 1
        } else {
          exoPlayer.seekToDefaultPosition()
          exoPlayer.play()
        }
      },
      onToggleMute = { isMuted = !isMuted },
      onVolumeChange = { next ->
        volume = next.coerceIn(0f, 1f)
        if (volume > 0f && isMuted) isMuted = false
      },
      onReload = { reloadCounter += 1 },
      onToggleResizeMode = { useFillMode = !useFillMode },
      onToggleLandscape = {
        isLandscape = !isLandscape
        activity?.requestedOrientation =
          if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
          else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
      },
      onToggleFavorite = onToggleFavorite,
      onBack = if (onBack != null) {
        {
          if (allowFullscreenToggle) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
          }
          onBack()
        }
      } else {
        null
      },
    )

    if (allSourcesFailed) {
      PlayerErrorCard(
        title = title,
        sourceCount = streams.size,
        errorDetail = lastErrorMessage,
        autoRemovedOnError = onStreamFailed != null,
        onRetry = {
          sourceIndex = 0
          reloadCounter += 1
        },
        onBack = if (onBack != null) {
          {
            if (allowFullscreenToggle) {
              activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            onBack()
          }
        } else {
          null
        },
        modifier = Modifier.align(Alignment.Center),
      )
    }
  }
}

/**
 * Convenience overload for playing a single HLS/HTTP stream URL directly.
 */
@Composable
fun PlayerScreen(
  streamUrl: String,
  title: String,
  modifier: Modifier = Modifier,
  quality: String? = null,
  userAgent: String? = null,
  referrer: String? = null,
  logoUrl: String? = null,
  isLive: Boolean = true,
  onBack: (() -> Unit)? = null,
) {
  val singleSource = remember(streamUrl, title, quality, userAgent, referrer) {
    listOf(
      StreamSource(
        url = streamUrl,
        title = title,
        quality = quality,
        userAgent = userAgent,
        referrer = referrer,
      )
    )
  }
  PlayerScreen(
    streams = singleSource,
    title = title,
    modifier = modifier,
    streamKey = streamUrl,
    logoUrl = logoUrl,
    isLive = isLive,
    onBack = onBack,
  )
}
