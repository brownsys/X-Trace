package edu.brown.cs.systems.xtrace.impl;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.brown.cs.systems.pubsub.Publisher;
import edu.brown.cs.systems.xtrace.Logger;
import edu.brown.cs.systems.xtrace.Reporting.XTraceReport3.Builder;
import edu.brown.cs.systems.xtrace.Settings;
import edu.brown.cs.systems.xtrace.Trace;

/**
 * The default implementation of X-Trace Logger using the 
 * edu.brown.cs.systems.pubsub package, which uses Zero MQ
 * @author Jonathan Mace
 *
 */
public class PubSubLogger extends Logger implements Runnable {
  
  /** Queue for outgoing reports.  For now, allow unbounded growth - the ZMQ handling
   * thread will never block on the socket (ZMQ handles that with the HWM setting) so 
   * the only way this queue can grow large is if the handler thread is descheduled for
   * large amounts of time */
  protected final BlockingQueue<Builder> outgoing = new LinkedBlockingQueue<Builder>();
  protected final Publisher publisher;
  protected volatile boolean alive = true;
  protected final Thread worker;
  
  /**
   * Creates a new log implementation, using the default pubsub server hostname and port
   * @param trace an xtrace metadata propagation
   */
  public PubSubLogger(Trace trace) {
    this(trace, Settings.SERVER_HOSTNAME, Settings.PUBSUB_PUBLISH_PORT);
  }
  
  /**
   * Creates a new log implementation, publishing to the specified hostname:port server
   * @param trace an x-trace metadata propagation
   * @param pubsub_server_hostname the hostname of the pubsub server
   * @param pubsub_server_port the port of the pubsub server to publish to
   */
  public PubSubLogger(Trace trace, String pubsub_server_hostname, int pubsub_server_port) {
    super(trace);
    publisher = new Publisher(pubsub_server_hostname, pubsub_server_port);
    worker = new Thread(this);
    worker.start();
  }
  
  /** Shuts down this logger and stops sending messages */
  public void close() {
    alive = false;
    worker.interrupt();
  }
  
  public boolean isAlive() {
    return alive;
  }

  @Override
  protected void logEvent(Builder event) {
    if (alive)
      outgoing.add(event);
  }

  @Override
  public void run() {
    // Run until we're done or interrupted
    while (alive && !Thread.currentThread().isInterrupted()) {
      try {
        publisher.publish(Settings.PUBSUB_TOPIC, outgoing.take().build());
      } catch (Exception e) {
        // Do nothing if interrupted, and allow graceful exit
        alive = false;
      }
    }
    
    // Clear the queue
    while(!outgoing.isEmpty())
      publisher.publish(Settings.PUBSUB_TOPIC, outgoing.poll().build());

    // Close the publisher
    publisher.close();
  }

}
