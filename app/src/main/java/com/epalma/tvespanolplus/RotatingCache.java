package com.epalma.tvespanolplus;

import java.io.*;
import java.util.Arrays;

/** Small three-generation cache. Callers decide whether bytes need encryption. */
public final class RotatingCache {
  private RotatingCache() {}

  public static File file(File dir, String key, int generation) {
    return new File(dir, key + "." + generation + ".cache");
  }

  public static byte[] read(File dir, String key, int generation, int maxBytes) throws IOException {
    File f = file(dir,key,generation);
    if (!f.isFile()) return null;
    if (f.length() <= 0 || f.length() > maxBytes) throw new IOException("cache-size");
    ByteArrayOutputStream out = new ByteArrayOutputStream((int)Math.min(f.length(), 1024 * 1024));
    try (InputStream in = new BufferedInputStream(new FileInputStream(f))) {
      byte[] buf = new byte[8192]; int n,total=0;
      while ((n=in.read(buf))!=-1) {
        total += n; if (total > maxBytes) throw new IOException("cache-too-large");
        out.write(buf,0,n);
      }
    }
    return out.toByteArray();
  }

  public static void write(File dir, String key, byte[] data, int generations) throws IOException {
    if (data == null || data.length == 0) throw new IOException("empty-cache");
    if (!dir.exists() && !dir.mkdirs()) throw new IOException("cache-dir");
    int keep = Math.max(1,Math.min(5,generations));
    byte[] latest = null;
    try { latest = read(dir,key,0,Math.max(data.length*2,1024)); } catch (Exception ignored) {}
    if (latest != null && Arrays.equals(latest,data)) return;
    for (int i=keep-1;i>=1;i--) {
      File src=file(dir,key,i-1), dst=file(dir,key,i);
      if (dst.exists() && !dst.delete()) throw new IOException("cache-delete");
      if (src.exists() && !src.renameTo(dst)) copy(src,dst);
    }
    File tmp = new File(dir,key+".tmp");
    try (FileOutputStream out=new FileOutputStream(tmp)) { out.write(data); out.getFD().sync(); }
    File dst=file(dir,key,0);
    if (dst.exists() && !dst.delete()) throw new IOException("cache-replace");
    if (!tmp.renameTo(dst)) { copy(tmp,dst); tmp.delete(); }
  }

  private static void copy(File src,File dst)throws IOException{
    try(InputStream in=new FileInputStream(src);OutputStream out=new FileOutputStream(dst)){
      byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);
    }
  }
}
