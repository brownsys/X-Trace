package edu.brown.cs.systems.xtrace.server.impl.deprecated;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import org.apache.log4j.Logger;

import edu.brown.cs.systems.xtrace.Reporting.XTraceReport2;
import edu.brown.cs.systems.xtrace.server.api.Report;
import edu.brown.cs.systems.xtrace.server.impl.ServerReporting.ReportOnDisk;

/**
 * This is the String representation used for reports in old versions of XTrace
 * Not particularly efficient on the server side, but human readable
 * @author Jonathan Mace
 */
public class Report2 implements Report {
  
  static private final String HEADER = "X-Trace Report ver 1.0";
  
  private final XTraceReport2 proto; 

  private Report2(String taskId, String title, List<String> tags, String reportstr) {
    XTraceReport2.Builder builder = XTraceReport2.newBuilder();
    builder.setTaskid(taskId);
    builder.setReport(reportstr);
    if (title!=null)
      builder.setTitle(title);
    if (tags!=null && !tags.isEmpty())
      builder.addAllTag(tags);
    proto = builder.build();
  }
  
  public Report2(XTraceReport2 protobufRepr) {
    proto = protobufRepr;
  }

  @Override
  public String getTaskID() {
    return proto.getTaskid();
  }

  @Override
  public boolean hasTags() {
    return proto.getTagCount() > 0;
  }

  @Override
  public boolean hasTitle() {
    return proto.hasTitle();
  }

  @Override
  public List<String> getTags() {
    return proto.getTagList();
  }

  @Override
  public String getTitle() {
    return proto.getTitle();
  }
  
  @Override
  public String toString() {
    return proto.getReport();
  }

  @Override
  public ReportOnDisk diskRepr() {
    return ReportOnDisk.newBuilder().setV2Report(proto).build();
  }

  @Override
  public JSONObject jsonRepr() {
    JSONObject jsonObj = new JSONObject();
    jsonObj.put("version", HEADER);
    
    String[] lines = proto.getReport().split("\n");
    
    for (int i = 1; i < lines.length; i++) {
      String line = lines[i];
      int idx = line.indexOf(":");
      if (idx >= 0) {
        String key = line.substring(0, idx).trim();
        String value = line.substring(idx + 1, line.length()).trim();
        if (!jsonObj.containsKey(key))
          jsonObj.put(key, new JSONArray());
        ((JSONArray) jsonObj.get(key)).add(value);
      }
    }

    return jsonObj;
  }
  
  public static Report2 parse(String strrep) {
    return parse(Arrays.asList(strrep.split("\n")));
  }
  
  public static Report2 parse(List<String> lines) {
    if (lines.size() == 0)
      return null;
    
    String header = lines.get(0);
    if (!header.equals(HEADER))
      return null;
    
    String taskId = null;
    String title = null;
    List<String> tags = null;
    StringBuilder str = new StringBuilder(header);
    
    for (int i = 1; i < lines.size(); i++) {
      String line = lines.get(i);
      int idx = line.indexOf(":");
      if (idx >= 0) {
        String key = line.substring(0, idx).trim();
        String value = line.substring(idx + 1, line.length()).trim();
        if ("X-Trace".equals(key)) {
          taskId = value;
          XTraceMetadata xmd = XTraceMetadata.createFromString(value);
          if (xmd.isValid())
            taskId = xmd.getTaskId().toString();
        } else if ("Title".equals(key))
          title = value;
        else if ("Tag".equals(key)) {
          if (tags==null)
            tags = new ArrayList<String>();
          tags.add(value);
        }
      }
      str.append("\n");
      str.append(line);
    }
    
    if (taskId!=null)
      return new Report2(taskId, title, tags, str.toString());
    else
      return null;
  }

}