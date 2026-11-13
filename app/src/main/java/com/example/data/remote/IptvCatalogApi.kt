package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET

@JsonClass(generateAdapter = true)
data class ApiChannelDto(
  val id: String? = null,
  val name: String? = null,
  @Json(name = "alt_names") val altNames: List<String>? = null,
  val country: String? = null,
  val categories: List<String>? = null,
  @Json(name = "is_nsfw") val isNsfw: Boolean? = null,
  val closed: String? = null,
  @Json(name = "replaced_by") val replacedBy: String? = null,
  val network: String? = null,
  val website: String? = null,
  val description: String? = null,
  @Json(name = "broadcast_area") val broadcastArea: List<String>? = null,
)

@JsonClass(generateAdapter = true)
data class ApiStreamDto(
  val channel: String? = null,
  val url: String? = null,
  val title: String? = null,
  val quality: String? = null,
  @Json(name = "user_agent") val userAgent: String? = null,
  @Json(name = "http_referrer") val httpReferrer: String? = null,
)

@JsonClass(generateAdapter = true)
data class ApiLogoDto(
  val channel: String? = null,
  val url: String? = null,
  @Json(name = "in_use") val inUse: Boolean? = null,
  val feed: String? = null,
)

@JsonClass(generateAdapter = true)
data class ApiCategoryDto(
  val id: String? = null,
  val name: String? = null,
)

@JsonClass(generateAdapter = true)
data class ApiCountryDto(
  val name: String? = null,
  val code: String? = null,
  val flag: String? = null,
  val languages: List<String>? = null,
)

interface IptvCatalogApi {
  @GET("channels.json")
  suspend fun getChannels(): List<ApiChannelDto>

  @GET("streams.json")
  suspend fun getStreams(): List<ApiStreamDto>

  @GET("logos.json")
  suspend fun getLogos(): List<ApiLogoDto>

  @GET("categories.json")
  suspend fun getCategories(): List<ApiCategoryDto>

  @GET("countries.json")
  suspend fun getCountries(): List<ApiCountryDto>
}
