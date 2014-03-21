package edu.brown.cs.systems.xtrace;

import edu.brown.cs.systems.xtrace.Metadata.XTraceMetadata;
import edu.brown.cs.systems.xtrace.Metadata.XTraceMetadata.Builder;
import edu.brown.cs.systems.xtrace.Metadata.XTraceMetadataOrBuilder;

/**
 * The Context class holds information about an X-Trace task, prior events in an execution,
 * and information about the originator of the task.
 * 
 * The main purpose of the Context class is to be saved and restored in an instrumented application.
 * Contexts can be serialized and deserialized, created, set, and unset via static methods in the
 * XTrace class.
 * 
 * Modification of the data contained in a Context is possible via a few privileged API methods,
 * but is otherwise extremely restricted
 * 
 * @author Jonathan Mace
 *
 */
public class Context {
  
  /**
   * A context can only be modified via a Manager.  This disallows arbitrary access to
   * the data of the context, and forces all modifications to be routed through modify()
   * @author Jonathan Mace
   */
  static class Manager {

    /** The actual Context that is active for this thread */
    private ThreadLocal<Context> context = new ThreadLocal<Context>();

    /** Returns true if a context is currently active for this thread */
    public boolean exists() {
      return context.get()!=null;
    }
    
    /** Sets the thread's current context to the one provided */
    public void set(Context ctx) {
      context.set(ctx);
    }

    /** Sets the thread's current context to one parsed from the bytes provided.
     * If the bytes are null or invalid, the context will be cleared */
    public void set(byte[] bytes) {
      context.set(Context.parse(bytes));
    }
    
    /** Returns the thread's context or null if none is set */
    public Context get() {
      Context ctx = context.get();
      if (ctx!=null)
        ctx.modifiable = false;
      return ctx;
    }
    
    /** Returns the builder for the thread's context such that it can be modified
     * If there is not currently a context, a new one will be created */
    public XTraceMetadata.Builder modify() {
      Context ctx = context.get();
      if (ctx==null)
        context.set(ctx = new Context(XTraceMetadata.newBuilder()));
      else if (!ctx.modifiable)
        context.set(ctx = new Context(ctx.builder.clone()));
      return ctx.builder;
    }
    
    /** Returns a readonly view on the thread's context or null if none is set */
    public XTraceMetadataOrBuilder observe() {
      Context ctx = context.get();
      return ctx==null ? null : ctx.observe();
    }
    
    /** Returns the byte representation of this context */
    public byte[] bytes() {
      Context ctx = context.get();
      return ctx==null ? null : ctx.bytes();
    }
    
  }
  
  /**
   * Returns the serialized representation of this X-Trace context.
   * Can be deserialized with the method XTrace.parse
   * @return the byte representation of this Context
   */
  public byte[] bytes() {
    return builder.build().toByteArray();
  }
  
  /** Returns a readonly view on the context */
  XTraceMetadataOrBuilder observe() {
    return builder;
  }

  /** Used by the manager to keep track of accesses to the Context */
  private volatile boolean modifiable = true;
  
  /** The actual protobuf containing the context */
  private final Builder builder;

  /** Create a new Context backed by the provided builder */
  private Context(Builder builder) {
    this.builder = builder;
  }
  
  /** Create a new empty Context */
  public Context() {
    this(XTraceMetadata.newBuilder());
  }
  
  /**
   * Parse the protocol buffers bytes and put them in a new Context
   * @param bytes protocol buffers serialized representation of the metadata, may be null
   * @return a new context wrapping the deserialized protocol buffers representation.
   * Returns null if the provided bytes were null or if there was an exception deserializing the bytes
   */
  public static Context parse(byte[] bytes) {
    if (bytes==null)
      return null;
    try {
      return new Context(XTraceMetadata.newBuilder().mergeFrom(bytes));
    } catch (Exception e) {
      return null;
    }
  }


}