package edu.brown.cs.systems.xtrace;

import junit.framework.TestCase;

import org.junit.Test;

import edu.brown.cs.systems.xtrace.Metadata.XTraceMetadataOrBuilder;
import edu.brown.cs.systems.xtrace.Reporting.XTraceReport3.Builder;

public class LoggerTest extends TestCase {
  
  static final class NullLogger extends Reporter {

    public Builder report = null;
    
    public NullLogger(Trace trace) {
      super(trace);
    }
    
    @Override
    protected void doSend(Builder report) {
      this.report = report;
    }

    @Override
    protected void close() {
    }
    
  }
  
  @Test
  public void testLogWithUpdate() {
    Trace xtrace = new Trace();
    NullLogger logger = new NullLogger(xtrace);
    
    byte[] start = TraceImplTest.randomTaskIDAndTenantAndParents(1);
    xtrace.set(start);
    XTraceMetadataOrBuilder metadata = xtrace.observe();
    xtrace.get(); // force update
    
    logger.report("test", "my test");
    Builder event = logger.report;
    assertNotNull(event);
    assertEquals("test", event.getAgent());
    assertEquals("my test", event.getLabel());
    assertEquals(metadata.getTaskID(), event.getTaskID());
    assertEquals(metadata.getParentEventID(0), event.getParentEventID(0));
    assertEquals(xtrace.observe().getParentEventID(0), event.getEventID());
    assertFalse(xtrace.observe().getParentEventID(0)==metadata.getParentEventID(0));
    assertFalse(metadata==xtrace.observe());
  }
  
  @Test
  public void testLogWithoutUpdate() {
    Trace xtrace = new Trace();
    NullLogger logger = new NullLogger(xtrace);
    
    byte[] start = TraceImplTest.randomTaskIDAndTenantAndParents(1);
    xtrace.set(start);
    XTraceMetadataOrBuilder metadata = xtrace.observe();
    
    logger.report("test", "my test");
    Builder event = logger.report;
    assertNotNull(event);
    assertEquals("test", event.getAgent());
    assertEquals("my test", event.getLabel());
    assertEquals(metadata.getTaskID(), event.getTaskID());
    assertFalse(metadata.getParentEventID(0)==event.getParentEventID(0)); // should have been reused and updated
    assertEquals(xtrace.observe().getParentEventID(0), event.getEventID()); // should have been reused
    assertTrue(xtrace.observe().getParentEventID(0)==metadata.getParentEventID(0));
    assertTrue(metadata==xtrace.observe());
  }
  
  @Test
  public void testLogOnlyTaskID() {
    Trace xtrace = new Trace();
    NullLogger logger = new NullLogger(xtrace);
    
    byte[] start = TraceImplTest.randomTaskIDAndTenant();
    xtrace.set(start);
    XTraceMetadataOrBuilder metadata = xtrace.observe();
    
    logger.report("test", "my test");
    Builder event = logger.report;
    assertNotNull(event);
    assertEquals("test", event.getAgent());
    assertEquals("my test", event.getLabel());
    assertEquals(metadata.getTaskID(), event.getTaskID());
    assertEquals(0, event.getParentEventIDCount());
    assertFalse(event.hasEventID());
    assertTrue(metadata==xtrace.observe());
  }
  
  @Test
  public void testLogOnlyTaskIDAndInvalidate() {
    Trace xtrace = new Trace();
    NullLogger logger = new NullLogger(xtrace);
    
    byte[] start = TraceImplTest.randomTaskIDAndTenant();
    xtrace.set(start);
    XTraceMetadataOrBuilder metadata = xtrace.observe();
    xtrace.get();
    
    logger.report("test", "my test");
    Builder event = logger.report;
    assertNotNull(event);
    assertEquals("test", event.getAgent());
    assertEquals("my test", event.getLabel());
    assertEquals(metadata.getTaskID(), event.getTaskID());
    assertEquals(0, event.getParentEventIDCount());
    assertFalse(event.hasEventID());
    assertTrue(metadata==xtrace.observe());
  }
  
  @Test
  public void testLogNoTaskID() {
    Trace xtrace = new Trace();
    NullLogger logger = new NullLogger(xtrace);
    
    byte[] start = TraceImplTest.randomTenantID();
    xtrace.set(start);
    Context ctx = xtrace.get();
    
    logger.report("test", "my test");
    Builder event = logger.report;
    assertNull(event);
    assertTrue(xtrace.get()==ctx);
  }

}
