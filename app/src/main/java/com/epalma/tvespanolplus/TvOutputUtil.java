package com.epalma.tvespanolplus;

import java.net.URL;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TvOutputUtil {
  private TvOutputUtil(){}

  public static boolean directStreamFriendly(String url){
    if(url==null)return false;
    String u=url.trim().toLowerCase(Locale.ROOT);
    if(!(u.startsWith("http://")||u.startsWith("https://")))return false;
    return u.contains(".m3u8")||u.contains(".mp4")||u.contains(".ts")||u.contains("output=ts")||u.contains("format=m3u8");
  }

  public static int score(String tech, boolean internet, boolean localNetwork, boolean directFriendly, boolean actualDevice){
    String t=tech==null?"":tech.toUpperCase(Locale.ROOT);
    int s;
    if("CAST".equals(t)){
      s=internet?94:52;
      if(actualDevice)s+=3;
      if(!directFriendly)s-=18;
    }else if("DLNA".equals(t)){
      s=localNetwork?84:48;
      if(actualDevice)s+=5;
      if(!directFriendly)s-=14;
      if(!internet&&localNetwork)s+=3;
    }else if("MIRACAST".equals(t)){
      s=internet?78:96;
      if(!directFriendly)s+=8;
      if(actualDevice)s+=1;
    }else{
      s=55;
    }
    if(s>100)s=100;
    if(s<0)s=0;
    return s;
  }

  public static String reason(String tech, boolean internet, boolean localNetwork, boolean directFriendly, boolean actualDevice){
    String t=tech==null?"":tech.toUpperCase(Locale.ROOT);
    if("CAST".equals(t)){
      if(!internet)return "Cast disponible, pero sin Internet la reproducción directa puede fallar.";
      if(!directFriendly)return "Cast disponible; este canal puede requerir autenticación especial, por eso duplicar pantalla puede ser más estable.";
      return actualDevice?"Receptor Cast detectado en la red; reproduce el canal directamente y deja libre el teléfono.":"Google Cast disponible; busca Chromecast o Google TV en la misma red.";
    }
    if("DLNA".equals(t)){
      if(!localNetwork)return "DLNA requiere que teléfono y Smart TV compartan la misma red local.";
      if(!directFriendly)return "Smart TV detectado, aunque este stream puede no ser compatible con reproducción directa.";
      return actualDevice?"Smart TV/renderer DLNA detectado en la red local; no necesita instalar nada en el TV.":"DLNA disponible en red local; se buscarán Smart TVs compatibles.";
    }
    if("MIRACAST".equals(t)){
      if(!internet)return "Recomendado sin Internet: duplica directamente la pantalla mediante la función inalámbrica del sistema.";
      if(!directFriendly)return "Recomendado para este canal porque duplica lo que reproduce el teléfono y evita incompatibilidades del stream.";
      return "Duplica toda la pantalla; útil para TVs y proyectores con pantalla inalámbrica/Miracast.";
    }
    return "Opción de salida disponible.";
  }

  public static String header(String raw,String name){
    if(raw==null||name==null)return "";
    Pattern p=Pattern.compile("(?im)^"+Pattern.quote(name)+"\\s*:\\s*(.+?)\\s*$");
    Matcher m=p.matcher(raw);
    return m.find()?m.group(1).trim():"";
  }

  public static String friendlyName(String xml){
    return tag(xml,"friendlyName");
  }

  public static String avTransportControlUrl(String xml){
    if(xml==null)return "";
    Pattern service=Pattern.compile("(?is)<service>.*?<serviceType>\\s*urn:schemas-upnp-org:service:AVTransport:[^<]+</serviceType>.*?<controlURL>\\s*([^<]+)\\s*</controlURL>.*?</service>");
    Matcher m=service.matcher(xml);
    return m.find()?decodeXml(m.group(1).trim()):"";
  }

  public static String resolveControlUrl(String location,String control){
    try{return new URL(new URL(location),control).toString();}catch(Exception e){return "";}
  }

  public static String xmlEscape(String s){
    if(s==null)return "";
    return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace(""","&quot;").replace("'","&apos;");
  }

  private static String tag(String xml,String tag){
    if(xml==null)return "";
    Pattern p=Pattern.compile("(?is)<"+tag+">\\s*(.*?)\\s*</"+tag+">");
    Matcher m=p.matcher(xml);
    return m.find()?decodeXml(m.group(1).trim()):"";
  }

  private static String decodeXml(String s){
    return s.replace("&amp;","&").replace("&lt;","<").replace("&gt;",">").replace("&quot;","\"").replace("&apos;","'");
  }
}
