package com.epalma.tvespanolplus;

import static org.junit.Assert.*;
import java.util.List;
import org.junit.Test;

public class EpgPolicyTest {
  @Test public void brokenDefaultGuideGetsFallbacks(){
    List<String> x=EpgPolicy.candidates("https://dearbulut.github.io/iptv/epg/guide.xml.gz");
    assertTrue(x.contains("https://i.mjh.nz/SamsungTVPlus/all.xml.gz"));
    assertTrue(x.contains("https://i.mjh.nz/PlutoTV/all.xml.gz"));
    assertTrue(x.contains("https://i.mjh.nz/Plex/all.xml.gz"));
    assertTrue(x.size()>=7);
  }

  @Test public void customGuideDoesNotMixPublicFallbacks(){
    List<String> x=EpgPolicy.candidates("https://example.com/custom.xml");
    assertTrue(x.contains("https://example.com/custom.xml"));
    assertTrue(x.contains("https://example.com/custom.xml.gz"));
    assertFalse(x.contains("https://i.mjh.nz/PlutoTV/all.xml.gz"));
  }
}
