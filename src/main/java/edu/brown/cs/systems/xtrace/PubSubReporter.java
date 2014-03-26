package edu.brown.cs.systems.xtrace;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.brown.cs.systems.pubsub.Publisher;
import edu.brown.cs.systems.pubsub.Settings;
import edu.brown.cs.systems.xtrace.Reporting.XTraceReport3.Builder;

/**
 * The default implementation of X-Trace Logger using the
 * edu.brown.cs.systems.pubsub package, which uses Zero MQ
 * 
 * @author Jonathan Mace
 * 
 */
class PubSubReporter extends Reporter implements Runnable {

  /**
   * Queue for outgoing reports. For now, allow unbounded growth - the ZMQ
   * handling thread will never block on the socket (ZMQ handles that with the
   * HWM setting) so the only way this queue can grow large is if the handler
   * thread is descheduled for large amounts of time
   */
  protected final BlockingQueue<Builder> outgoing = new LinkedBlockingQueue<Builder>();
  protected volatile boolean alive = true;
  protected final Thread worker;
  private String hostname = null;
  private int port = 0;

  /**
   * Creates a new log implementation, using the default pubsub server hostname
   * and port
   * 
   * @param trace
   *          an xtrace metadata propagation
   */
  public PubSubReporter(Trace trace) {
    this(trace, null, 0);
  }

  /**
   * Creates a new log implementation, publishing to the specified hostname:port
   * server
   * 
   * @param trace
   *          an x-trace metadata propagation
   * @param pubsub_server_hostname
   *          the hostname of the pubsub server
   * @param pubsub_server_port
   *          the port of the pubsub server to publish to
   */
  public PubSubReporter(Trace trace, String hostname, int port) {
    super(trace);
    this.hostname = hostname;
    this.port = port;
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
  protected void doSend(Builder report) {
    if (alive)
      outgoing.add(report);
  }

  @Override
  public void run() {
    // Just run until we're done, interrupted, or get an exception
    if (hostname==null)
      hostname = Settings.SERVER_HOSTNAME;
    if (port==0)
      port = Settings.CLIENT_PUBLISH_PORT;
    Publisher publisher = new Publisher(hostname, port);
    try {
      while (alive && !Thread.currentThread().isInterrupted()) {
        publisher.publish(XTraceSettings.PUBSUB_TOPIC, outgoing.take().build());
      }
    } catch (Exception e) {
      alive = false;
    }

    // Clear the queue
    while (!outgoing.isEmpty())
      publisher.publish(XTraceSettings.PUBSUB_TOPIC, outgoing.poll().build());

    // Close the publisher
    publisher.close();

  }

}
