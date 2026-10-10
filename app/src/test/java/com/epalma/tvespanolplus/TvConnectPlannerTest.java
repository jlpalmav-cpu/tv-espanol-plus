package com.epalma.tvespanolplus;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class TvConnectPlannerTest {
  @Test public void ranksBestDirectMethodFirst(){
    List<String> order=TvConnectPlanner.order(true,96,true,70,true,"");
    assertEquals("CAST",order.get(0));
    assertEquals("DLNA",order.get(1));
    assertEquals("WIRELESS_DISPLAY",order.get(2));
  }

  @Test public void remembersLastSuccessfulMethod(){
    List<String> order=TvConnectPlanner.order(true,96,true,70,true,"DLNA");
    assertEquals("DLNA",order.get(0));
    assertEquals("CAST",order.get(1));
  }

  @Test public void unavailablePreferredMethodIsIgnored(){
    List<String> order=TvConnectPlanner.order(false,0,true,78,false,"CAST");
    assertEquals(1,order.size());
    assertEquals("DLNA",order.get(0));
  }

  @Test public void wirelessIsAlwaysFallback(){
    List<String> order=TvConnectPlanner.order(false,0,false,0,true,"");
    assertEquals(1,order.size());
    assertEquals("WIRELESS_DISPLAY",order.get(0));
  }
}
