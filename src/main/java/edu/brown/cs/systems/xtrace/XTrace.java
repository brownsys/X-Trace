package edu.brown.cs.systems.xtrace;

import edu.brown.cs.systems.xtrace.Metadata.XTraceMetadataOrBuilder;

/**
 * The front door to X-Trace v3.
 * 
 * Metadata propagation is provided as static methods on the X-Trace class
 * 
 * Logging is provided as instance methods on instances returned by XTrace.getLogger
 * 
 * Provides static methods for X-Trace metadata propagation.  
 * Also provides static methods to return logger instances for logging against named classes.
 * 
 * @author Jonathan Mace
 */
public class XTrace {

  private static final Trace METADATA = new Trace();
  private static final Reporter REPORTER = new PubSubReporter(METADATA);
  
  public interface Logger {
    public boolean valid();
    public void logEvent(String message, Object... labels);    
  }
  
  /** If logging is turned off for an agent, then they're given the null logger which does nothing */
  static Logger NULL_LOGGER = new Logger() {
    public boolean valid() {
      return false;
    }
    public void logEvent(String message, Object... labels) {
    }
  };
  
  static class LoggerImpl implements Logger {
    private final String agent;
    public LoggerImpl(String agent) {
      this.agent = agent;
    }
    public boolean valid() {
      return REPORTER.valid();
    }
    public void logEvent(String message, Object... labels) {
      REPORTER.sendReport(agent, message, labels);
    }
  }
  
  /**
   * Returns the default logger
   * @return
   */
  public static Logger getLogger() {
    if (XTraceSettings.REPORTING_ENABLED_DEFAULT)
      return new LoggerImpl("default");
    else
      return NULL_LOGGER;
  }
  
  public static Logger getLogger(String agent) {
    if (agent==null)
      return NULL_LOGGER;
    else if (XTraceSettings.REPORTING_ENABLED_DEFAULT && !XTraceSettings.REPORTING_DISABLED.contains(agent))
      return new LoggerImpl(agent);
    else if (XTraceSettings.REPORTING_ENABLED.contains(agent))
      return new LoggerImpl(agent);
    else
      return NULL_LOGGER;
  }
  
  /**
   * Shorthand for getLogger(agent.getName())
   * @param agent The name of the agent will be used as the name of the logger to retrieve
   * @return an xtrace event logger that can be used to log events
   */
  public static Logger getLogger(Class<?> agent) {
     if (agent==null)
       return NULL_LOGGER;
     else
       return getLogger(agent.getName());
  }

  /**
   * Instructs X-Trace to start propagating the provided metadata in this thread
   * 
   * @param bytes
   *          byte representation of the X-Trace metadata to start propagating
   *          in this thread
   */
  public static void set(byte[] bytes) {
    METADATA.set(bytes);
  }

  /**
   * Instructs X-Trace to start propagating the provided metadata in this thread
   * 
   * @param metadata
   *          the metadata to start propagating in this thread
   */
  public static void set(Context metadata) {
    METADATA.set(metadata);
  }

  /**
   * Merge the metadata provided into the metadata currently being propagated by
   * this thread. If nothing is currently being propagated in this thread, this
   * method call is equivalent to set
   * 
   * @param metadata
   *          the metadata to merge into this thread
   */
  public static void join(Context metadata) {
    METADATA.join(metadata);
  }

  /**
   * Merge the metadata provided into the metadata currently being propagated by
   * this thread. If nothing is currently being propagated in this thread, this
   * method call is equivalent to set
   * 
   * @param bytes
   *          the byte representation of the X-Trace metadata to merge into this
   *          thread
   */
  public static void join(byte[] bytes) {
    METADATA.join(bytes);
  }

  /**
   * @return the X-Trace metadata being propagated in this thread
   */
  public static Context get() {
    return METADATA.get();
  }

  /**
   * @return the byte representation of the X-Trace metadata being propagated in
   *         this thread
   */
  public static byte[] bytes() {
    return METADATA.bytes();
  }

  /**
   * @return true if X-Trace is currently propagating metadata in this thread
   */
  public static boolean active() {
    return METADATA.exists();
  }

  /**
   * Stops propagating any X-Trace metadata in this thread
   */
  public static void stop() {
    METADATA.clear();
  }

  /**
   * @return true if a task ID is being propagated by X-Trace in this thread
   */
  public static boolean hasTaskID() {
    XTraceMetadataOrBuilder xmd = METADATA.observe();
    return xmd == null ? false : xmd.hasTaskID();
  }

  /**
   * @return true if a tenant class is being propagated by X-Trace in this
   *         thread
   */
  public static boolean hasTenantClass() {
    XTraceMetadataOrBuilder xmd = METADATA.observe();
    return xmd == null ? false : xmd.hasTenantClass();
  }

  /**
   * @return the task ID currently being propagated by X-Trace in this thread,
   *         or null if none being propagated
   */
  public static Long getTaskID() {
    XTraceMetadataOrBuilder xmd = METADATA.observe();
    return xmd == null ? null : xmd.hasTaskID() ? xmd.getTaskID() : null;
  }

  /**
   * @return the tenant class currently being propagated by X-Trace in this
   *         thread, or null if none being propagated
   */
  public static Integer getTenantClass() {
    XTraceMetadataOrBuilder xmd = METADATA.observe();
    return xmd == null ? null : xmd.hasTenantClass() ? xmd.getTenantClass() : null;
  }
  
  /**
   * @return true if the thread currently has multiple parent X-Trace event IDs,
   * and it is therefore worth logging a message before serializing.
   */
  public static boolean shouldLogBeforeSerialization() {
    XTraceMetadataOrBuilder xmd = METADATA.observe();
    return xmd == null ? false : xmd.getParentEventIDCount() > 1;
  }

  /**
   * Start propagating a task ID in this thread if we aren't already propagating
   * a task ID.  If we aren't currently propagating a task ID, a new one is
   * randomly generated
   * 
   * @param trackCausality
   *          should we also track causality for this task?
   */
  public static void startTask(boolean trackCausality) {
    setTask(Reporter.random.nextLong(), trackCausality);
  }

  /**
   * Start propagating the specified taskID in this thread.  If X-Trace is already
   * propagating a taskid in this thread, then this method call does nothing.
   * 
   * @param taskid
   *          the taskID to start propagating in this thread
   * @param trackCausality
   *          should we also track causality for this task?
   */
  public static void setTask(long taskid, boolean trackCausality) {
    XTraceMetadataOrBuilder current = METADATA.observe();
    if (current != null && current.hasTaskID())
      return;

    if (trackCausality)
      METADATA.modify().setTaskID(Reporter.random.nextLong()).clearParentEventID().addParentEventID(0L);
    else
      METADATA.modify().setTaskID(Reporter.random.nextLong()).clearParentEventID();
  }

  /**
   * Start propagating the specified tenant class in this thread.  If X-Trace is
   * already propagating a tenant class, then it will be overwritten by the
   * tenant class provided.
   */
  public static void setTenantClass(int tenantclass) {
    METADATA.modify().setTenantClass(tenantclass);
  }
  
  

}
