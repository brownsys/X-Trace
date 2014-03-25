package edu.brown.cs.systems.xtrace;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;

import org.junit.Test;

import edu.brown.cs.systems.xtrace.LoggerTest.NullLogger;
import edu.brown.cs.systems.xtrace.Metadata.XTraceMetadata;
import edu.brown.cs.systems.xtrace.Metadata.XTraceMetadata.Builder;

/**
 * Performance tests of X-Trace metadata
 * @author a-jomace
 *
 */
public class LoggerPerf extends TestCase {

  static final class DQLogger extends PubSubReporter {

    public DQLogger(Trace trace) {
      super(trace, "localhost", 9999);
    }
    
    @Override
    public void run() {
      while (alive && !Thread.currentThread().isInterrupted()) {
        try {
          outgoing.take();
        } catch (InterruptedException e) {
          alive = false;
        }
      }
    }
    
  }
  
  private static final DecimalFormat format = new DecimalFormat("#.##");
  private void printResults(double duration, double count, double cycles) {
    System.out.println("  Time:  " + format.format(duration/1000000000.0) + " seconds");
    System.out.println("  Count: " + count);
    System.out.println("  Avg:   " + format.format(duration/count) + " ns");
    System.out.println("  CPU:   " + format.format(cycles/count) + " cpu ns");
  }
  
  private static final ThreadMXBean tbean = ManagementFactory.getThreadMXBean();
  private static final Random random = new Random();
  
  private void doWork(final Reporter logger, int numthreads, final int iterations, final boolean taskid, final boolean tenantid, final boolean parentid) {

    final AtomicLong totalcount = new AtomicLong();
    final AtomicLong totalduration = new AtomicLong();
    final AtomicLong totalcpu = new AtomicLong();
    
    final CountDownLatch ready = new CountDownLatch(numthreads);
    final CountDownLatch go = new CountDownLatch(1);
    final CountDownLatch done = new CountDownLatch(numthreads);
    
    Runnable work = new Runnable() {
      @Override
      public void run() {
        if (taskid || tenantid) {
          Builder xmd = XTraceMetadata.newBuilder();
          if (taskid)
            xmd.setTaskID(random.nextLong());
          if (tenantid)
            xmd.setTenantClass(random.nextInt());
          if (taskid && parentid)
            xmd.addParentEventID(random.nextLong());
          logger.xtrace.set(xmd.build().toByteArray());
        }
        
        ready.countDown();
        try {
          go.await();
        } catch (Exception e) {
          e.printStackTrace();
        }
        
        long startcycles = tbean.getCurrentThreadCpuTime();
        long start = System.nanoTime();
        int i = 0;
        for (; i < iterations; i++) {
          logger.report("LoggerPerf", "Logging event");
        }
        long duration = System.nanoTime() - start;
        long cycles = tbean.getCurrentThreadCpuTime() - startcycles;
        totalcount.getAndAdd(i);
        totalduration.getAndAdd(duration);
        totalcpu.getAndAdd(cycles);
        
        done.countDown();
      }
    };
    
    for (int i = 0; i < numthreads; i++) {
      new Thread(work).start();
    }
    
    try {
      ready.await();
      go.countDown();
      done.await();
      printResults(totalduration.get(), totalcount.get(), totalcpu.get());
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    
  }
  
  @Test
  public void testReportGenerationSpeedNull() {
    System.out.println("CREATE NULL");
    Trace xtrace = new Trace();
    Reporter logger = new NullLogger(xtrace);
    doWork(logger, 1, 10000000, false, false, false);
  }
  
  @Test
  public void testReportGenerationSpeedTenantID() {
    System.out.println("CREATE TENANTID");
    Trace xtrace = new Trace();
    Reporter logger = new NullLogger(xtrace);
    doWork(logger, 1, 10000000, false, true, false);
  }
  
  @Test
  public void testReportGenerationSpeedTaskID() {
    System.out.println("CREATE TASKID");
    Trace xtrace = new Trace();
    Reporter logger = new NullLogger(xtrace);
    doWork(logger, 1, 10000000, true, false, false);
  }
  
  @Test
  public void testReportGenerationSpeedCausality() {
    System.out.println("CREATE TASKID+CAUSALITY");
    Trace xtrace = new Trace();
    Reporter logger = new NullLogger(xtrace);
    doWork(logger, 1, 10000000, true, false, true);
  }
  
  @Test
  public void testReportingSpeedNull() {
    System.out.println("CREATE+SEND NULL");
    Trace xtrace = new Trace();
    Reporter logger = new DQLogger(xtrace);
    doWork(logger, 1, 10000000, false, false, false);
    logger.close();
  }
  
  @Test
  public void testReportingSpeedTenantID() {
    System.out.println("CREATE+SEND TENANTID");
    Trace xtrace = new Trace();
    Reporter logger = new DQLogger(xtrace);
    doWork(logger, 1, 10000000, false, true, false);
    logger.close();
  }
  
  @Test
  public void testReportingSpeedTaskID() {
    System.out.println("CREATE+SEND TASKID");
    Trace xtrace = new Trace();
    Reporter logger = new DQLogger(xtrace);
    doWork(logger, 1, 10000000, true, false, false);
    logger.close();
  }
  
  @Test
  public void testReportingSpeedCausality() {
    System.out.println("CREATE+SEND TASKID+CAUSALITY");
    Trace xtrace = new Trace();
    Reporter logger = new DQLogger(xtrace);
    doWork(logger, 1, 10000000, true, false, true);
    logger.close();
  }
  
  @Test
  public void testReportingSpeedTenantID10() {
    System.out.println("CREATE+SENDx10 TENANTID");
    Trace xtrace = new Trace();
    Reporter logger = new DQLogger(xtrace);
    doWork(logger, 10, 1000000, false, true, false);
    logger.close();
  }
  
  @Test
  public void testReportingSpeedTaskID10() {
    System.out.println("CREATE+SENDx10 TASKID");
    Trace xtrace = new Trace();
    Reporter logger = new DQLogger(xtrace);
    doWork(logger, 10, 1000000, true, false, false);
    logger.close();
  }
  
  @Test
  public void testReportingSpeedCausality10() {
    System.out.println("CREATE+SENDx10 TASKID+CAUSALITY");
    Trace xtrace = new Trace();
    Reporter logger = new DQLogger(xtrace);
    doWork(logger, 10, 1000000, true, false, true);
    logger.close();
  }

}
