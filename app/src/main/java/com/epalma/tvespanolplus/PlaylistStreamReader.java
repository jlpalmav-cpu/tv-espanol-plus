package com.epalma.tvespanolplus;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Streams very large playlists without keeping the whole response in RAM.
 * A small prefix is retained only when the full playlist stays within cacheMax.
 */
final class PlaylistStreamReader {
  static final int CACHE_MAX_BYTES = 24 * 1024 * 1024;
  static final long HARD_MAX_BYTES = 256L * 1024L * 1024L;

  private PlaylistStreamReader() {}

  static final class CappedCacheInputStream extends FilterInputStream {
    private final int cacheMax;
    private final long hardMax;
    private final ByteArrayOutputStream cache;
    private long total;
    private boolean cacheOverflow;

    CappedCacheInputStream(InputStream in, int cacheMax, long hardMax) {
      super(in);
      this.cacheMax = Math.max(0, cacheMax);
      this.hardMax = Math.max(1L, hardMax);
      this.cache = new ByteArrayOutputStream(Math.min(this.cacheMax, 1024 * 1024));
    }

    @Override public int read() throws IOException {
      int v = super.read();
      if (v >= 0) {
        count(1);
        if (!cacheOverflow) {
          if (cache.size() < cacheMax) cache.write(v);
          else cacheOverflow = true;
        }
      }
      return v;
    }

    @Override public int read(byte[] b, int off, int len) throws IOException {
      int n = super.read(b, off, len);
      if (n > 0) {
        count(n);
        if (!cacheOverflow) {
          int remaining = cacheMax - cache.size();
          if (n <= remaining) cache.write(b, off, n);
          else cacheOverflow = true;
        }
      }
      return n;
    }

    private void count(int n) throws IOException {
      total += n;
      if (total > hardMax) throw new IOException("Lista supera el límite de seguridad");
    }

    long totalBytes() { return total; }
    boolean cacheable() { return !cacheOverflow && total > 0; }
    byte[] cachedBytes() { return cacheable() ? cache.toByteArray() : null; }
  }
}
