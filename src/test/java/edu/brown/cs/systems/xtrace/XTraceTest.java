package edu.brown.cs.systems.xtrace;

import junit.framework.TestCase;

import org.junit.Test;

/**
 * Tests of the main XTrace API
 * @author a-jomace
 *
 */
public class XTraceTest extends TestCase {
  
  @Test
  public void testBlanks() {
    // Everything should be blank initially
    nothing();
    
    // Join on null, everything should still be blank
    XTrace.join((byte[]) null);
    nothing();
    XTrace.join((Context) null);
    nothing();
    
    // Set null, everything should still be blank
    XTrace.set((byte[]) null);
    nothing();
    XTrace.set((Context) null);
    nothing();
    
    // Stop, everything should still be blank
    XTrace.stop();
    nothing();
  }
  
  @Test
  public void testTaskID() {
    // Everything should be blank initially    
    nothing();
    
    // Test just task ID
    XTrace.startTask(false);
    partial(true, true, false, false);
    long taskId = XTrace.getTaskID();
    
    XTrace.startTask(false);
    partial(true, true, false, false);
    assertEquals(taskId, (long) XTrace.getTaskID());
    
    XTrace.startTask(true);
    partial(true, true, false, false);
    assertEquals(taskId, (long) XTrace.getTaskID());
    
    XTrace.stop();
    nothing();
  }
  
  @Test
  public void testTenantID() {
    // Everything should be blank initially
    nothing();
    
    // Test just tenant ID
    XTrace.setTenantClass(1);
    partial(true, false, true, false);
    assertEquals(1, (int) XTrace.getTenantClass());
    
    XTrace.setTenantClass(2);
    partial(true, false, true, false);
    assertEquals(2, (int) XTrace.getTenantClass());
    
    XTrace.stop();
    nothing();
  }
  
  @Test
  public void testTaskIDandTenantID() {
    // Everything should be blank initially
    nothing();
    
    // Test task ID and tenant ID
    XTrace.startTask(false);
    XTrace.setTenantClass(1);
    partial(true, true, true, false);
    long taskId = XTrace.getTaskID();
    assertEquals(1, (int) XTrace.getTenantClass());
    
    XTrace.startTask(false);
    partial(true, true, true, false);
    assertEquals(taskId, (long) XTrace.getTaskID());
    assertEquals(1, (int) XTrace.getTenantClass());
    
    XTrace.setTenantClass(2);
    partial(true, true, true, false);
    assertEquals(taskId, (long) XTrace.getTaskID());
    assertEquals(2, (int) XTrace.getTenantClass());
    
    XTrace.stop();
    nothing();
  }
  
  @Test
  public void testTaskIDwithCausality() {
    // Everything should be blank initially    
    nothing();
    
    // Test just task ID
    XTrace.startTask(true);
    partial(true, true, false, true);
    long taskId = XTrace.getTaskID();
    
    XTrace.startTask(false);
    partial(true, true, false, true);
    assertEquals(taskId, (long) XTrace.getTaskID());
    
    XTrace.startTask(true);
    partial(true, true, false, true);
    assertEquals(taskId, (long) XTrace.getTaskID());
    
    XTrace.stop();
    nothing();
  }
  
  @Test
  public void testTaskIDwithCausalityandTenantID() {
    // Everything should be blank initially
    nothing();
    
    // Test task ID and tenant ID
    XTrace.startTask(true);
    XTrace.setTenantClass(1);
    partial(true, true, true, true);
    long taskId = XTrace.getTaskID();
    assertEquals(1, (int) XTrace.getTenantClass());
    
    XTrace.startTask(true);
    partial(true, true, true, true);
    assertEquals(taskId, (long) XTrace.getTaskID());
    assertEquals(1, (int) XTrace.getTenantClass());
    
    XTrace.setTenantClass(2);
    partial(true, true, true, true);
    assertEquals(taskId, (long) XTrace.getTaskID());
    assertEquals(2, (int) XTrace.getTenantClass());
    
    XTrace.stop();
    nothing();
  }
  
  private void nothing() {
    partial(false, false, false, false);
  }
  
  private void partial(boolean exists, boolean taskid, boolean tenantclass, boolean parents) {
    exists(exists);
    taskid(taskid);
    tenantclass(tenantclass);
    if (exists)
      parents(parents);
  }
  
  private void exists(boolean exists) {
    assertTrue(XTrace.active()==exists);
    assertTrue((XTrace.bytes()!=null)==exists);
    assertTrue((XTrace.get()!=null)==exists);
  }
  
  private void taskid(boolean exists) {
    assertTrue(XTrace.hasTaskID()==exists);
    assertTrue((XTrace.getTaskID()!=null)==exists);
  }
  
  private void tenantclass(boolean exists) {
    assertTrue(XTrace.hasTenantClass()==exists);
    assertTrue((XTrace.getTenantClass()!=-1)==exists);
  }
  
  private void parents(boolean exists) {
    if (exists) taskid(true);
    assertTrue((XTrace.get().observe().getParentEventIDCount()>0)==exists);
  }
  
  
  

}
