package com.example.data.repository

import com.example.data.model.Channel
import com.example.data.model.StreamSource
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.concurrent.TimeUnit

object StreamValidator {

  private val OFFLINE_TITLE_MARKERS = listOf(
    "[offline]",
    "[dead]",
    "[broken]",
    "[error]",
  )

  private val UNSUPPORTED_EXTENSIONS = listOf(
    ".mpd",
    ".ism",
    ".isml",
    ".f4m",
  )

  private val WEBPAGE_DOMAINS = listOf(
    "youtube.com",
    "youtu.be",
    "twitch.tv",
    "dailymotion.com",
    "vimeo.com",
    "facebook.com",
    "instagram.com",
  )

  fun isPlayableStreamUrl(
    rawUrl: String,
    title: String = "",
  ): Boolean {
    val url = rawUrl.trim()
    if (url.isEmpty()) return false

    if (title.isNotBlank()) {
      val lowerTitle = title.lowercase()
      if (OFFLINE_TITLE_MARKERS.any { it in lowerTitle }) return false
    }

    val uri = runCatching { URI(url) }.getOrNull() ?: return false
    val scheme = uri.scheme?.lowercase() ?: return false
    if (scheme != "http" && scheme != "https") return false

    val host = uri.host?.lowercase()?.takeIf { it.isNotBlank() } ?: return false
    if (isPrivateOrLocalHost(host)) return false

    val path = uri.path?.lowercase().orEmpty()
    if (UNSUPPORTED_EXTENSIONS.any { ext -> path.endsWith(ext) || "$ext/" in path || "$ext?" in url.lowercase() }) {
      return false
    }

    if (WEBPAGE_DOMAINS.any { domain -> host == domain || host.endsWith(".$domain") }) {
      if (".m3u8" !in url.lowercase()) return false
    }

    return true
  }

  private fun isPrivateOrLocalHost(host: String): Boolean {
    if (host == "localhost" || host == "0.0.0.0" || host.endsWith(".local")) return true
    if (host.startsWith("127.") || host.startsWith("10.") || host.startsWith("192.168.")) return true
    if (host.startsWith("172.")) {
      val secondOctet = host.removePrefix("172.").substringBefore('.').toIntOrNull()
      if (secondOctet != null && secondOctet in 16..31) return true
    }
    return false
  }

  fun createProbeClient(): OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(3500, TimeUnit.MILLISECONDS)
      .readTimeout(3500, TimeUnit.MILLISECONDS)
      .writeTimeout(3500, TimeUnit.MILLISECONDS)
      .followRedirects(true)
      .followSslRedirects(true)
      .build()

  fun isChannelReachable(
    client: OkHttpClient,
    channel: Channel,
  ): Boolean {
    for (stream in channel.streams) {
      if (isStreamReachable(client, stream)) {
        return true
      }
    }
    return false
  }

  fun isStreamReachable(
    client: OkHttpClient,
    stream: StreamSource,
  ): Boolean {
    if (!isPlayableStreamUrl(stream.url, stream.title)) return false
    return runCatching {
      val lowerUrl = stream.url.lowercase()
      val isHlsUrl = ".m3u8" in lowerUrl || "hls" in lowerUrl
      val requestBuilder = Request.Builder()
        .url(stream.url)
        .get()

      if (!isHlsUrl) {
        requestBuilder.header("Range", "bytes=0-2047")
      }

      if (!stream.userAgent.isNullOrBlank()) {
        requestBuilder.header("User-Agent", stream.userAgent)
      } else {
        requestBuilder.header("User-Agent", "UiPTvPlayer/1.0 (Android)")
      }
      if (!stream.referrer.isNullOrBlank()) {
        requestBuilder.header("Referer", stream.referrer)
      }

      client.newCall(requestBuilder.build()).execute().use { response ->
        if (!response.isSuccessful && response.code != 206) {
          return@use false
        }

        val contentType = response.header("Content-Type")?.lowercase().orEmpty()
        if ("text/html" in contentType) {
          return@use false
        }

        val source = response.body?.source() ?: return@use true
        source.request(1024)
        val preview = source.buffer.clone().readUtf8(minOf(source.buffer.size, 1024L)).trimStart()
        val lowerPreview = preview.lowercase()
        if (lowerPreview.startsWith("<!doctype html") || lowerPreview.startsWith("<html")) {
          return@use false
        }

        val isHls = isHlsUrl || "mpegurl" in contentType
        if (isHls && preview.isNotEmpty() && "#EXTM3U" !in preview.uppercase()) {
          return@use false
        }

        true
      }
    }.getOrDefault(false)
  }
}
