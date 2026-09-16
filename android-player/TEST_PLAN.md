# TV Español+ Player — Plan de pruebas v1.0.0

Criterio de salida: el APK se publica únicamente después de completar unitarias, lint, compilación e instrumentación en emulador.

- M3U: parser, cabecera, deduplicación, múltiples fuentes.
- Búsqueda: mayúsculas/minúsculas, acentos, puntuación, alias, fuzzy matching.
- Contexto temporal: en vivo primero, luego próximo, hoy, mañana, futuro.
- Recuperación: failover de stream y rechazo de actualización catastróficamente reducida.
- Rendimiento: búsqueda sobre 10,000 canales.
- UI/UX: arranque sin login, foco TV, navegación D-pad, acceso directo al canal.
- Vista doble: búsqueda independiente por panel, un solo audio, agrandar y liberar el otro reproductor.
- Sistema: actualización al abrir, periódica de 6 horas y al recuperar red.
- Instrumentación: lanzamiento real en emulador Android API 35.

© epalma
+504 99461582 - Tegucigalpa - Honduras
