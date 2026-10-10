package com.epalma.tvespanolplus;

import static org.junit.Assert.*;
import org.junit.Test;

public class SecuritySanitizerTest {
  @Test public void hidesQueryCredentials(){
    String x=SecuritySanitizer.redact("https://host/xmltv.php?username=leo&password=secret&type=m3u");
    assertFalse(x.contains("leo"));
    assertFalse(x.contains("secret"));
    assertTrue(x.contains("username=••••"));
    assertTrue(x.contains("password=••••"));
  }

  @Test public void hidesUserInfoCredentials(){
    String x=SecuritySanitizer.redact("https://user:pass@example.com/live");
    assertFalse(x.contains("user:pass"));
    assertTrue(x.contains("••••:••••@"));
  }
}
