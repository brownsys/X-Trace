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
  public void set(Context metadata) {
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
  public void join(Context metadata) {
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
  public void join(byte[] bytes) {
    TRACE.join(bytes);
  }
  
  
  
  /**
   * Get the current X-Trace metadata for this thread.
   * @return An X-Trace context containing the thread's current metadata
   */
  public Context get() {
    return TRACE.get();
  }
  
  /**
   * Returns the byte representation of the current X-Trace metadata
   * @return the byte representation of the current X-Trace metadata,
   * or null if no metadata is set
   */
  public byte[] bytes() {
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
  public boolean canLog() {
    return LOG.canLog();
  }
  
  public void logEvent(String agent, String label, Object... fields) {
    LOG.logEvent(agent, label, fields);
  }
  
  public void logEvent(Class<?> agent, String label, Object... fields) {
    LOG.logEvent(agent, label, fields);
  }
  
}
