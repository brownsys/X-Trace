package edu.brown.cs.systems.xtrace;

import junit.framework.TestCase;

import org.junit.Test;

/**
 * Tests of the main XTrace API
 * @author a-jomace
 *
 */
public class XTraceConfigTest extends TestCase {
  
  @Test
  public void testXTraceConfig() {
    assertTrue(XTraceSettings.REPORTING_DISABLED.contains("randomDisabledAgentName"));
    assertFalse(XTraceSettings.REPORTING_DISABLED.contains("notdisabled"));
  }
  
  

}
