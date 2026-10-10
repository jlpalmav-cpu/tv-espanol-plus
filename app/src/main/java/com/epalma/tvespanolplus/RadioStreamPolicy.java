package com.epalma.tvespanolplus;

import java.net.URI;
import java.util.Locale;

final class RadioStreamPolicy {
  private RadioStreamPolicy() {}

  static boolean isLegacyPlaylistUrl(String raw) {
    String u = raw == null ? "" : raw.toLowerCase(Locale.ROOT).split("\\?")[0];
    return u.endsWith(".pls") || (u.endsWith(".m3u") && !u.endsWith(".m3u8"));
  }

  static String firstPlayable(String body, String baseUrl) {
    if (body == null) return "";
    String[] lines = body.replace("\r", "").split("\n");
    for (String raw : lines) {
      String s = raw.trim();
      if (s.isEmpty() || s.startsWith("#") || s.startsWith("[")) continue;
      int eq = s.indexOf('=');
      if (eq > 0 && s.substring(0, eq).trim().toLowerCase(Locale.ROOT).matches("file\\d+")) {
        s = s.substring(eq + 1).trim();
      } else if (eq > 0) {
        continue;
      }
      if (s.startsWith("http://") || s.startsWith("https://")) return s;
      try {
        if (baseUrl != null && !baseUrl.isEmpty()) return URI.create(baseUrl).resolve(s).toString();
      } catch (Exception ignored) {}
    }
    return "";
  }

  static String kind(String raw) {
    String u = raw == null ? "" : raw.toLowerCase(Locale.ROOT).split("\\?")[0];
    if (u.endsWith(".m3u8")) return "hls";
    if (u.endsWith(".mp3")) return "mp3";
    if (u.endsWith(".aac") || u.endsWith(".aacp")) return "aac";
    return "auto";
  }
}
