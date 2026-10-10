package com.epalma.tvespanolplus;

import java.text.Normalizer;
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
    return s.trim();
  }

  static boolean matches(String normalizedQuery, String title, String description, String channelName) {
    String q = normalize(normalizedQuery);
    if (q.isEmpty()) return true;
    String hay = normalize((title == null ? "" : title) + " " +
        (description == null ? "" : description) + " " +
        (channelName == null ? "" : channelName));
    if (hay.contains(q)) return true;
    String[] words = q.split(" ");
    int useful = 0;
    for (String word : words) {
      if (word.isEmpty() || word.equals("fc") || word.equals("cf")) continue;
      useful++;
      if (!hay.contains(word)) return false;
    }
    return useful > 0;
  }
}
