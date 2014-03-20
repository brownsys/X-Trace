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
  
  /**
   * Creates and returns a new Context.  The context is not set anywhere; call a set(Context)
   * method to set it to the active context.
   * @param randomTaskID if true, generates and includes a random task ID in the metadata. if false, no task id is included
   * @param isCausal if randomTaskID is true and isCausal is true, adds causality tracing to the metadata
   * @param tenantClass if null, no tenant class is included. if not null, the tenantclass specified is included
   * @return a new context
   */
  public static Context create(boolean randomTaskID, boolean isCausal, Integer tenantClass) {
    if (randomTaskID && isCausal)
      return create(Logger.random.nextLong(), tenantClass, 0L);
    else if (randomTaskID)
      return create(Logger.random.nextLong(), tenantClass);
    else
      return create(null, tenantClass);
  }

  /**
   * Creates and returns a new Context.  The context is not set anywhere; call a set(Context)
   * method to set it to the active context.
   * @param taskid The task ID to set in the context, or null if no task id desired
   * @param tenantclass The tenant class to set in the context, or null if no tenant class desired
   * @param parentIds The causal parent ids to set in the context, optional, and only valid if taskid is not null
   * @return
   */
  public static Context create(Long taskid, Integer tenantclass, Long... parentIds) {
    Builder builder = XTraceMetadata.newBuilder();
    if (taskid!=null)
      builder.setTaskID(taskid);
    if (tenantclass!=null)
      builder.setTenantClass(tenantclass);
    if (taskid!=null)
      for (Long parentid : parentIds)
        if (parentid!=null)
          builder.addParentEventID(parentid);
    return new Context(builder);
  }
  
}