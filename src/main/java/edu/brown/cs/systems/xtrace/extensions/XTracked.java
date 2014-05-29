package edu.brown.cs.systems.xtrace.extensions;

import edu.brown.cs.systems.xtrace.Context;
import edu.brown.cs.systems.xtrace.XTrace;

public class XTracked {
  
  private Context __xtraced__xtrace_metadata = null;
  
  public void saveXTrace(Context ctx) {
    __xtraced__xtrace_metadata = ctx;
  }
  
  public void saveActiveXTrace() {
    saveXTrace(XTrace.get());
  }
  
  public Context getXTrace() {
    return __xtraced__xtrace_metadata;
  }
  
  public int getTenantClass() {
    return XTrace.getTenantClass(__xtraced__xtrace_metadata);
  }
  
  public void joinSavedXTrace() {
    XTrace.join(__xtraced__xtrace_metadata);
  }

}
