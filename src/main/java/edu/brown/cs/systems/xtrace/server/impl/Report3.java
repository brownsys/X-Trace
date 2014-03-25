package edu.brown.cs.systems.xtrace.server.impl;

import java.util.List;

import net.minidev.json.JSONArray;
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
    JSONObject json = new JSONObject();
    
    if (event.hasTaskID())
      json.put("taskID", this.taskID);
    if (event.hasTimestamp())
      json.put("Timestamp", event.getTimestamp());
    if (event.hasHRT())
      json.put("HRT", event.getHRT());
    if (event.hasCycles())
      json.put("Cycles", event.getCycles());
    if (event.hasHost())
      json.put("Host", event.getHost());
    if (event.hasProcessID())
      json.put("ProcessID", event.getProcessID());
    if (event.hasProcessName())
      json.put("ProcessName", event.getProcessName());
    if (event.hasThreadID())
      json.put("ThreadID", event.getThreadID());
    if (event.hasThreadName())
      json.put("ThreadName", event.getThreadName());
    if (event.hasAgent())
      json.put("Agent", event.getAgent());
    if (event.hasSource())
      json.put("Source", event.getSource());
    if (event.hasLabel())
      json.put("Label", event.getLabel());
    for (int i = 0; i < event.getKeyCount(); i++) {
      json.put(event.getKey(i), event.getValue(i));
    }
    if (event.getTagsCount()>0) {
      JSONArray tags = new JSONArray();
      tags.addAll(event.getTagsList());
      json.put("Tag", tags);
    }
    if (event.hasTitle())
      json.put("Title", event.getTitle());
    if (event.hasTenantClass())
      json.put("TenantClass", event.getTenantClass());
    if (event.hasEventID())
      json.put("EventID", Long.toString(event.getEventID()));
    if (event.getParentEventIDCount() > 0) {
      JSONArray parents = new JSONArray();
      for (int i = 0; i < event.getParentEventIDCount(); i++) {
        parents.add(Long.toString(event.getParentEventID(i)));
      }
      json.put("ParentEventID", parents);
    }
    if (event.hasOp())
      json.put("Operation", event.getOp());

    return json;
  }
  
}