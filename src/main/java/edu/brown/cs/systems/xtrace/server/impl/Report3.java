package edu.brown.cs.systems.xtrace.server.impl;

import java.util.List;

import net.minidev.json.JSONObject;
import edu.brown.cs.systems.xtrace.Reporting.XTraceReport3;
import edu.brown.cs.systems.xtrace.server.api.Report;
import edu.brown.cs.systems.xtrace.server.impl.ServerReporting.ReportOnDisk;

/**
 * X-Trace version 3 representation of X-Trace reports.  Directly
 * serializes protocol buffers messages to disk.  Not human readable
 * but substantially more efficient
 * @author Jonathan Mace
 */
public class Report3 implements Report {

  static private final String HEADER = "X-Trace Report ver 3.0";
  
  private final String taskID;
  private final XTraceReport3 event;
  
  public Report3(XTraceReport3 event) {
    this.event = event;
    this.taskID = String.format("%16s", Long.toHexString(event.getTaskID())).replace(' ', '0');
  }

  @Override
  public String getTaskID() {
    return taskID;
  }

  @Override
  public boolean hasTags() {
    return event.getTagsCount()>0;
  }

  @Override
  public boolean hasTitle() {
    return event.hasTitle();
  }

  @Override
  public List<String> getTags() {
    return event.getTagsList();
  }

  @Override
  public String getTitle() {
    return event.getTitle();
  }
  
  @Override
  public String toString() {
    return event.toString();
  }
  
  @Override
  public ReportOnDisk diskRepr() {
    return ReportOnDisk.newBuilder().setV3Report(event).build();
  }

  @Override
  public JSONObject jsonRepr() {
    JSONObject jsonObj = new JSONObject();
    jsonObj.put("version", HEADER);
    
    // TODO
    throw new UnsupportedOperationException();

  }
  
}