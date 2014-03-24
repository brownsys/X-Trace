package edu.brown.cs.systems.xtrace;

import edu.brown.cs.systems.xtrace.Reporter.Decorator;

/**
 * Provides some additional API calls to customize X-Trace from the client side
 * 
 * @author Jonathan Mace
 */
public class XTraceExtensions {

  public static void setReportDecorator(Decorator decorator) {
    XTrace.REPORTER.setDecorator(decorator);
  }

}
