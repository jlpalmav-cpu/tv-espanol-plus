package com.epalma.tvespanolplus;

final class SecuritySanitizer {
  private SecuritySanitizer(){}

  static String redact(String raw){
    if(raw==null) return "";
    String x=raw.replaceAll("(?i)(password=)[^&\\s]+","$1••••")
        .replaceAll("(?i)(username=)[^&\\s]+","$1••••");
    return x.replaceAll("(?i)(https?://)([^/@:]+):([^/@]+)@","$1••••:••••@");
  }
}
