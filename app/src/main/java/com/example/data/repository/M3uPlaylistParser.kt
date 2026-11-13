package com.example.data.repository

import com.example.data.model.Channel
import com.example.data.model.StreamSource

object M3uPlaylistParser {

  private val ATTR_REGEX = Regex("""([a-zA-Z0-9_-]+)="([^"]*)"""")
  private val QUALITY_REGEX = Regex("""\((\d{3,4}p|4K|HD|FHD|SD)\)""", RegexOption.IGNORE_CASE)
  private val BRACKET_TAG_REGEX = Regex("""\s*\[[^\]]*]""")

  fun parse(
    content: String,
    countryCode: String,
    countryName: String,
  ): List<Channel> {
    if (content.isBlank()) return emptyList()

    val normalizedCode = countryCode.uppercase()
    val channelsById = LinkedHashMap<String, MutableChannelAccumulator>()

    var pendingName = ""
    var pendingTvgId = ""
    var pendingLogo: String? = null
    var pendingCategories: List<String> = emptyList()
    var pendingUserAgent: String? = null
    var pendingReferrer: String? = null
    var hasExtInf = false

    val lines = content.lineSequence()
    for (rawLine in lines) {
      val line = rawLine.trim()
      if (line.isEmpty()) continue

      when {
        line.startsWith("#EXTINF", ignoreCase = true) -> {
          hasExtInf = true
          pendingUserAgent = null
          pendingReferrer = null

          val commaIdx = line.indexOf(',')
          val header = if (commaIdx >= 0) line.substring(0, commaIdx) else line
          pendingName = if (commaIdx >= 0) line.substring(commaIdx + 1).trim() else ""

          val attrs = HashMap<String, String>()
          for (match in ATTR_REGEX.findAll(header)) {
            attrs[match.groupValues[1].lowercase()] = match.groupValues[2].trim()
          }

          pendingTvgId = attrs["tvg-id"].orEmpty()
          pendingLogo = attrs["tvg-logo"]?.takeIf { it.startsWith("http", ignoreCase = true) }
          pendingCategories =
            attrs["group-title"]
              ?.split(';', ',')
              ?.map { it.trim().lowercase() }
              ?.filter { it.isNotBlank() && it != "undefined" }
              ?.distinct()
              .orEmpty()
        }

        line.startsWith("#EXTVLCOPT:", ignoreCase = true) -> {
          val opt = line.substringAfter(':', "").trim()
          when {
            opt.startsWith("http-user-agent=", ignoreCase = true) ->
              pendingUserAgent = opt.substringAfter('=').trim().takeIf { it.isNotBlank() }
            opt.startsWith("http-referrer=", ignoreCase = true) ->
              pendingReferrer = opt.substringAfter('=').trim().takeIf { it.isNotBlank() }
          }
        }

        line.startsWith("#") -> {
          // Ignore other M3U directives/comments
        }

        hasExtInf && (line.startsWith("http://", ignoreCase = true) || line.startsWith("https://", ignoreCase = true)) -> {
          val quality = QUALITY_REGEX.find(pendingName)?.groupValues?.getOrNull(1)
          if (!StreamValidator.isPlayableStreamUrl(line, pendingName)) {
            hasExtInf = false
            pendingUserAgent = null
            pendingReferrer = null
            continue
          }
          val cleanName =
            pendingName
              .replace(BRACKET_TAG_REGEX, "")
              .replace(QUALITY_REGEX, "")
              .trim()
              .ifBlank { pendingName.ifBlank { "Live TV ($normalizedCode)" } }

          val baseId =
            pendingTvgId
              .substringBefore('@')
              .trim()
              .ifBlank {
                val slug = cleanName.lowercase().replace(Regex("[^a-z0-9]+"), "")
                "${slug.ifBlank { "ch" }}.${normalizedCode.lowercase()}"
              }

          val acc =
            channelsById.getOrPut(baseId) {
              MutableChannelAccumulator(
                id = baseId,
                name = cleanName,
                country = normalizedCode,
                categories = pendingCategories,
                logo = pendingLogo,
                description = buildString {
                  if (pendingCategories.isNotEmpty()) {
                    append(
                      pendingCategories.joinToString(", ") { cat ->
                        cat.replaceFirstChar { it.uppercaseChar() }
                      }
                    )
                    append(" • ")
                  }
                  append("Live from $countryName")
                },
              )
            }

          if (acc.logo.isNullOrBlank() && !pendingLogo.isNullOrBlank()) {
            acc.logo = pendingLogo
          }
          if (acc.categories.isEmpty() && pendingCategories.isNotEmpty()) {
            acc.categories = pendingCategories
          }

          if (acc.streams.none { it.url == line }) {
            acc.streams.add(
              StreamSource(
                url = line,
                title = quality?.let { "$cleanName ($it)" } ?: cleanName,
                quality = quality,
                userAgent = pendingUserAgent,
                referrer = pendingReferrer,
              )
            )
          }

          hasExtInf = false
          pendingUserAgent = null
          pendingReferrer = null
        }

        else -> {
          hasExtInf = false
        }
      }
    }

    return channelsById.values.map { acc ->
      acc.streams.sortByDescending { CatalogRepository.getQualityRank(it.quality) }
      Channel(
        id = acc.id,
        name = acc.name,
        country = acc.country,
        categories = acc.categories,
        isNsfw = false,
        logo = acc.logo,
        streams = acc.streams.toList(),
        description = acc.description,
      )
    }
  }

  private class MutableChannelAccumulator(
    val id: String,
    val name: String,
    val country: String,
    var categories: List<String>,
    var logo: String?,
    val description: String,
    val streams: MutableList<StreamSource> = mutableListOf(),
  )
}
