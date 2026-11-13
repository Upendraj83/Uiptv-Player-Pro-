package com.example

import com.example.data.repository.StreamValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamValidatorTest {

  @Test
  fun acceptsValidPublicHttpAndHlsStreamUrls() {
    assertTrue(StreamValidator.isPlayableStreamUrl("https://cdn.example.com/live/channel.m3u8"))
    assertTrue(StreamValidator.isPlayableStreamUrl("http://stream.example.org:8080/live/index.m3u8?token=abc"))
    assertTrue(StreamValidator.isPlayableStreamUrl("https://media.example.com/feed.ts", "Live News HD"))
  }

  @Test
  fun rejectsUnplayableProtocolsLocalHostsDashAndOfflineMarkers() {
    assertFalse(StreamValidator.isPlayableStreamUrl("rtmp://live.example.com/app/stream"))
    assertFalse(StreamValidator.isPlayableStreamUrl("rtsp://cam.example.com/live"))
    assertFalse(StreamValidator.isPlayableStreamUrl("udp://239.0.0.1:1234"))
    assertFalse(StreamValidator.isPlayableStreamUrl("http://localhost:8080/live.m3u8"))
    assertFalse(StreamValidator.isPlayableStreamUrl("http://192.168.1.10/live.m3u8"))
    assertFalse(StreamValidator.isPlayableStreamUrl("https://cdn.example.com/live/manifest.mpd"))
    assertFalse(StreamValidator.isPlayableStreamUrl("https://cdn.example.com/channel.ism/Manifest"))
    assertFalse(StreamValidator.isPlayableStreamUrl("https://www.youtube.com/watch?v=12345"))
    assertFalse(StreamValidator.isPlayableStreamUrl("https://cdn.example.com/live.m3u8", "News TV [Offline]"))
    assertFalse(StreamValidator.isPlayableStreamUrl(""))
  }
}
