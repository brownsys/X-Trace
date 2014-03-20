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
   * Creates a new context that has neither a taskid, nor a tenantclass, nor any parent ids
   */
  public static Context blank() {
    return new Context(XTraceMetadata.newBuilder());
  }

  /**
   * Creates a new context that has the given taskid, but no tenant class or any parent ids
   */
  public static Context newTask(Long taskid, boolean isTaskCausal) {
    if (isTaskCausal)
      return create(taskid, null, 0L);
    else
      return create(taskid, null);
  }

  /**
   * Creates a new context that has the given taskid and tenant class
   */
  public static Context newTask(Long taskid, Integer tenantclass, boolean isTaskCausal) {
    if (isTaskCausal)
      return create(taskid, tenantclass, 0L);
    else
      return create(taskid, tenantclass);
  }
  
  /**
   * Creates a new context that has the given tenant class, but no taskid
   */
  public static Context forTenant(Integer tenantclass) {
    return create(null, tenantclass);
  }
  
  public static Context create(Long taskid, Integer tenantclass, Long... parentIds) {
    Builder builder = XTraceMetadata.newBuilder();
    if (taskid!=null)
      builder.setTaskID(taskid);
    if (tenantclass!=null)
      builder.setTenantClass(tenantclass);
    for (Long parentid : parentIds)
      if (parentid!=null)
        builder.addParentEventID(parentid);
    return new Context(builder);
  }
  
}