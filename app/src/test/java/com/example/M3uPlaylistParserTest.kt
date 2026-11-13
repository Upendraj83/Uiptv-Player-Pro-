package com.example

import com.example.data.repository.M3uPlaylistParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uPlaylistParserTest {

  @Test
  fun parseGroupsStreamsAndExtractsMetadataForSouthAsiaPlaylists() {
    val sampleM3u = """
      #EXTM3U
      #EXTINF:-1 tvg-id="DDNews.in@SD" tvg-logo="https://example.org/ddnews.png" group-title="News",DD News (576p)
      https://stream.example.org/ddnews/576p.m3u8
      #EXTINF:-1 tvg-id="DDNews.in@HD" tvg-logo="https://example.org/ddnews.png" group-title="News",DD News (1080p)
      #EXTVLCOPT:http-user-agent=TiweeAgent/1.0
      #EXTVLCOPT:http-referrer=https://example.org/
      https://stream.example.org/ddnews/1080p.m3u8
      #EXTINF:-1 tvg-id="PTVNews.pk" group-title="News;Public",PTV News (720p) [Geo-blocked]
      https://stream.example.org/ptvnews.m3u8
    """.trimIndent()

    val indiaChannels = M3uPlaylistParser.parse(sampleM3u, "IN", "India")
    assertEquals(2, indiaChannels.size)

    val ddNews = indiaChannels.first { it.id == "DDNews.in" }
    assertEquals("DD News", ddNews.name)
    assertEquals("IN", ddNews.country)
    assertEquals(listOf("news"), ddNews.categories)
    assertEquals("https://example.org/ddnews.png", ddNews.logo)
    assertEquals(2, ddNews.streams.size)
    // Higher quality 1080p should be sorted first
    assertEquals("1080p", ddNews.streams.first().quality)
    assertEquals("TiweeAgent/1.0", ddNews.streams.first().userAgent)
    assertEquals("https://example.org/", ddNews.streams.first().referrer)

    val ptv = indiaChannels.first { it.id == "PTVNews.pk" }
    assertEquals("PTV News", ptv.name)
    assertEquals(listOf("news", "public"), ptv.categories)
    assertTrue(ptv.description?.contains("India") == true)
  }
}
