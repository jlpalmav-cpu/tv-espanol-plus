package com.epalma.tvespanolplus;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

final class EpgPolicy {
  private EpgPolicy(){}

  static boolean usesAutoFallback(String raw){
    String x=raw==null?"":raw.toLowerCase(Locale.ROOT);
    return x.contains("dearbulut.github.io/iptv/epg/guide.xml");
  }

  static List<String> candidates(String raw){
    LinkedHashSet<String> out=new LinkedHashSet<>();
    if(raw!=null){
      for(String part:raw.split(",")){
        String u=part.trim();
        if(u.isEmpty()) continue;
        out.add(u);
        String l=u.toLowerCase(Locale.ROOT);
        if(l.endsWith(".xml.gz")) out.add(u.substring(0,u.length()-3));
        else if(l.endsWith(".xml")) out.add(u+".gz");
      }
    }
    if(usesAutoFallback(raw)){
      out.add("https://i.mjh.nz/SamsungTVPlus/all.xml.gz");
      out.add("https://i.mjh.nz/PlutoTV/all.xml.gz");
      out.add("https://i.mjh.nz/Plex/all.xml.gz");
      out.add("https://raw.githubusercontent.com/dearbulut/iptv/state/epg/mi-tv.xml.gz");
      out.add("https://raw.githubusercontent.com/dearbulut/iptv/state/epg/tvplus-com-tr.xml.gz");
    }
    return new ArrayList<>(out);
  }
}
