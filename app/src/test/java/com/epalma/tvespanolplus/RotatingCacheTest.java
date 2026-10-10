package com.epalma.tvespanolplus;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.File;
import java.nio.charset.StandardCharsets;

public class RotatingCacheTest {
  @Test public void keepsThreeGenerations() throws Exception {
    File dir = new File(System.getProperty("java.io.tmpdir"),"pv-cache-"+System.nanoTime());
    RotatingCache.write(dir,"x","one".getBytes(StandardCharsets.UTF_8),3);
    RotatingCache.write(dir,"x","two".getBytes(StandardCharsets.UTF_8),3);
    RotatingCache.write(dir,"x","three".getBytes(StandardCharsets.UTF_8),3);
    assertEquals("three",new String(RotatingCache.read(dir,"x",0,100),StandardCharsets.UTF_8));
    assertEquals("two",new String(RotatingCache.read(dir,"x",1,100),StandardCharsets.UTF_8));
    assertEquals("one",new String(RotatingCache.read(dir,"x",2,100),StandardCharsets.UTF_8));
  }
}
