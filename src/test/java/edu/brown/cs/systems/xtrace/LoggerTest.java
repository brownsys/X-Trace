package edu.brown.cs.systems.xtrace;

import junit.framework.TestCase;

import org.junit.Test;

import edu.brown.cs.systems.xtrace.Reporting.XTraceReport3.Builder;

public class LoggerTest extends TestCase {
  
  static final class NullLogger extends Logger {

    public Builder event = null;
    
    public NullLogger(Trace trace) {
      super(trace);
    }
    
    @Override
    protected void logEvent(Builder event) {
      this.event = event;
    }

    @Override
    protected void close() {
    }
    
  }
  
  @Test
  public void testLog() {
    Trace xtrace = new Trace();
    NullLogger logger = new NullLogger(xtrace);
    
    byte[] start = TraceImplTest.randomTaskIDAndTenantAndParents(1);
    xtrace.set(start);
    edu.brown.cs.systems.xtrace.Metadata.XTraceMetadata.Builder metadata = xtrace.get().builder;
    
    logger.logEvent("test", "my test");
    Builder event = logger.event;
    assertNotNull(event);
    assertEquals("test", event.getAgent());
    assertEquals("my test", event.getLabel());
    assertEquals(metadata.getTaskID(), event.getTaskID());
    assertEquals(metadata.getParentEventID(0), event.getParentEventID(0));
    assertEquals(xtrace.peek().builder.getParentEventID(0), event.getEventID());
    assertFalse(metadata==xtrace.peek().builder);
  }
  
  @Test
  public void testLogOnlyTaskID() {
    Trace xtrace = new Trace();
    NullLogger logger = new NullLogger(xtrace);
    
    byte[] start = TraceImplTest.randomTaskIDAndTenant();
    xtrace.set(start);
    edu.brown.cs.systems.xtrace.Metadata.XTraceMetadata.Builder metadata = xtrace.get().builder;
    
    logger.logEvent("test", "my test");
    Builder event = logger.event;
    assertNotNull(event);
    assertEquals("test", event.getAgent());
    assertEquals("my test", event.getLabel());
    assertEquals(metadata.getTaskID(), event.getTaskID());
    assertEquals(0, event.getParentEventIDCount());
    assertFalse(event.hasEventID());
    assertTrue(metadata==xtrace.peek().builder);
  }
  
  @Test
  public void testLogNoTaskID() {
    Trace xtrace = new Trace();
    NullLogger logger = new NullLogger(xtrace);
    
    byte[] start = TraceImplTest.randomTenantID();
    xtrace.set(start);
    Context ctx = xtrace.get();
    
    logger.logEvent("test", "my test");
    Builder event = logger.event;
    assertNull(event);
    assertTrue(xtrace.peek()==ctx);
  }

}
