package edu.brown.cs.systems.xtrace;

import edu.brown.cs.systems.xtrace.impl.PubSubLogger;

/**
 * The front door to X-Trace v3.  Has static instances of Log and Trace to allow
 * direct static invocation, allowing same kind of API interaction as previous versions of X-Trace
 * @author Jonathan Mace
 */
public class XTrace {
  
  private static Trace TRACE = new Trace();
  private static Logger LOG = new PubSubLogger(TRACE);

  /**
   * Set this thread's X-Trace metadata to the provided bytes
   * If the bytes are invalid, the metadata will be set to null
   * @param m byte representation of the metadata to set
   */
  public static void set(byte[] m) {
    TRACE.set(m);
  }
  
  /**
   * Set this thread's X-Trace metadata to the provided metadata.
   * Null metadata is allowed
   * @param metadata
   */
  public static void set(Context metadata) {
    TRACE.set(metadata);
  }
  
  /**
   * Merge this thread's X-Trace metadata with the provided metadata.
   * Null metadata is allowed
   * Semantics for a merge are:
   *   - if the thread's current X-Trace metadata is null, then this
   *     method behaves like the method set
   *   - the parent event ids are always unioned
   *   - the task id will be one of the task ids of the parents
   *   - the tenant class will be one of the tenant classes of the parents
   * @param metadata
   */
  public static void join(Context metadata) {
    TRACE.join(metadata);
  }
  
  /**
   * Merge this thread's X-Trace metadata with the provided bytes.
   * Null bytes are allowed
   * Semantics for a merge are:
   *   - if the thread's current X-Trace metadata is null, then this
   *     method behaves like the method set
   *   - the parent event ids are always unioned
   *   - the task id will be one of the task ids of the parents
   *   - the tenant class will be one of the tenant classes of the parents
   * @param metadata
   */
  public static void join(byte[] bytes) {
    TRACE.join(bytes);
  }
  
  
  
  /**
   * Get the current X-Trace metadata for this thread.
   * @return An X-Trace context containing the thread's current metadata
   */
  public static Context get() {
    return TRACE.get();
  }
  
  /**
   * Returns the byte representation of the current X-Trace metadata
   * @return the byte representation of the current X-Trace metadata,
   * or null if no metadata is set
   */
  public static byte[] bytes() {
    return TRACE.bytes();
  }
  
  /**
   * If there is a metadata currently set, returns true.
   * Note that the metadata may be an empty metadata
   * @return true if there is a metadata set, false otherwise
   */
  public static boolean exists() {
    return TRACE.exists();
  }

  
  /**
   * Returns true if we're able to send log messages.
   * @return
   */
  public static boolean canLog() {
    return LOG.canLog();
  }
  
  public boolean hasTaskID() {
    Context ctx = TRACE.peek();
    return ctx!=null && ctx.hasTaskID();
  }
  
  /**
   * Returns the task ID if one is set, otherwise null
   * @return
   */
  public Long getTaskID() {
    Context ctx = TRACE.peek();
    return ctx==null ? null : ctx.hasTaskID() ? ctx.getTaskID() : null;
  }
  
  public boolean hasTenantClass() {
    Context ctx = TRACE.peek();
    return ctx!=null && ctx.hasTenantClass();
  }
  
  /**
   * Returns the tenant class if one is set, otherwise null
   * @return tenant class if metadata exists and has a tenant class, null otherwise
   */
  public Integer getTenantClass() {
    Context ctx = TRACE.peek();
    return ctx==null ? null : ctx.hasTenantClass() ? ctx.getTenantClass() : null;
  }
  
  
  /**
   * Starts a new trace, creating new metadata only if necessary.
   * If metadata already exists, this method does nothing
   * This method will not propagate a tenant class
   * It is recommended to log an event after starting a trace; this method only starts the metadata propagation
   * @param trackCausality should we track causality?
   */
  public static void startTrace(boolean trackCausality) {
    if (TRACE.exists())
      return;
    
    Context ctx = Context.create(Logger.random.nextLong(), null, trackCausality ? 0L : null);
    TRACE.set(ctx);
  }
  
  /**
   * Starts a new trace, creating new metadata only if necessary.
   * If metadata already exists, this method does nothing
   * It is recommended to log an event after starting a trace; this method only starts the metadata propagation
   * @param tenantclass the class of the tenant to propagate
   * @param trackTask should we propagate a task id?
   * @param trackCausality should we propagate event ids?
   */
  public static void startTenantTrace(int tenantclass, boolean trackTask, boolean trackCausality) {
    if (TRACE.exists())
      return;
    
    Context ctx = Context.create(trackTask ? Logger.random.nextLong() : null, tenantclass, (trackCausality && trackTask) ? 0L : null);
    TRACE.set(ctx);
  }
  
  /**
   * If we are currently propagating metadata for an X-Trace task,
   * this method logs an event for the task
   * @param agent
   * @param label
   * @param fields
   */
  public static void logEvent(String agent, String label, Object... fields) {
    LOG.logEvent(agent, label, fields);
  }

  /**
   * If we are currently propagating metadata for an X-Trace task,
   * this method logs an event for the task
   * @param agent
   * @param label
   * @param fields
   */
  public static void logEvent(Class<?> agent, String label, Object... fields) {
    LOG.logEvent(agent, label, fields);
  }
  
}
