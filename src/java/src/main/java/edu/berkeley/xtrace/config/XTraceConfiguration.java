package edu.berkeley.xtrace.config;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import edu.berkeley.xtrace.config.XTraceConfigurationParser.InvalidConfigurationException;

/**
 * Stores configuration values that can be modified in xtrace.xml
 * 
 * xtrace.xml should be an XML file.
 * Example:
 * <Configuration>
 *  <XTrace>Enabled</XTrace>
 *  <Causality>Disabled</Causality>
 *  <Filters>
 *    <Default>Enabled</Default>
 *    <Enabled>org.apache.hadoop.ipc.RPC</Enabled>
 *    <Disabled>org.apache.hadoop.net.NetUtils</Disabled>
 *  </Filters>
 *  
 * </Configuration>
 * @author jon
 *
 */
public class XTraceConfiguration {
  
  /**
   * The currently active X-Trace configuration.  This field should be
   * treated as readonly; instead of setting this field directly, use
   * the setter method {@link XTraceConfiguration.set}.  To update
   * a configuration, update its values then call the set method again.
   */
  public static XTraceConfiguration active = new XTraceConfiguration();
  
  /**
   * Sets the active XTraceConfiguration to the configuration provided.
   * This method should be called to set the active configuration, rather
   * than setting the field directly. 
   * @param newconfiguration
   */
  public static void set(XTraceConfiguration newconfiguration) {
    XTraceConfiguration.active = newconfiguration;
    XTraceConfiguration.ENABLED = newconfiguration.enabled;
  }

  /** Turns X-Trace off.  All X-Trace API calls check that this is true. */
  public static boolean ENABLED = true;
  
  /**
   * This config's value for whether X-Trace is enabled.  This will be copied into
   * the static field {@link XTraceConfiguration.ENABLED} when set to being the
   * active configuration with {@link XTraceConfiguration.set}
   */
  private boolean enabled = true;
  
  /** Specifies whether X-Trace should be on or off.  This will be copied into
   * the static field {@link XTraceConfiguration.ENABLED} when set to being the
   * active configuration with {@link XTraceConfiguration.set}
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
  
  /**
   * The only config value that goes through a method call.  If this configuration
   * is the active configuration, returns {@XTraceConfiguration.ENABLED}, regardless
   * of whether its value has since been updated with {@XTraceConfiguration.setEnabled}
   * @return
   */
  public boolean isEnabled() {
    return this==active? XTraceConfiguration.ENABLED : this.enabled;
  }
  
  /** Contains information about the log levels that are on or off */
  public XTraceLogLevel loglevels = new XTraceLogLevel();
  
  /** Turns on or off causality.  Causality off is more efficient.  Default on. */
  public boolean causality = true;

  /** Turns on or off actual logging of events */
  public boolean logging = true;
  
  /**
   * Parses the X-Trace configuration in the specified file, and returns an XTraceConfiguration
   * object representing the configuration.  Does not modify the currently active configuration;
   * to do so, pass the result to {@link XTraceConfiguration.set()}.
   * @param f A file containing an X-Trace configuration in XML format
   * @return an XTraceConfiguration loaded from the specified file
   * @throws IOException 
   * @throws  
   */
  public static XTraceConfiguration load(File f) throws InvalidConfigurationException, IOException {
    return XTraceConfigurationParser.parse(f);
  }
  
  static {
    // Search for xtrace.xml on the classpath, try to load the first one that we find. 
    URL resource = Thread.currentThread().getContextClassLoader().getResource("xtrace.xml");
    if (resource!=null) {
      try {
          File f = new File(resource.getFile());
          XTraceConfiguration config = XTraceConfiguration.load(f);
          XTraceConfiguration.set(config);
          System.out.println("Successfully loaded X-Trace configuration xtrace.xml");
      } catch (IOException e) {
        System.err.println("Could not load xtrace.xml: " + e.getMessage());      
      } catch (InvalidConfigurationException e) {
        System.err.println("Could not parse xtrace.xml: " + e.getMessage());
      }
    } else {
      System.out.println("X-Trace could not find a configuration file to load.  Will use default configuration.");
    }
  }
  
}
