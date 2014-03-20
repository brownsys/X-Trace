package edu.brown.cs.systems.xtrace;

import edu.brown.cs.systems.xtrace.Metadata.XTraceMetadata;
import edu.brown.cs.systems.xtrace.Metadata.XTraceMetadata.Builder;

/**
 * The Trace class is the base class for propagating X-Trace metadata in a client application
 * 
 * In X-Trace version 3, the API methods of this class are NOT static, to
 * allow an application to log across multiple slices of an application simultaneously
 * 
 * However, for ease of use, a static instance of Trace is available in the
 * XTraceContext class
 * 
 * @author Jonathan Mace
 */
public class Trace {
  
  private ThreadLocal<Context> context = new ThreadLocal<Context>(); 

  /**
   * Set this thread's X-Trace metadata to the provided bytes
   * If the bytes are invalid, the metadata will be set to null
   * @param m byte representation of the metadata to set
   */
  public void set(byte[] m) {
    // First, if setting to null, clear current and return
    if (m==null) {
      context.remove();
      return;
    }
    
    // Otherwise, just create and set a new context
    context.set(Context.parse(m));
  }
  
  /**
   * Set this thread's X-Trace metadata to the provided metadata.
   * Null metadata is allowed
   * @param metadata
   */
  public void set(Context metadata) {
    // First, if setting to null, clear current and return
    if (metadata==null) {
      context.remove();
      return;
    }
    
    // Otherwise, set the context
    context.set(metadata);
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
    // Do nothing if provided metadata is null
    if (metadata==null)
      return;
    
    // Do a set if the current metadata is null
    Context current = context.get();
    if (current==null)
      context.set(metadata);
    
    // Break out early if current is the same as metadata
    if (current==metadata)
      return;
    
    // Do nothing if provided metadata has no parents
    if (metadata.builder.getParentEventIDCount()==0)
      return;
    
    // Do a set if the current metadata has no parents
    if (current.builder.getParentEventIDCount()==0)
      context.set(metadata);
    
    // Check to see whether the parents are different
    int numToAdd = 0;
    long[] toAdd = new long[metadata.builder.getParentEventIDCount()];
    parents: for (int i = 0; i < toAdd.length; i++) {
      long parenti = metadata.builder.getParentEventID(i);
      for (int j = 0; j < current.builder.getParentEventIDCount(); j++) {
        if (current.builder.getParentEventID(j)==parenti) {
          continue parents;
        }
      }
      toAdd[numToAdd++] = parenti;
    }
    
    // If there are parents to add and our current is immutable, create and set a new one
    if (current.immutable && numToAdd!=0)
      context.set(current = current.copy());
      
    // Add new parents
    for (int i = 0; i < numToAdd; i++)
      current.builder.addParentEventID(toAdd[i]);
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
    if (bytes==null)
      return;
    
    join(Context.parse(bytes));
  }
  
  
  
  /**
   * Get the current X-Trace metadata for this thread.
   * @return An X-Trace context containing the thread's current metadata
   */
  public Context get() {
    Context current = context.get();
    if (current!=null)
      current.immutable = true;
    return current;
  }
  
  /**
   * Returns the byte representation of the current X-Trace metadata
   * @return the byte representation of the current X-Trace metadata,
   * or null if no metadata is set
   */
  public byte[] bytes() {
    Context current = context.get();
    if (current!=null)
      return current.bytes();
    return null;
  }
  
  /**
   * Take a look at the contents of the current X-Trace metadata.
   * @return The protocol buffers representation of the current metadata
   */
  Context peek() {
    return context.get();
  }
  
  boolean immutable() {
    Context current = context.get();
    return current==null ? false : current.immutable;
  }
  
  /**
   * If there is a metadata currently set, returns true.
   * Note that the metadata may be an empty metadata
   * @return true if there is a metadata set, false otherwise
   */
  public boolean exists() {
    return context.get()!=null;
  }
  
  /**
   * Sets the parent event ID of the current context to the provided id.
   * This API method enables X-Trace logging to update event IDs for
   * logging causality.
   * This method will remove all current parent IDs and update the metadata
   * parent ID to the provided ID.  If the current context is immutable,
   * a new context is created.  If the current context is null, this method
   * does nothing.
   * @param eventid the parent event id to set in the metadata.
   * @param reuseifpossible if true (defaults to true), reuses the context.
   * this is usually safe to do, since contexts are marked as immutable when
   * a user retrieves it
   */
  void setParentEventID(long eventid, boolean reuseifpossible) {
    Context current = context.get();
    if (current!=null) {
      if (!current.immutable && reuseifpossible) {
        current.builder.clearParentEventID();
      } else {
        Builder newbuilder = XTraceMetadata.newBuilder();
        newbuilder.setTaskID(current.builder.getTaskID());
        newbuilder.setTenantClass(current.builder.getTenantClass());
        current = new Context(newbuilder);
        context.set(current);
      }
      current.builder.addParentEventID(eventid);
    }
  }
  
  void setParentEventID(long eventid) {
    setParentEventID(eventid, true);
  }
  
  
}
