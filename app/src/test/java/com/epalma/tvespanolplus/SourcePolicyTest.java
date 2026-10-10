package com.epalma.tvespanolplus;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;

public class SourcePolicyTest {
  @Test public void palmaVisionIsProtected(){
    assertTrue(SourcePolicy.isProtected(SourcePolicy.PALMAVISION_ID));
    assertFalse(SourcePolicy.canEdit(SourcePolicy.PALMAVISION_ID));
    assertFalse(SourcePolicy.canDelete(SourcePolicy.PALMAVISION_ID));
  }
  @Test public void oneActiveSourceAndSafeFallback(){
    Set<String> ids=new HashSet<>(Arrays.asList("palmas","casa"));
    assertEquals("palmas",SourcePolicy.selectActive(ids,"palmas"));
    assertEquals(SourcePolicy.PALMAVISION_ID,SourcePolicy.selectActive(ids,"missing"));
    assertEquals(SourcePolicy.PALMAVISION_ID,SourcePolicy.selectActive(ids,SourcePolicy.PALMAVISION_ID));
  }
  @Test public void classifiesExternalContent(){
    assertEquals(SourcePolicy.MOVIE,SourcePolicy.classify(false,"Avatar","Peliculas HD","http://x/a.mp4"));
    assertEquals(SourcePolicy.SERIES,SourcePolicy.classify(false,"Show S02E05","Series","http://x/5.ts"));
    assertEquals(SourcePolicy.MUSIC,SourcePolicy.classify(false,"Rock FM","Musica","http://x/live.mp3"));
    assertEquals(SourcePolicy.LIVE,SourcePolicy.classify(false,"Canal 5","Honduras","http://x/live.m3u8"));
    assertEquals(SourcePolicy.MOVIE,SourcePolicy.classify(false,"Avatar","","http://x/movie/u/p/123.ts"));
    assertEquals(SourcePolicy.SERIES,SourcePolicy.classify(false,"Episode 1","","http://x/series/u/p/456.ts"));
  }
  @Test public void systemListNeverGetsReclassified(){
    assertEquals(SourcePolicy.LIVE,SourcePolicy.classify(true,"Movie S01E01","Peliculas","http://x/test.mp4"));
  }
}
