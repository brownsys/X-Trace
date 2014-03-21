package edu.brown.cs.systems.xtrace.server.impl;

import org.apache.log4j.Logger;

import edu.brown.cs.systems.pubsub.Subscriber;
import edu.brown.cs.systems.pubsub.Subscriber.Callback;
import edu.brown.cs.systems.xtrace.Reporting.XTraceReport3;
import edu.brown.cs.systems.xtrace.XTraceSettings;
import edu.brown.cs.systems.xtrace.server.api.DataStore;
import edu.brown.cs.systems.xtrace.server.api.MetadataStore;

public class PubSubSource extends Callback<XTraceReport3> {
  private static final Logger LOG = Logger.getLogger(PubSubSource.class);

  private final Subscriber subscriber;
  private final MetadataStore metadata;
  private final DataStore data;

  public PubSubSource(String serverHostname, int pubsubSubscribePort, DataStore data, MetadataStore metadata) {
    subscriber = new Subscriber(serverHostname, pubsubSubscribePort);
    subscriber.subscribe(XTraceSettings.PUBSUB_TOPIC, this);
    this.data = data;
    this.metadata = metadata;
  }

  public void shutdown() {
    subscriber.close();
    LOG.info("PubSub subscriber closed");
  }

  @Override
  protected void OnMessage(XTraceReport3 msg) {
    try {
      Report3 report = new Report3(msg);
      data.reportReceived(report);
      metadata.reportReceived(report);
    } catch (Exception e) {
      LOG.warn("PubSub exception receiving report\n" + msg, e);
    }
  }
}
