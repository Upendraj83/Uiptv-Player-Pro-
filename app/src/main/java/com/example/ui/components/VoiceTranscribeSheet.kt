package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.AudioTranscriptionViewModel
import com.example.ai.TranscriptionUiState
import com.example.ui.theme.LocalTiweeColors
import com.example.ui.theme.TiweeEmerald
import com.example.ui.theme.TiweeLiveRed
import com.example.ui.theme.TiweePurple
import com.example.ui.theme.TiweePurpleDeep

@Composable
fun VoiceTranscribeOverlay(
  onUseForChannelSearch: (String) -> Unit,
  onDismiss: () -> Unit,
  transcriptionViewModel: AudioTranscriptionViewModel = viewModel(),
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val extColors = LocalTiweeColors.current
  val uiState by transcriptionViewModel.uiState.collectAsStateWithLifecycle()

  var hasMicPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    )
  }
  var permissionDenied by remember { mutableStateOf(false) }
  var copiedFeedback by remember { mutableStateOf(false) }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { granted ->
    hasMicPermission = granted
    permissionDenied = !granted
    if (granted) {
      copiedFeedback = false
      transcriptionViewModel.startRecording()
    }
  }

  BackHandler {
    transcriptionViewModel.cancelRecording()
    onDismiss()
  }

  DisposableEffect(Unit) {
    onDispose {
      if (uiState is TranscriptionUiState.Recording) {
        transcriptionViewModel.cancelRecording()
      }
    }
  }

  Surface(
    color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
    modifier = Modifier
      .fillMaxSize()
      .testTag("voice_transcribe_overlay"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 20.dp, vertical = 14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 600.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(TiweePurple.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.GraphicEq,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(22.dp),
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Transcribe Audio",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
              text = "Powered by gemini-3.5-transcribe",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.secondary,
            )
          }
        }

        IconButton(
          onClick = {
            transcriptionViewModel.cancelRecording()
            onDismiss()
          },
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("transcribe_close_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Close transcription",
            tint = MaterialTheme.colorScheme.onBackground,
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Column(
        modifier = Modifier
          .weight(1f)
          .widthIn(max = 600.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        // Microphone status & pulsing orb
        val isRecording = uiState is TranscriptionUiState.Recording
        val isTranscribing = uiState is TranscriptionUiState.Transcribing

        val pulseTransition = rememberInfiniteTransition(label = "mic_pulse")
        val pulseScale by pulseTransition.animateFloat(
          initialValue = 1f,
          targetValue = if (isRecording) 1.14f else 1f,
          animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse,
          ),
          label = "mic_pulse_scale",
        )

        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.size(136.dp),
        ) {
          if (isRecording) {
            Box(
              modifier = Modifier
                .size(132.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(TiweeLiveRed.copy(alpha = 0.2f)),
            )
          }
          Surface(
            onClick = {
              copiedFeedback = false
              when {
                isRecording -> transcriptionViewModel.stopAndTranscribe()
                isTranscribing -> Unit
                !hasMicPermission -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                else -> transcriptionViewModel.startRecording()
              }
            },
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(
              2.dp,
              if (isRecording) TiweeLiveRed else TiweePurple.copy(alpha = 0.7f),
            ),
            modifier = Modifier
              .size(96.dp)
              .testTag("transcribe_mic_orb_button"),
          ) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(
                  Brush.linearGradient(
                    if (isRecording) listOf(TiweeLiveRed, TiweeLiveRed.copy(alpha = 0.75f))
                    else listOf(TiweePurple, TiweePurpleDeep)
                  )
                ),
              contentAlignment = Alignment.Center,
            ) {
              when {
                isTranscribing -> {
                  CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(34.dp),
                  )
                }
                isRecording -> {
                  Icon(
                    imageVector = Icons.Rounded.Stop,
                    contentDescription = "Stop recording and transcribe",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                  )
                }
                else -> {
                  Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = "Start microphone recording",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (val state = uiState) {
          is TranscriptionUiState.Idle -> {
            Text(
              text = if (permissionDenied) {
                "Microphone permission required"
              } else {
                "Tap the microphone to start speaking"
              },
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onBackground,
              textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = if (permissionDenied) {
                "Please allow microphone access so UiPTv Player can capture your audio and transcribe it."
              } else {
                "Speak naturally to transcribe speech into text or search live TV channels by voice."
              },
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
          }

          is TranscriptionUiState.Recording -> {
            val mins = state.elapsedSeconds / 60
            val secs = state.elapsedSeconds % 60
            Text(
              text = String.format("Listening... %02d:%02d", mins, secs),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = TiweeLiveRed,
            )
            Spacer(modifier = Modifier.height(14.dp))
            WaveformVisualizer(amplitudes = state.amplitudes)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Tap the stop button when finished to transcribe",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          is TranscriptionUiState.Transcribing -> {
            Text(
              text = "Transcribing audio with Gemini...",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Processing your microphone recording",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          is TranscriptionUiState.Streaming -> {
            TranscriptResultCard(
              title = "Live Transcription",
              transcript = state.text,
              isStreaming = true,
              copiedFeedback = false,
              onCopy = null,
              onSearchChannels = null,
              onRecordAgain = null,
            )
          }

          is TranscriptionUiState.Success -> {
            TranscriptResultCard(
              title = "Transcribed Text",
              transcript = state.transcript,
              isStreaming = false,
              copiedFeedback = copiedFeedback,
              onCopy = {
                clipboardManager.setText(AnnotatedString(state.transcript))
                copiedFeedback = true
              },
              onSearchChannels = {
                onUseForChannelSearch(state.transcript)
                onDismiss()
              },
              onRecordAgain = {
                copiedFeedback = false
                if (!hasMicPermission) {
                  permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else {
                  transcriptionViewModel.startRecording()
                }
              },
            )
          }

          is TranscriptionUiState.Error -> {
            Surface(
              shape = RoundedCornerShape(20.dp),
              color = TiweeLiveRed.copy(alpha = 0.12f),
              border = BorderStroke(1.dp, TiweeLiveRed.copy(alpha = 0.4f)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("transcribe_error_card"),
            ) {
              Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Icon(
                  imageVector = Icons.Rounded.ErrorOutline,
                  contentDescription = null,
                  tint = TiweeLiveRed,
                  modifier = Modifier.size(34.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = state.message,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                  onClick = {
                    if (!hasMicPermission) {
                      permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                      transcriptionViewModel.startRecording()
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = TiweePurple),
                  modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("transcribe_retry_button"),
                ) {
                  Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Try Again")
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun WaveformVisualizer(amplitudes: List<Float>) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(52.dp)
      .padding(horizontal = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    amplitudes.forEach { level ->
      val barHeight = (10f + (level * 38f)).dp
      Box(
        modifier = Modifier
          .width(6.dp)
          .height(barHeight)
          .clip(RoundedCornerShape(999.dp))
          .background(
            Brush.verticalGradient(listOf(TiweePurple, TiweeLiveRed))
          ),
      )
    }
  }
}

@Composable
private fun TranscriptResultCard(
  title: String,
  transcript: String,
  isStreaming: Boolean,
  copiedFeedback: Boolean,
  onCopy: (() -> Unit)?,
  onSearchChannels: (() -> Unit)?,
  onRecordAgain: (() -> Unit)?,
) {
  val extColors = LocalTiweeColors.current
  Surface(
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(
      1.dp,
      if (isStreaming) TiweePurple.copy(alpha = 0.55f) else extColors.cardBorder,
    ),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("transcribe_result_card"),
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge,
          color = if (isStreaming) MaterialTheme.colorScheme.secondary else TiweeEmerald,
        )
        if (isStreaming) {
          CircularProgressIndicator(
            color = TiweePurple,
            strokeWidth = 2.dp,
            modifier = Modifier.size(16.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = transcript,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("transcribe_output_text"),
      )

      if (!isStreaming) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          if (onSearchChannels != null) {
            Button(
              onClick = onSearchChannels,
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = TiweePurple),
              modifier = Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("transcribe_search_channels_button"),
            ) {
              Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Search TV")
            }
          }

          if (onCopy != null) {
            OutlinedButton(
              onClick = onCopy,
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, extColors.cardBorder),
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("transcribe_copy_button"),
            ) {
              Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (copiedFeedback) "Copied" else "Copy")
            }
          }
        }

        if (onRecordAgain != null) {
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedButton(
            onClick = onRecordAgain,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, TiweePurple.copy(alpha = 0.45f)),
            modifier = Modifier
              .fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("transcribe_record_again_button"),
          ) {
            Icon(Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Record Another Clip")
          }
        }
      }
    }
  }
}
