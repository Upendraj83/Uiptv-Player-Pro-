package com.example.ai

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.MicrophoneAudioRecorder
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File

sealed interface TranscriptionUiState {
  data object Idle : TranscriptionUiState
  data class Recording(
    val elapsedSeconds: Int,
    val amplitudes: List<Float>,
  ) : TranscriptionUiState
  data object Transcribing : TranscriptionUiState
  data class Streaming(val text: String) : TranscriptionUiState
  data class Success(val transcript: String) : TranscriptionUiState
  data class Error(val message: String) : TranscriptionUiState
}

class AudioTranscriptionViewModel(
  application: Application,
) : AndroidViewModel(application) {

  private val recorder = MicrophoneAudioRecorder(application.applicationContext)

  private val transcriptionModel by lazy {
    Firebase.ai(backend = GenerativeBackend.googleAI())
      .generativeModel("gemini-3.5-transcribe")
  }

  private val _uiState = MutableStateFlow<TranscriptionUiState>(TranscriptionUiState.Idle)
  val uiState: StateFlow<TranscriptionUiState> = _uiState.asStateFlow()

  private var meterJob: Job? = null
  private var transcriptionJob: Job? = null

  fun startRecording() {
    transcriptionJob?.cancel()
    meterJob?.cancel()

    try {
      recorder.startRecording()
    } catch (e: Exception) {
      Log.e(TAG, "Unable to start microphone recording", e)
      _uiState.value = TranscriptionUiState.Error(
        e.localizedMessage ?: "Microphone could not be accessed. Please check permissions."
      )
      return
    }

    val startTimeMs = System.currentTimeMillis()
    val bars = MutableList(WAVEFORM_BAR_COUNT) { 0.08f }
    _uiState.value = TranscriptionUiState.Recording(elapsedSeconds = 0, amplitudes = bars.toList())

    meterJob = viewModelScope.launch {
      while (true) {
        delay(90L)
        val amp = recorder.currentNormalizedAmplitude()
        bars.removeAt(0)
        bars.add(amp)
        val elapsedSec = ((System.currentTimeMillis() - startTimeMs) / 1000L).toInt()
        _uiState.value = TranscriptionUiState.Recording(
          elapsedSeconds = elapsedSec,
          amplitudes = bars.toList(),
        )
        if (elapsedSec >= MAX_RECORDING_SECONDS) {
          stopAndTranscribe()
          break
        }
      }
    }
  }

  fun stopAndTranscribe() {
    meterJob?.cancel()
    meterJob = null

    val audioFile = recorder.stopRecording()
    if (audioFile == null || !audioFile.exists() || audioFile.length() == 0L) {
      _uiState.value = TranscriptionUiState.Error("No audio captured. Hold or tap Record and speak clearly.")
      return
    }

    transcribeFile(audioFile)
  }

  fun cancelRecording() {
    meterJob?.cancel()
    meterJob = null
    transcriptionJob?.cancel()
    transcriptionJob = null
    recorder.cancelRecording()
    _uiState.value = TranscriptionUiState.Idle
  }

  fun reset() {
    cancelRecording()
    _uiState.value = TranscriptionUiState.Idle
  }

  private fun transcribeFile(audioFile: File) {
    transcriptionJob?.cancel()
    transcriptionJob = viewModelScope.launch {
      _uiState.value = TranscriptionUiState.Transcribing
      try {
        val bytes = withContext(Dispatchers.IO) {
          if (audioFile.length() > MAX_INLINE_AUDIO_BYTES) {
            throw IllegalArgumentException("Recording exceeds the 18 MB inline audio limit.")
          }
          audioFile.readBytes()
        }

        val prompt = content {
          inlineData(bytes, "audio/mp4")
          text("Transcribe the spoken words in this audio recording accurately. Return only the clean transcription text without extra commentary.")
        }

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
          transcriptionModel.generateContentStream(prompt).collect { chunk ->
            val chunkText = chunk.text.orEmpty()
            if (chunkText.isNotEmpty()) {
              accumulated.append(chunkText)
              _uiState.value = TranscriptionUiState.Streaming(accumulated.toString().trim())
            }
          }
        }

        val finalTranscript = accumulated.toString().trim()
        if (finalTranscript.isNotBlank()) {
          _uiState.value = TranscriptionUiState.Success(finalTranscript)
        } else {
          _uiState.value = TranscriptionUiState.Error(
            "No speech could be transcribed from the recording. Try speaking closer to the microphone."
          )
        }
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Audio transcription timed out", e)
        _uiState.value = TranscriptionUiState.Error(
          "Transcription timed out after 60 seconds. Please check your connection and try again."
        )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Audio transcription failed", e)
        _uiState.value = TranscriptionUiState.Error(
          e.localizedMessage ?: "Audio transcription failed."
        )
      } finally {
        withContext(Dispatchers.IO) {
          runCatching { audioFile.delete() }
        }
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    meterJob?.cancel()
    transcriptionJob?.cancel()
    recorder.release()
  }

  private companion object {
    const val TAG = "AudioTranscriptionVM"
    const val WAVEFORM_BAR_COUNT = 22
    const val MAX_RECORDING_SECONDS = 90
    const val MAX_INLINE_AUDIO_BYTES = 18L * 1024L * 1024L
  }
}
