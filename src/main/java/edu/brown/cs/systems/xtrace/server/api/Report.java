package edu.brown.cs.systems.xtrace.server.api;

import java.util.List;

import net.minidev.json.JSONObject;
import edu.brown.cs.systems.xtrace.server.impl.ServerReporting.ReportOnDisk;

/**
 * Interface for representing a report.
 * @author a-jomace
 *
 */
public interface Report {
  
  public String getTaskID();
  
  public boolean hasTags();
  
  public boolean hasTitle();
  
  public List<String> getTags();
  
  public String getTitle();
  
  public JSONObject jsonRepr();

  public ReportOnDisk diskRepr();
  
}
