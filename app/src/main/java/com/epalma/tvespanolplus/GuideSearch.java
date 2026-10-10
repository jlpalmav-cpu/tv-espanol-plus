package com.epalma.tvespanolplus;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class GuideSearch {
  private GuideSearch() {}

  static String normalize(String value) {
    if (value == null) return "";
    String s = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replaceAll("\\p{M}+", "")
        .toLowerCase(Locale.ROOT)
        .replace('ç','c')
        .replaceAll("[^a-z0-9]+", " ")
        .trim()
        .replaceAll("\\s+", " ");
    if (s.equals("barca") || s.equals("fcb")) return "barcelona";
    s = s.replace(" barca ", " barcelona ");
    if (s.startsWith("barca ")) s = "barcelona " + s.substring(6);
    if (s.endsWith(" barca")) s = s.substring(0, s.length()-6) + " barcelona";
    s = s.replace(" fcb ", " barcelona ");
    if (s.startsWith("fcb ")) s = "barcelona " + s.substring(4);
    if (s.endsWith(" fcb")) s = s.substring(0, s.length()-4) + " barcelona";
    return s.trim().replaceAll("\\s+", " ");
  }

  static boolean matches(String query, String title, String description, String channelName) {
    return rank(query, title, description) >= 0;
  }

  static int rank(String query, String title, String description) {
    String q = normalize(query);
    if (q.isEmpty()) return -1;
    String t = normalize(title);
    String d = normalize(description);
    if (t.equals(q)) return 1200;
    if (!q.isEmpty() && t.contains(q)) return 1120;
    List<String> qt = usefulTokens(q);
    if (qt.isEmpty()) return -1;
    if (allExact(qt, t)) return 1040;
    if (allFuzzy(qt, t)) return 920;
    if (!d.isEmpty() && d.contains(q)) return 700;
    if (allExact(qt, d)) return 640;
    if (allFuzzy(qt, d)) return 520;
    return -1;
  }

  private static List<String> usefulTokens(String s) {
    ArrayList<String> out = new ArrayList<>();
    for (String x : s.split(" ")) {
      if (x.isEmpty() || x.equals("fc") || x.equals("cf") || x.equals("club") ||
          x.equals("vs") || x.equals("v") || x.equals("de") || x.equals("del") ||
          x.equals("la") || x.equals("el")) continue;
      out.add(x);
    }
    return out;
  }

  private static boolean allExact(List<String> queryTokens, String haystack) {
    if (haystack == null || haystack.isEmpty()) return false;
    List<String> hs = usefulTokens(haystack);
    for (String q : queryTokens) {
      boolean ok = false;
      for (String h : hs) if (h.equals(q)) { ok = true; break; }
      if (!ok) return false;
    }
    return true;
  }

  private static boolean allFuzzy(List<String> queryTokens, String haystack) {
    if (haystack == null || haystack.isEmpty()) return false;
    List<String> hs = usefulTokens(haystack);
    for (String q : queryTokens) {
      boolean ok = false;
      for (String h : hs) {
        if (h.equals(q)) { ok = true; break; }
        if (q.length() >= 5 && h.length() >= 5) {
          int limit = Math.max(q.length(), h.length()) >= 9 ? 2 : 1;
          if (Math.abs(q.length()-h.length()) <= limit && distance(q,h,limit) <= limit) {
            ok = true; break;
          }
        }
      }
      if (!ok) return false;
    }
    return true;
  }

  static int distance(String a, String b, int cutoff) {
    if (a.equals(b)) return 0;
    if (Math.abs(a.length()-b.length()) > cutoff) return cutoff + 1;
    int[] prev = new int[b.length()+1], cur = new int[b.length()+1];
    for (int j=0;j<=b.length();j++) prev[j]=j;
    for (int i=1;i<=a.length();i++) {
      cur[0]=i;
      int rowMin=cur[0];
      char ca=a.charAt(i-1);
      for (int j=1;j<=b.length();j++) {
        int cost=ca==b.charAt(j-1)?0:1;
        cur[j]=Math.min(Math.min(cur[j-1]+1,prev[j]+1),prev[j-1]+cost);
        rowMin=Math.min(rowMin,cur[j]);
      }
      if (rowMin>cutoff) return cutoff+1;
      int[] tmp=prev; prev=cur; cur=tmp;
    }
    return prev[b.length()];
  }
}
