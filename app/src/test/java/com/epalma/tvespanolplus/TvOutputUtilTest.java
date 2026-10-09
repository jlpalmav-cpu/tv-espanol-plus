package com.epalma.tvespanolplus;

import org.junit.Test;
import static org.junit.Assert.*;

public class TvOutputUtilTest {
  @Test public void castWinsWithInternetAndFriendlyStream(){
    int cast=TvOutputUtil.score("CAST",true,true,true,true);
    int dlna=TvOutputUtil.score("DLNA",true,true,true,true);
    int mira=TvOutputUtil.score("MIRACAST",true,true,true,false);
    assertTrue(cast>dlna);
    assertTrue(dlna>mira);
  }

  @Test public void miracastWinsWithoutInternet(){
    int cast=TvOutputUtil.score("CAST",false,true,true,true);
    int dlna=TvOutputUtil.score("DLNA",false,true,true,true);
    int mira=TvOutputUtil.score("MIRACAST",false,true,true,false);
    assertTrue(mira>dlna);
    assertTrue(dlna>cast);
  }

  @Test public void miracastGetsBoostForDifficultStream(){
    int cast=TvOutputUtil.score("CAST",true,true,false,true);
    int mira=TvOutputUtil.score("MIRACAST",true,true,false,false);
    assertTrue(mira>cast);
  }

  @Test public void parsesSsdpAndDlnaDescription(){
    String ssdp="HTTP/1.1 200 OK\r\nLOCATION: http://192.168.1.20:1400/xml/device.xml\r\nUSN: uuid:test\r\n\r\n";
    assertEquals("http://192.168.1.20:1400/xml/device.xml",TvOutputUtil.header(ssdp,"LOCATION"));
    String xml="<root><device><friendlyName>TV Sala</friendlyName><serviceList><service><serviceType>urn:schemas-upnp-org:service:AVTransport:1</serviceType><controlURL>/MediaRenderer/AVTransport/Control</controlURL></service></serviceList></device></root>";
    assertEquals("TV Sala",TvOutputUtil.friendlyName(xml));
    assertEquals("/MediaRenderer/AVTransport/Control",TvOutputUtil.avTransportControlUrl(xml));
    assertEquals("http://192.168.1.20:1400/MediaRenderer/AVTransport/Control",TvOutputUtil.resolveControlUrl("http://192.168.1.20:1400/xml/device.xml","/MediaRenderer/AVTransport/Control"));
  }
}
