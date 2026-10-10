package com.epalma.tvespanolplus;

final class RadioControlPolicy {
  private RadioControlPolicy() {}

  static int adjacentIndex(int current, int size, int delta) {
    if (size <= 0) return -1;
    int base = current < 0 || current >= size ? 0 : current;
    int out = (base + delta) % size;
    if (out < 0) out += size;
    return out;
  }

  static int nextSourceIndex(int current, int size) {
    if (size <= 1) return 0;
    int base = current < 0 ? 0 : current;
    return (base + 1) % size;
  }
}
