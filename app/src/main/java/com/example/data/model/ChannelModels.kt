package com.example.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class StreamSource(
  val url: String,
  val title: String = "",
  val quality: String? = null,
  val userAgent: String? = null,
  val referrer: String? = null,
)

@Immutable
@Serializable
data class Channel(
  val id: String,
  val name: String,
  val altNames: List<String> = emptyList(),
  val country: String = "",
  val categories: List<String> = emptyList(),
  val isNsfw: Boolean = false,
  val logo: String? = null,
  val streams: List<StreamSource> = emptyList(),
  val description: String? = null,
)

@Immutable
@Serializable
data class Category(
  val id: String,
  val name: String,
)

@Immutable
@Serializable
data class Country(
  val code: String,
  val name: String,
  val flag: String = "🌐",
  val languages: List<String> = emptyList(),
)

@Immutable
@Serializable
data class CatalogData(
  val timestampMs: Long = 0L,
  val channels: List<Channel> = emptyList(),
  val categories: List<Category> = emptyList(),
  val countries: List<Country> = emptyList(),
)

enum class ThemePalette(val value: String) {
  DARK("dark"),
  AMOLED("amoled");

  companion object {
    fun fromValue(value: String?): ThemePalette =
      if (value == AMOLED.value) AMOLED else DARK
  }
}

@Immutable
data class ParentalSettings(
  val allowAdultChannels: Boolean = false,
  val pin: String? = null,
)
