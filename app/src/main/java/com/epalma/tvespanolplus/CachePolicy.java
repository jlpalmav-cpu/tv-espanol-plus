package com.epalma.tvespanolplus;

import java.text.Normalizer;
import java.util.Locale;

/** Pure policy helpers used by the recovery, VOD seek and music-radio layers. */
public final class CachePolicy {
  private CachePolicy() {}

  public static boolean acceptableCount(int previousCount, int candidateCount) {
    if (candidateCount < 1 || candidateCount > 100000) return false;
    if (previousCount < 20) return true;
    int floor = Math.max(10, (int)Math.floor(previousCount * 0.35d));
    return candidateCount >= floor;
  }

  public static boolean shouldResume(long positionMs, long durationMs) {
    if (positionMs < 60000L) return false;
    if (durationMs <= 0L) return true;
    if (positionMs >= durationMs - 120000L) return false;
    return positionMs < (long)(durationMs * 0.92d);
  }

  public static long seekDeltaForRepeat(int repeatCount) {
    if (repeatCount >= 8) return 60000L;
    if (repeatCount >= 3) return 30000L;
    return 10000L;
  }

  public static String radioGenre(String language, String tags, String name) {
    String x = norm((tags == null ? "" : tags) + " " + (name == null ? "" : name));
    if (containsAny(x,"news","noticias","talk","sports","deportes","podcast","relig","sermon","politic","traffic","weather","weatheradio")) return null;
    boolean spanish = norm(language).contains("span") || norm(language).contains("espan") || norm(language).equals("es");
    boolean english = norm(language).contains("english") || norm(language).equals("en");
    if (!spanish && !english) return null;

    if (spanish) {
      if (containsAny(x,"salsa")) return "Salsa";
      if (containsAny(x,"merengue")) return "Merengue";
      if (containsAny(x,"bachata")) return "Bachata";
      if (containsAny(x,"reggaeton","urbano","urban latin","trap latino")) return "Urbano / Reggaetón";
      if (containsAny(x,"regional mexicana","ranchera","norteno","nortena","banda","mariachi")) return "Regional mexicana";
      if (containsAny(x,"romantic","romantica","balada","love songs")) return "Romántica / Baladas";
      if (containsAny(x,"rock latino","rock en espanol","latin rock")) return "Rock Latino";
      if (containsAny(x,"electronic","electronica","dance","edm","house","techno")) return "Electrónica";
      if (containsAny(x,"oldies","clasicos","classic hits","80s","90s","2000s")) return "Clásicos";
      if (containsAny(x,"latin pop","pop latino","pop","hits","musica","music","variety","variada")) return "Pop / Variada";
      return null;
    }

    if (containsAny(x,"classic rock")) return "Classic Rock";
    if (containsAny(x,"alternative","indie")) return "Alternative";
    if (containsAny(x,"r&b","rnb","soul")) return "R&B / Soul";
    if (containsAny(x,"hip hop","hip-hop","rap")) return "Hip-Hop";
    if (containsAny(x,"country")) return "Country";
    if (containsAny(x,"electronic","dance","edm","house","techno")) return "Dance / Electronic";
    if (containsAny(x,"jazz","blues")) return "Jazz / Blues";
    if (containsAny(x,"80s")) return "80s";
    if (containsAny(x,"90s")) return "90s";
    if (containsAny(x,"2000s","00s")) return "2000s";
    if (containsAny(x,"oldies","classic hits","60s","70s")) return "Oldies";
    if (containsAny(x,"rock")) return "Rock";
    if (containsAny(x,"pop","top 40","hits","music","variety")) return "Pop / Hits";
    return null;
  }

  static String norm(String raw) {
    if (raw == null) return "";
    return Normalizer.normalize(raw, Normalizer.Form.NFD)
        .replaceAll("\\p{M}+", "")
        .toLowerCase(Locale.ROOT)
        .replace('_',' ')
        .trim();
  }

  static boolean containsAny(String x, String... needles) {
    for (String n : needles) if (x.contains(n)) return true;
    return false;
  }
}