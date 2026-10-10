package com.epalma.tvespanolplus;

import org.junit.Test;
import static org.junit.Assert.*;

public class CachePolicyTest {
  @Test public void rejectsCatastrophicPlaylistShrink(){
    assertTrue(CachePolicy.acceptableCount(1700,1650));
    assertTrue(CachePolicy.acceptableCount(1700,700));
    assertFalse(CachePolicy.acceptableCount(1700,70));
  }
  @Test public void resumePolicyAvoidsBeginningAndCredits(){
    assertFalse(CachePolicy.shouldResume(30000,7200000));
    assertTrue(CachePolicy.shouldResume(3600000,7200000));
    assertFalse(CachePolicy.shouldResume(7150000,7200000));
  }
  @Test public void dpadSeekAcceleratesOnHold(){
    assertEquals(10000L,CachePolicy.seekDeltaForRepeat(0));
    assertEquals(30000L,CachePolicy.seekDeltaForRepeat(4));
    assertEquals(60000L,CachePolicy.seekDeltaForRepeat(10));
  }
  @Test public void radioClassificationExcludesTalkAndGroupsMusic(){
    assertEquals("Salsa",CachePolicy.radioGenre("Spanish","salsa,latin,music","Radio Salsa"));
    assertEquals("Classic Rock",CachePolicy.radioGenre("English","classic rock,music","Rock FM"));
    assertNull(CachePolicy.radioGenre("Spanish","news,talk","Noticias 24"));
    assertNull(CachePolicy.radioGenre("French","pop,music","Paris Pop"));
  }
}
