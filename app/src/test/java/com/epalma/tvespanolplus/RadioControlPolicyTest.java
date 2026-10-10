package com.epalma.tvespanolplus;

import static org.junit.Assert.*;
import org.junit.Test;

public class RadioControlPolicyTest {
  @Test public void previousAndNextWrap(){
    assertEquals(2,RadioControlPolicy.adjacentIndex(0,3,-1));
    assertEquals(0,RadioControlPolicy.adjacentIndex(2,3,1));
    assertEquals(1,RadioControlPolicy.adjacentIndex(0,3,1));
  }

  @Test public void missingCurrentStartsSafely(){
    assertEquals(0,RadioControlPolicy.adjacentIndex(-1,3,0));
    assertEquals(-1,RadioControlPolicy.adjacentIndex(-1,0,1));
  }

  @Test public void sourceRetryCycles(){
    assertEquals(1,RadioControlPolicy.nextSourceIndex(0,3));
    assertEquals(0,RadioControlPolicy.nextSourceIndex(2,3));
    assertEquals(0,RadioControlPolicy.nextSourceIndex(0,1));
  }
}
