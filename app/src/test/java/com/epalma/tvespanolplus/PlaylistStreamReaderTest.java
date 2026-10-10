package com.epalma.tvespanolplus;

import static org.junit.Assert.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.Test;

public class PlaylistStreamReaderTest {
  @Test public void smallPlaylistCanBeCached() throws Exception {
    byte[] data="#EXTM3U\n#EXTINF:-1,Canal\nhttps://example.test/live.m3u8\n".getBytes("UTF-8");
    PlaylistStreamReader.CappedCacheInputStream in=
      new PlaylistStreamReader.CappedCacheInputStream(new ByteArrayInputStream(data),1024,4096);
    byte[] buf=new byte[64]; while(in.read(buf)!=-1){}
    assertTrue(in.cacheable());
    assertArrayEquals(data,in.cachedBytes());
    assertEquals(data.length,in.totalBytes());
  }

  @Test public void largePlaylistKeepsStreamingButStopsCaching() throws Exception {
    byte[] data=new byte[40];
    PlaylistStreamReader.CappedCacheInputStream in=
      new PlaylistStreamReader.CappedCacheInputStream(new ByteArrayInputStream(data),16,128);
    byte[] buf=new byte[11]; int total=0,n; while((n=in.read(buf))!=-1)total+=n;
    assertEquals(40,total);
    assertFalse(in.cacheable());
    assertNull(in.cachedBytes());
  }

  @Test public void hardSafetyLimitStillApplies() throws Exception {
    byte[] data=new byte[40];
    PlaylistStreamReader.CappedCacheInputStream in=
      new PlaylistStreamReader.CappedCacheInputStream(new ByteArrayInputStream(data),16,24);
    byte[] buf=new byte[8];
    try {
      while(in.read(buf)!=-1){}
      fail("Expected safety limit");
    } catch(IOException expected) {
      assertTrue(expected.getMessage().contains("límite de seguridad"));
    }
  }
}
