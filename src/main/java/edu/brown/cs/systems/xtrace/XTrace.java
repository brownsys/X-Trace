package edu.brown.cs.systems.xtrace;

import java.util.List;

import edu.brown.cs.systems.xtrace.Metadata.XTraceMetadataOrBuilder;
import edu.brown.cs.systems.xtrace.Reporting.XTraceReport3;
import edu.brown.cs.systems.xtrace.Reporting.XTraceReport3.Builder;

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

  static final Trace METADATA = new Trace();
  static final Reporter REPORTER = new PubSubReporter(METADATA);
  
  public interface Logger {
    /** Returns true if this logger is currently able to send reports */
    public boolean valid();
    /** Creates and sends a report */
    public void log(String message, Object... labels);
    /** Creates and sends a report, adding the provided strings as tags */
    public void tag(String message, String... tags);
    /** Decorates then sends the provided report */
    public void log(Builder report);
    /** Decorates then sends the provided report which came from an out-of-band source,
     * so the XTrace metadata for the current thread is not appended before sending */
    public void logOOB(Builder report);
  }
  
  /** If logging is turned off for an agent, then they're given the null logger which does nothing */
  static Logger NULL_LOGGER = new Logger() {
    public boolean valid() {
      return false;
    }
    public void log(String message, Object... labels) {
    }
    public void log(Builder report) {
    }
    public void logOOB(Builder report) {
    }
    public void tag(String message, String... tags) {
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
    public void log(String message, Object... labels) {
      REPORTER.report(agent, message, labels);
    }
    public void log(XTraceReport3.Builder report) {
      REPORTER.report(agent, report);
    }
    public void logOOB(XTraceReport3.Builder report) {
      REPORTER.reportNoXTrace(agent, report);
    }
    public void tag(String message, String... tags) {
      REPORTER.reportTagged(agent, message, tags);
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
      return getLogger();
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
  
  public static boolean isCausalityEnabled() {
    XTraceMetadataOrBuilder xmd = METADATA.observe();
    return xmd == null ? false : xmd.getParentEventIDCount() > 0;    
  }
  
  public static List<Long> getParentIDs() {
    XTraceMetadataOrBuilder xmd = METADATA.observe();
    return xmd == null ? null : xmd.getParentEventIDList();
  }

  /**
   * @return the tenant class currently being propagated by X-Trace in this
   *         thread, or -1 if none being propagated
   */
  public static int getTenantClass() {
    XTraceMetadataOrBuilder xmd = METADATA.observe();
    return xmd == null ? -1 : xmd.hasTenantClass() ? xmd.getTenantClass() : -1;
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
