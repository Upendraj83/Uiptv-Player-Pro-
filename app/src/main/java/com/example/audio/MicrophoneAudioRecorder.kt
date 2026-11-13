package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import kotlin.math.min

class MicrophoneAudioRecorder(
  private val context: Context,
) {
  private var recorder: MediaRecorder? = null
  private var outputFile: File? = null

  fun startRecording(): File {
    stopAndDiscardInternal()

    val file = File(context.cacheDir, "voice_transcribe_${System.currentTimeMillis()}.m4a")
    val mediaRecorder =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
      } else {
        @Suppress("DEPRECATION")
        MediaRecorder()
      }

    try {
      mediaRecorder.apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setAudioSamplingRate(16_000)
        setAudioEncodingBitRate(64_000)
        setAudioChannels(1)
        setMaxDuration(120_000)
        setOutputFile(file.absolutePath)
        prepare()
        start()
      }
      recorder = mediaRecorder
      outputFile = file
      return file
    } catch (e: Exception) {
      Log.e(TAG, "Failed to start MediaRecorder", e)
      runCatching { mediaRecorder.release() }
      runCatching { file.delete() }
      throw e
    }
  }

  fun stopRecording(): File? {
    val active = recorder ?: return null
    val file = outputFile
    recorder = null
    return try {
      active.stop()
      active.release()
      file?.takeIf { it.exists() && it.length() > 0L }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to stop MediaRecorder cleanly", e)
      runCatching { active.release() }
      runCatching { file?.delete() }
      null
    } finally {
      outputFile = null
    }
  }

  fun cancelRecording() {
    stopAndDiscardInternal()
  }

  fun currentNormalizedAmplitude(): Float {
    val active = recorder ?: return 0f
    return try {
      val raw = active.maxAmplitude
      min(1f, raw / 18_000f).coerceAtLeast(0.05f)
    } catch (_: Exception) {
      0.05f
    }
  }

  fun release() {
    stopAndDiscardInternal()
  }

  private fun stopAndDiscardInternal() {
    val active = recorder
    val file = outputFile
    recorder = null
    outputFile = null
    if (active != null) {
      runCatching { active.stop() }
      runCatching { active.release() }
    }
    if (file != null && file.exists()) {
      runCatching { file.delete() }
    }
  }

  private companion object {
    const val TAG = "MicrophoneAudioRecorder"
  }
}
