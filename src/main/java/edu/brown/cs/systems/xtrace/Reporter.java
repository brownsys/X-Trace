package edu.brown.cs.systems.xtrace;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import edu.brown.cs.systems.xtrace.Metadata.XTraceMetadataOrBuilder;
import edu.brown.cs.systems.xtrace.Reporting.XTraceReport3;
import edu.brown.cs.systems.xtrace.Reporting.XTraceReport3.Builder;

/**
 * The Reporter class is the base class for X-Trace reporting.
 * 
 * In X-Trace v3, it is separated from metadata propagation; one of the
 * motivations of X-Trace v3 is to be able to use the X-Trace metadata
 * propagation for purposes other than sending reports
 * 
 * Reporter is an abstract class that can be extended to provide custom
 * reporting implementations. Out of the box, X-Trace v3 offers 0MQ Pub-sub
 * logging only.
 * 
 * @author Jonathan Mace
 */
public abstract class Reporter {

  /**
   * An interface to automatically add additional fields to XTrace reports when
   * they're created. Register decorators using the setDecorator method. For
   * now, only one decorator may be registered
   * 
   * @author Jonathan Mace
   */
  public static interface Decorator {
    public Builder decorate(Builder builder);
  }

  protected static final Random random = new Random(31 * (17 * Utils.getHost().hashCode() + Utils.getProcessID()) * System.currentTimeMillis());

  protected final Trace xtrace;
  protected static final String host = Utils.getHost();
  protected static final int procid = Utils.getProcessID();

  Reporter(Trace trace) {
    this.xtrace = trace;
  }

  protected Decorator decorator = null;

  public void setDecorator(Decorator decorator) {
    this.decorator = decorator;
  }

  /**
   * Creates a new report builder. X-Trace metadata fields are only ever added
   * at report send time, by the sendReport method
   * 
   * @return a new Builder for an XTraceReport3 with some fields such as
   *         timestamp, host, processid etc. filled in.
   */
  public static Builder createReport() {
    Builder builder = XTraceReport3.newBuilder();
    builder.setHost(host);
    builder.setProcessID(procid);
    builder.setProcessName(Utils.getProcessName());
    builder.setThreadID((int) Thread.currentThread().getId());
    builder.setThreadName(Thread.currentThread().getName());
    builder.setTimestamp(System.currentTimeMillis());
    builder.setHRT(System.nanoTime());
    return builder;
  }

  /**
   * Creates a new report builder. X-Trace metadata fields are only ever added
   * at report send time, by the sendReport method
   * 
   * @return a new Builder for an XTraceReport3 with the fields of createReport
   *         added and also the provided agent, label and user-defined fields
   */
  public static Builder createReport(String label, Object... fields) {
    Builder builder = createReport();
    builder.setLabel(label);
    for (int i = 0; i < fields.length - 1; i += 2) {
      // Key cannot be null, but value can
      if (fields[i] != null) {
        builder.addKey(fields[i].toString());
        builder.addValue(fields[i + 1] == null ? "null" : fields[i + 1].toString());
      }
    }
    return builder;
  }

  /**
   * @return true if we're currently able to send reports
   */
  public boolean valid() {
    XTraceMetadataOrBuilder metadata = xtrace.observe();
    return metadata != null && metadata.hasTaskID();
  }

  /**
   * Send an X-Trace report. This method call will do nothing if the current
   * X-Trace metadata is invalid
   * 
   * @param agent
   *          An agent to log this report against
   * @param label
   *          The message of the report
   * @param fields
   *          Additional values to include in the report. toString will be
   *          called on each of these
   */
  public void report(String agent, String label, Object... fields) {
    if (!valid())
      return;

    sendReport(agent, createReport(label, fields), true);
  }

  /**
   * Sends the provided report, attaching values for the current X-Trace
   * metadata to the report This method does not perform any checks to ensure
   * that reporting is allowed. Clients utilizing this method (over the agent,
   * label implementations) must perform their own checking.
   * 
   * @param report
   *          The report to finalize and then send
   */
  public void report(String agent, Builder report) {
    sendReport(agent, report, true);
  }

  /**
   * Sends the provided builder without attaching any values for the current
   * X-Trace metadata This method does not perform any checks to ensure that
   * reporting is allowed. Clients utilizing this method (over the agent, label
   * implementations) must perform their own checking.
   * 
   * @param report
   *          The report to send
   */
  public void reportNoXTrace(String agent, Builder report) {
    sendReport(agent, report, false);
  }

  /**
   * Sends an X-Trace report. This method call will do nothing if the current
   * X-Trace metadata is invalid
   * 
   * @param agent
   *          An agent to log this report against
   * @param label
   *          The message of the report
   * @param tags
   *          Strings to tag this report with, that will be used as database
   *          keywords
   */
  public void reportTagged(String agent, String label, String... tags) {
    if (!valid())
      return;

    sendReport(agent, createReport(label).addAllTags(Arrays.asList(tags)), true);
  }

  /**
   * Called before a report is about to be sent. The last opportunity to add
   * fields to the report. Here is where we add the XTrace metadata if desired
   */
  protected void sendReport(String agent, Builder builder, boolean includeXTrace) {
    // Set the agent
    builder.setAgent(agent);

    // Apply the user-defined decorator
    if (decorator != null)
      decorator.decorate(builder);

    // Add XTrace metadata if desired
    if (includeXTrace) {
      XTraceMetadataOrBuilder metadata = xtrace.observe();
      if (metadata != null) {
        builder.setTaskID(metadata.getTaskID());

        // Record the tenant class if necessary
        if (metadata.hasTenantClass())
          builder.setTenantClass(metadata.getTenantClass());

        // Record causality if necessary
        if (metadata.getParentEventIDCount() != 0) {
          builder.addAllParentEventID(metadata.getParentEventIDList());
          long neweventid = random.nextLong();
          builder.setEventID(neweventid);
          xtrace.modify().clearParentEventID().addParentEventID(neweventid);
        }
      }
    }

    doSend(builder);
  }

  /**
   * Actual method for subclasses to implement to do the sending of a report
   * 
   * @param report
   *          The report to send
   */
  protected abstract void doSend(Builder report);

  protected abstract void close();

  public static class Utils {

    private static Class<?> MainClass;
    private static String ProcessName;
    private static Integer ProcessID;
    private static String Host;

    public static Class<?> getMainClass() {
      if (MainClass == null) {
        Collection<StackTraceElement[]> stacks = Thread.getAllStackTraces().values();
        for (StackTraceElement[] currStack : stacks) {
          if (currStack.length == 0)
            continue;
          StackTraceElement lastElem = currStack[currStack.length - 1];
          if (lastElem.getMethodName().equals("main")) {
            try {
              String mainClassName = lastElem.getClassName();
              MainClass = Class.forName(mainClassName);
            } catch (ClassNotFoundException e) {
              // bad class name in line containing main?!
              // shouldn't happen
              e.printStackTrace();
            }
          }
        }
      }
      return MainClass;
    }

    public static String getProcessName() {
      if (ProcessName == null) {
        Class<?> mainClass = getMainClass();
        if (mainClass == null)
          return "";
        else
          ProcessName = mainClass.getSimpleName();
      }
      return ProcessName;
    }

    public static int getProcessID() {
      if (ProcessID == null) {
        String procname = ManagementFactory.getRuntimeMXBean().getName();
        ProcessID = Integer.parseInt(procname.substring(0, procname.indexOf('@')));
      }
      return ProcessID;
    }

    public static String getHost() {
      if (Host == null) {
        try {
          Host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
          Host = "unknown";
        }
      }
      return Host;
    }
  }
}
