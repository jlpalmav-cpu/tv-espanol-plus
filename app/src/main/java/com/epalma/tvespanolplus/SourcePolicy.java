package com.epalma.tvespanolplus;

import java.util.*;
import java.util.regex.Pattern;

/** Pure rules for PalmaVision multi-source management. */
public final class SourcePolicy {
  public static final String PALMAVISION_ID="palmavision";
  public static final String TYPE_SYSTEM="system";
  public static final String TYPE_LOCAL="local";
  public static final String TYPE_URL="url";
  public static final String TYPE_XTREAM="xtream";
  public static final String LIVE="live", MOVIE="movie", SERIES="series", MUSIC="music";
  private static final Pattern EPISODE=Pattern.compile("(?i).*(?:S\\d{1,2}E\\d{1,3}|\\bTEMP(?:ORADA)?\\s*\\d+|\\bEP(?:ISODIO)?\\s*\\d+).*");

  private SourcePolicy(){}

  public static boolean isProtected(String id){return PALMAVISION_ID.equals(id);}
  public static boolean canEdit(String id){return !isProtected(id);}
  public static boolean canDelete(String id){return !isProtected(id);}

  /** Exactly one source is active; unknown targets fall back to PalmaVision. */
  public static String selectActive(Collection<String> ids,String requested){
    if(PALMAVISION_ID.equals(requested))return PALMAVISION_ID;
    if(ids!=null&&requested!=null&&ids.contains(requested))return requested;
    return PALMAVISION_ID;
  }

  /** Classifies M3U entries without changing the immutable PalmaVision system list. */
  public static String classify(boolean systemSource,String name,String group,String url){
    if(systemSource)return LIVE;
    String n=norm(name),g=norm(group),u=(url==null?"":url).toLowerCase(Locale.ROOT);
    boolean audio=u.matches(".*\\.(mp3|aac|m4a|ogg|flac)(?:\\?.*)?$");
    if(audio||has(g,"radio","music","musica","música","audio")||has(n," radio ","fm ","am "))return MUSIC;
    if(hasPath(u,"/series/","/series?","/episode/","/episodes/")||has(g,"series","serie","tv show","tv shows","temporada","episodio")||EPISODE.matcher(name==null?"":name).matches())return SERIES;
    boolean fileVod=u.matches(".*\\.(mp4|mkv|avi|mov|m4v|webm)(?:\\?.*)?$");
    if(hasPath(u,"/movie/","/movies/","/vod/")||has(g,"movie","movies","pelicula","películas","peliculas","cine","cinema","vod","film")||fileVod)return MOVIE;
    return LIVE;
  }

  private static String norm(String x){return (" "+(x==null?"":x).toLowerCase(Locale.ROOT).replace('í','i').replace('ú','u')+" ").replaceAll("\\s+"," ");}
  private static boolean has(String x,String... terms){for(String t:terms)if(x.contains(t))return true;return false;}
  private static boolean hasPath(String x,String... terms){for(String t:terms)if(x.contains(t))return true;return false;}
}
