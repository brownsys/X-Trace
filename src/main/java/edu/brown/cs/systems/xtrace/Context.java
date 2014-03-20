package edu.brown.cs.systems.xtrace;

import edu.brown.cs.systems.xtrace.Metadata.XTraceMetadata;
import edu.brown.cs.systems.xtrace.Metadata.XTraceMetadata.Builder;


public class Context {
  boolean immutable = false;
  Builder builder;
  Context(Builder builder) {
    this.builder = builder;
  }
  public byte[] bytes() {
    return builder.build().toByteArray();
  }
  public static Context parse(byte[] bytes) {
    try {
      return new Context(XTraceMetadata.newBuilder().mergeFrom(bytes));
    } catch (Exception e) {
      return null;
    }
  }
  public Context copy() {
    return new Context(builder.clone());
  }
}