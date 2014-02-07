package com.yuvalshavit.antlr4.examples.util;

import java.util.concurrent.atomic.*;

/**
 * based on XorShift: http://jcip.net.s3-website-us-east-1.amazonaws.com/listings/XorShift.java
 * Code is public domain: see http://jcip.net.s3-website-us-east-1.amazonaws.com/listings.html
 *
 * When I say "based on" I mean "identical except for class name and package."
 *
 * @author Brian Goetz and Tim Peierls
 */
public class QuickRandom {
  static final AtomicInteger seq = new AtomicInteger(8862213);
  int x = -1831433054;

  public QuickRandom() {
    this((int) System.nanoTime() + seq.getAndAdd(129));
  }

  public QuickRandom(int seed) {
    x = seed;
  }

  public int next() {
    x ^= x << 6;
    x ^= x >>> 21;
    x ^= (x << 7);
    return x;
  }
}