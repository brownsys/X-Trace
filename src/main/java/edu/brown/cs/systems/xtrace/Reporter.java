package edu.brown.cs.systems.xtrace;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Random;

import edu.brown.cs.systems.xtrace.Metadata.XTraceMetadataOrBuilder;
import edu.brown.cs.systems.xtrace.Reporting.XTraceReport3;
import edu.brown.cs.systems.xtrace.Reporting.XTraceReport3.Builder;

/**
 * The Reporter class is the base class for X-Trace reporting.
 * 
 * In X-Trace v3, it is separated from metadata propagation; one of the motivations of
 * X-Trace v3 is to be able to use the X-Trace metadata propagation for purposes other than sending reports
 * 
 * Reporter is an abstract class that can be extended to provide custom reporting implementations.
 * Out of the box, X-Trace v3 offers 0MQ Pub-sub logging only.
 * 
 * @author Jonathan Mace
 */
abstract class Reporter {
  
  /**
   * An interface to automatically add additional fields to XTrace reports when they're created.
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
  
  Reporter(Trace trace) {
    this.xtrace = trace;
  }
  
  protected Decorator decorator = null;
  
  public void setDecorator(Decorator decorator) {
    this.decorator = decorator;
  }
  
  protected Builder createReport(String agent, String label, Object... fields) {
    Builder builder = XTraceReport3.newBuilder();
    
    // Take a look at the current XTrace metadata
    XTraceMetadataOrBuilder metadata = xtrace.observe();
    builder.setTaskID(metadata.getTaskID());
    
    // Record the tenant class if necessary
    if (metadata.hasTenantClass())
      builder.setTenantClass(metadata.getTenantClass());
    
    // Record causality if necessary
    if(metadata.getParentEventIDCount()!=0) {
      builder.addAllParentEventID(metadata.getParentEventIDList());
      long neweventid = random.nextLong();
      builder.setEventID(neweventid);
      xtrace.modify().clearParentEventID().addParentEventID(neweventid);
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
   * @return true if we're currently able to send reports
   */
  public boolean valid() {
    XTraceMetadataOrBuilder metadata = xtrace.observe();
    return metadata!=null && metadata.hasTaskID();
  }
  
  public void sendReport(String agent, String label, Object... fields) {
    if (!valid())
      return;
    
    sendReport(createReport(agent, label, fields));
  }
  
  public void sendReport(Class<?> agent, String label, Object... fields) {
    if (!valid())
      return;
    
    sendReport(createReport(agent.toString(), label, fields));
  }
  
  protected abstract void sendReport(Builder report);

  protected abstract void close();
  
  private static class Utils {

    private static Class<?> MainClass;
    private static String ProcessName;
    private static Integer ProcessID;
    private static String Host;
    
    public static Class<?> getMainClass(){
      if (MainClass==null) {
        Collection<StackTraceElement[]> stacks = Thread.getAllStackTraces().values();
        for (StackTraceElement[] currStack : stacks) {
          if (currStack.length==0)
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
      if (ProcessName==null) {
        Class<?> mainClass = getMainClass();
        if (mainClass==null)
          return "";
        else
          ProcessName = mainClass.getSimpleName();
      }
      return ProcessName;
    }
    
    public static int getProcessID() {
      if (ProcessID==null) {
        String procname = ManagementFactory.getRuntimeMXBean().getName();
        ProcessID = Integer.parseInt(procname.substring(0, procname.indexOf('@')));
      }
      return ProcessID;
    }
    
    public static String getHost() {
      if (Host==null) {
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
