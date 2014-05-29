package edu.brown.cs.systems.xtrace.extensions;

import edu.brown.cs.systems.xtrace.Context;
import edu.brown.cs.systems.xtrace.XTrace;

public interface XTracked {

  public void saveXTrace(Context ctx);
  public void saveActiveXTrace();
  public Context getXTrace();
  public int getTenantClass();
  public void joinSavedXTrace();
  
  public class XTrackedImpl implements XTracked {
    
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

}
