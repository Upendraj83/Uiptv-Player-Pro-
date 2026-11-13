package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.CatalogData
import com.example.data.model.Category
import com.example.data.model.Channel
import com.example.data.model.Country
import com.example.data.model.StreamSource
import com.example.data.remote.IptvCatalogApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

class CatalogRepository(
  private val context: Context,
) {
  private val cacheFile = File(context.applicationContext.cacheDir, CACHE_FILE_NAME)
  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  private val httpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(20, TimeUnit.SECONDS)
      .readTimeout(45, TimeUnit.SECONDS)
      .writeTimeout(20, TimeUnit.SECONDS)
      .build()
  }

  private val api: IptvCatalogApi by lazy {
    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    Retrofit.Builder()
      .baseUrl(API_BASE_URL)
      .client(httpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(IptvCatalogApi::class.java)
  }

  suspend fun fetchAndAssembleCatalog(
    forceRefresh: Boolean = false,
    onProgress: (String) -> Unit = {},
  ): CatalogData =
    withContext(Dispatchers.IO) {
      if (!forceRefresh) {
        readCacheIfValid()?.let { cached ->
          return@withContext cached
        }
      }

      onProgress("Fetching channels, categories & streams...")

      val (categoriesRes, countriesRes, streamsRes, logosRes, channelsRes, regionalChannels) =
        coroutineScope {
          val categoriesDef = async { runCatching { api.getCategories() }.getOrDefault(emptyList()) }
          val countriesDef = async { runCatching { api.getCountries() }.getOrDefault(emptyList()) }
          val streamsDef = async { runCatching { api.getStreams() }.getOrDefault(emptyList()) }
          val logosDef = async { runCatching { api.getLogos() }.getOrDefault(emptyList()) }
          val channelsDef = async { runCatching { api.getChannels() }.getOrDefault(emptyList()) }
          val regionalDef = async { fetchFeaturedCountryPlaylists() }
          CatalogResponses(
            categoriesDef.await(),
            countriesDef.await(),
            streamsDef.await(),
            logosDef.await(),
            channelsDef.await(),
            regionalDef.await(),
          )
        }

      onProgress("Assembling India, Pakistan, Bangladesh & global streams...")

      // Group and sort playable streams by quality rank
      val streamsByChannel = HashMap<String, MutableList<StreamSource>>(streamsRes.size)
      for (item in streamsRes) {
        val channelId = item.channel?.takeIf { it.isNotBlank() } ?: continue
        val url = item.url?.takeIf { it.isNotBlank() } ?: continue
        val streamTitle = item.title.orEmpty()
        if (!StreamValidator.isPlayableStreamUrl(url, streamTitle)) continue
        val stream =
          StreamSource(
            url = url,
            title = streamTitle,
            quality = item.quality?.takeIf { it.isNotBlank() },
            userAgent = item.userAgent?.takeIf { it.isNotBlank() },
            referrer = item.httpReferrer?.takeIf { it.isNotBlank() },
          )
        val channelStreams = streamsByChannel.getOrPut(channelId) { mutableListOf() }
        if (channelStreams.none { it.url == url }) {
          channelStreams.add(stream)
        }
      }

      for (streams in streamsByChannel.values) {
        if (streams.size > 1) {
          streams.sortByDescending { getQualityRank(it.quality) }
        }
      }

      // Pick best logo per channel
      val logosByChannel = HashMap<String, String>(logosRes.size)
      val logoScores = HashMap<String, Int>(logosRes.size)
      for (logo in logosRes) {
        val channelId = logo.channel?.takeIf { it.isNotBlank() } ?: continue
        val url = logo.url?.takeIf { it.isNotBlank() } ?: continue
        val inUse = logo.inUse != false
        val hasNoFeed = logo.feed == null
        val score = (if (inUse) 2 else 0) + (if (hasNoFeed) 1 else 0)
        val existingScore = logoScores[channelId] ?: -1
        if (score > existingScore) {
          logoScores[channelId] = score
          logosByChannel[channelId] = url
        }
      }

      // Assemble playable channels from JSON API
      val playableById = LinkedHashMap<String, Channel>(channelsRes.size + regionalChannels.size)
      for (raw in channelsRes) {
        val id = raw.id?.takeIf { it.isNotBlank() } ?: continue
        if (!raw.closed.isNullOrBlank() || !raw.replacedBy.isNullOrBlank()) continue

        val streams = streamsByChannel[id] ?: continue
        if (streams.isEmpty()) continue

        val computedDescription =
          if (!raw.description.isNullOrBlank()) {
            raw.description
          } else {
            val parts = mutableListOf<String>()
            if (!raw.network.isNullOrBlank()) parts.add("${raw.network} network")
            if (!raw.categories.isNullOrEmpty()) {
              parts.add(
                raw.categories.joinToString(", ") { cat ->
                  cat.replaceFirstChar { ch -> ch.uppercaseChar() }
                }
              )
            }
            if (!raw.country.isNullOrBlank()) parts.add("Live from ${raw.country}")
            if (!raw.broadcastArea.isNullOrEmpty()) {
              parts.add("Broadcast: ${raw.broadcastArea.joinToString(", ")}")
            }
            if (parts.isNotEmpty()) parts.joinToString(" • ") else raw.website
          }

        playableById[id] =
          Channel(
            id = id,
            name = raw.name?.takeIf { it.isNotBlank() } ?: id,
            altNames = raw.altNames.orEmpty(),
            country = raw.country.orEmpty(),
            categories = raw.categories.orEmpty(),
            isNsfw = raw.isNsfw == true,
            logo = logosByChannel[id],
            streams = streams,
            description = computedDescription,
          )
      }

      // Merge regional India (IN), Pakistan (PK), and Bangladesh (BD) channels from live country M3Us
      for (regional in regionalChannels) {
        val existing = playableById[regional.id]
        if (existing == null) {
          playableById[regional.id] =
            if (regional.logo.isNullOrBlank() && logosByChannel[regional.id] != null) {
              regional.copy(logo = logosByChannel[regional.id])
            } else {
              regional
            }
        } else {
          val existingUrls = existing.streams.mapTo(HashSet()) { it.url }
          val extraStreams = regional.streams.filter { it.url !in existingUrls }
          val mergedLogo = existing.logo?.takeIf { it.isNotBlank() } ?: regional.logo
          val mergedCategories =
            if (existing.categories.isNotEmpty()) existing.categories else regional.categories
          if (extraStreams.isNotEmpty() || mergedLogo != existing.logo || mergedCategories != existing.categories) {
            playableById[regional.id] = existing.copy(
              logo = mergedLogo,
              categories = mergedCategories,
              streams = (existing.streams + extraStreams).sortedByDescending { getQualityRank(it.quality) },
            )
          }
        }
      }

      val playableChannels = playableById.values.toList()
      if (playableChannels.isEmpty()) {
        throw IllegalStateException("Unable to fetch live TV channels. Check your connection and retry.")
      }

      val categoriesById = LinkedHashMap<String, Category>()
      for (c in categoriesRes) {
        val id = c.id?.takeIf { it.isNotBlank() } ?: continue
        categoriesById[id.lowercase()] = Category(id = id, name = c.name?.takeIf { it.isNotBlank() } ?: id)
      }
      for (ch in playableChannels) {
        for (cat in ch.categories) {
          val key = cat.lowercase()
          if (key.isNotBlank() && key !in categoriesById) {
            categoriesById[key] = Category(id = key, name = key.replaceFirstChar { it.uppercaseChar() })
          }
        }
      }

      val countriesByCode = LinkedHashMap<String, Country>()
      for (c in countriesRes) {
        val code = c.code?.takeIf { it.isNotBlank() }?.uppercase() ?: continue
        countriesByCode[code] =
          Country(
            code = code,
            name = c.name?.takeIf { it.isNotBlank() } ?: code,
            flag = c.flag?.takeIf { it.isNotBlank() } ?: flagFromCode(code),
            languages = c.languages.orEmpty(),
          )
      }

      // Ensure India, Pakistan, and Bangladesh are always present in the country catalog
      for (featured in FEATURED_SOUTH_ASIA_COUNTRIES) {
        val existing = countriesByCode[featured.code]
        countriesByCode[featured.code] =
          if (existing == null) {
            featured
          } else {
            existing.copy(
              name = existing.name.ifBlank { featured.name },
              flag = existing.flag.ifBlank { featured.flag },
              languages = existing.languages.ifEmpty { featured.languages },
            )
          }
      }

      val result =
        CatalogData(
          timestampMs = System.currentTimeMillis(),
          channels = playableChannels,
          categories = categoriesById.values.toList(),
          countries = countriesByCode.values.toList(),
        )

      writeCache(result)
      result
    }

  private suspend fun fetchFeaturedCountryPlaylists(): List<Channel> =
    coroutineScope {
      FEATURED_SOUTH_ASIA_COUNTRIES.map { country ->
        async {
          runCatching {
            val url = "$COUNTRY_M3U_BASE_URL${country.code.lowercase()}.m3u"
            val request = okhttp3.Request.Builder().url(url).build()
            httpClient.newCall(request).execute().use { response ->
              if (!response.isSuccessful) return@use emptyList()
              val body = response.body?.string().orEmpty()
              M3uPlaylistParser.parse(
                content = body,
                countryCode = country.code,
                countryName = country.name,
              )
            }
          }.onFailure { err ->
            Log.w(TAG, "Failed to fetch ${country.name} M3U playlist: ${err.message}")
          }.getOrDefault(emptyList())
        }
      }.flatMap { it.await() }
    }

  private fun readCacheIfValid(): CatalogData? {
    return try {
      if (!cacheFile.exists()) return null
      val text = cacheFile.readText()
      val cached = json.decodeFromString<CatalogData>(text)
      val age = System.currentTimeMillis() - cached.timestampMs
      if (cached.channels.isNotEmpty() && age in 0 until CACHE_TTL_MS) {
        cached
      } else {
        null
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to read catalog cache: ${e.message}")
      null
    }
  }

  private fun writeCache(data: CatalogData) {
    try {
      cacheFile.writeText(json.encodeToString(CatalogData.serializer(), data))
    } catch (e: Exception) {
      Log.w(TAG, "Failed to write catalog cache: ${e.message}")
    }
  }

  private data class CatalogResponses(
    val categories: List<com.example.data.remote.ApiCategoryDto>,
    val countries: List<com.example.data.remote.ApiCountryDto>,
    val streams: List<com.example.data.remote.ApiStreamDto>,
    val logos: List<com.example.data.remote.ApiLogoDto>,
    val channels: List<com.example.data.remote.ApiChannelDto>,
    val regionalChannels: List<Channel>,
  )

  companion object {
    private const val TAG = "CatalogRepository"
    private const val API_BASE_URL = "https://iptv-org.github.io/api/"
    private const val COUNTRY_M3U_BASE_URL = "https://iptv-org.github.io/iptv/countries/"
    private const val CACHE_FILE_NAME = "tiwee_catalog_cache_v3.json"
    private const val CACHE_TTL_MS = 1000L * 60L * 60L * 6L // 6 hours

    val FEATURED_SOUTH_ASIA_CODES = listOf("IN", "PK", "BD")

    val FEATURED_SOUTH_ASIA_COUNTRIES = listOf(
      Country(code = "IN", name = "India", flag = "🇮🇳", languages = listOf("hin", "eng", "tam", "tel")),
      Country(code = "PK", name = "Pakistan", flag = "🇵🇰", languages = listOf("urd", "eng", "pan")),
      Country(code = "BD", name = "Bangladesh", flag = "🇧🇩", languages = listOf("ben", "eng")),
    )

    fun getQualityRank(quality: String?): Int {
      if (quality.isNullOrBlank()) return 1
      val q = quality.lowercase()
      return when {
        "4k" in q || "2160" in q -> 6
        "1080" in q || "fhd" in q -> 5
        "720" in q || "hd" in q -> 4
        "576" in q || "480" in q || "sd" in q -> 3
        "360" in q || "240" in q -> 2
        else -> 1
      }
    }

    fun flagFromCode(code: String): String {
      if (code.length != 2) return "🌐"
      return try {
        val upper = code.uppercase()
        val first = Character.toChars(0x1F1E6 + (upper[0] - 'A'))
        val second = Character.toChars(0x1F1E6 + (upper[1] - 'A'))
        String(first) + String(second)
      } catch (_: Exception) {
        "🌐"
      }
    }
  }
}
