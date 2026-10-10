package com.epalma.tvespanolplus;

import static org.junit.Assert.*;
import org.junit.Test;

public class RadioStreamPolicyTest {
  @Test public void detectsLegacyPlaylistsButNotHls(){
    assertTrue(RadioStreamPolicy.isLegacyPlaylistUrl("https://x.test/live.pls"));
    assertTrue(RadioStreamPolicy.isLegacyPlaylistUrl("https://x.test/live.m3u?x=1"));
    assertFalse(RadioStreamPolicy.isLegacyPlaylistUrl("https://x.test/live.m3u8"));
  }

  @Test public void resolvesPlsAndM3uEntries(){
    assertEquals("https://stream.test/live.mp3",
      RadioStreamPolicy.firstPlayable("[playlist]\nFile1=https://stream.test/live.mp3\n","https://x.test/list.pls"));
    assertEquals("https://x.test/radio/audio/live.aac",
      RadioStreamPolicy.firstPlayable("#EXTM3U\naudio/live.aac\n","https://x.test/radio/list.m3u"));
  }

  @Test public void classifiesCommonStreamKinds(){
    assertEquals("hls",RadioStreamPolicy.kind("https://x.test/live.m3u8"));
    assertEquals("mp3",RadioStreamPolicy.kind("https://x.test/live.mp3"));
    assertEquals("aac",RadioStreamPolicy.kind("https://x.test/live.aac"));
    assertEquals("auto",RadioStreamPolicy.kind("https://x.test/stream"));
  }
}
