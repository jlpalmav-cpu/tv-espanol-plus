package com.epalma.tvespanolplus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class TvConnectPlanner {
  private TvConnectPlanner(){}

  public static List<String> order(boolean hasCast,int castScore,boolean hasDlna,int dlnaScore,boolean wirelessAvailable,String preferred){
    ArrayList<MethodScore> methods=new ArrayList<>();
    if(hasCast)methods.add(new MethodScore("CAST",castScore));
    if(hasDlna)methods.add(new MethodScore("DLNA",dlnaScore));
    Collections.sort(methods,Comparator.comparingInt((MethodScore m)->m.score).reversed());
    ArrayList<String> out=new ArrayList<>();
    String pref=preferred==null?"":preferred.trim().toUpperCase();
    if(("CAST".equals(pref)&&hasCast)||("DLNA".equals(pref)&&hasDlna))out.add(pref);
    for(MethodScore m:methods)if(!out.contains(m.name))out.add(m.name);
    if(wirelessAvailable)out.add("WIRELESS_DISPLAY");
    return out;
  }

  static final class MethodScore{
    final String name;
    final int score;
    MethodScore(String n,int s){name=n;score=s;}
  }
}
