package edu.brown.cs.systems.xtrace;

import java.util.Random;

import edu.brown.cs.systems.xtrace.Metadata.XTraceMetadata;
import edu.brown.cs.systems.xtrace.Reporting.XTraceReport3;
import edu.brown.cs.systems.xtrace.Reporting.XTraceReport3.Builder;

/**
 * The Log class is the base class for X-Trace logging.
 * 
 * In X-Trace v3, it is separated from metadata propagation; one of the motivations of
 * X-Trace v3 is to use the X-Trace metadata propagation for purposes other than logging
 * 
 * Log is an abstract class that can be extended to build custom logging mechanisms.
 * Out of the box, X-Trace v3 offers 0MQ logging only.
 * 
 * @author Jonathan Mace
 */
public abstract class Logger {
  
  /**
   * An interface to automatically add additional fields to XTrace events when they're created.
   * Register decorators using the setDecorator method.  For now, only one decorator may
   * be registered
   * @author Jonathan Mace
   */
  public static interface Decorator {
    public Builder decorate(Builder builder);
  }
  
  protected static final Random random = new Random(31*(17 * Utils.getHost().hashCode() + Utils.getProcessID())*System.currentTimeMillis());
  
  protected final Trace xtrace;
  protected static final String host = Utils.getHost();
  protected static final int procid = Utils.getProcessID();
  protected static String procname = Utils.getProcessName();
  
  public Logger(Trace trace) {
    this.xtrace = trace;
  }
  
  protected Decorator decorator = null;
  
  public void setDecorator(Decorator decorator) {
    this.decorator = decorator;
  }
  
  protected Builder createEvent(String agent, String label, Object... fields) {
    // Nothing fancy; just allocate a new event object
    Builder builder = XTraceReport3.newBuilder();
    
    // Take a look at the current XTrace metadata
    XTraceMetadata.Builder metadata = xtrace.peek().builder;
    builder.setTaskID(metadata.getTaskID());
    
    // Record the tenant class if necessary
    if (metadata.hasTenantClass())
      builder.setTenantClass(metadata.getTenantClass());
    
    // Record causality if necessary
    if(metadata.getParentEventIDCount()!=0) {
      builder.addAllParentEventID(metadata.getParentEventIDList());
      long neweventid = random.nextLong();
      builder.setEventID(neweventid);
      xtrace.setParentEventID(neweventid);
    }
    
    // Set a bunch of simple fields
    builder.setHost(host);
    builder.setProcessID(procid);
    builder.setProcessName(procname);
    builder.setThreadID((int)Thread.currentThread().getId());
    builder.setThreadName(Thread.currentThread().getName());
    builder.setTimestamp(System.currentTimeMillis());
    builder.setHRT(System.nanoTime());
    
    // Set the user-defined fields
    builder.setAgent(agent);
    builder.setLabel(label);
    for (Object obj : fields) {
      if (obj!=null)
        builder.addValues(obj.toString());
      else
        builder.addValues("null");
    }
    
    // Decorate, if a decorator has been provided
    if (decorator!=null)
      return decorator.decorate(builder);
    else
      return builder;
  }
  
  /**
   * Returns true if we're able to send log messages.
   * @return
   */
  public boolean canLog() {
    Context ctx = xtrace.peek();
    return ctx!=null && ctx.builder.hasTaskID();
  }
  
  public void logEvent(String agent, String label, Object... fields) {
    if (!canLog())
      return;
    
    logEvent(createEvent(agent, label, fields));
  }
  
  public void logEvent(Class<?> agent, String label, Object... fields) {
    if (!canLog())
      return;
    
    logEvent(createEvent(agent.toString(), label, fields));
  }
  
  protected abstract void logEvent(Builder event);

  protected abstract void close();
}
