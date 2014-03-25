package edu.brown.cs.systems.xtrace;

import java.util.HashSet;
import java.util.Set;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class XTraceSettings {

  public static final Config CONFIG = ConfigFactory.load();

  public static final String SERVER_HOSTNAME = CONFIG.getString("xtrace.server.hostname");
  public static final String SERVER_BIND_HOSTNAME = CONFIG.getString("xtrace.server.bind-hostname");

  public static final int WEBUI_PORT = CONFIG.getInt("xtrace.server.webui.port");
  public static final String WEBUI_SRC_DIR = CONFIG.getString("xtrace.server.webui.dir");

  public static final int TCP_PORT = CONFIG.getInt("xtrace.tcp.port");

  public static final int PUBSUB_PUBLISH_PORT = CONFIG.getInt("xtrace.pubsub.client-publish-port");
  public static final int PUBSUB_SUBSCRIBE_PORT = CONFIG.getInt("xtrace.pubsub.client-subscribe-port");
  public static final String PUBSUB_TOPIC = CONFIG.getString("xtrace.pubsub.topic");

  public static final int DATABASE_UPDATE_INTERVAL = CONFIG.getInt("xtrace.server.database-update-interval-ms");

  public static final String DATASTORE_DIRECTORY = CONFIG.getString("xtrace.server.datastore.dir");
  public static final int DATASTORE_BUFFER_SIZE = CONFIG.getInt("xtrace.server.datastore.buffer-size");
  public static final int DATASTORE_CACHE_SIZE = CONFIG.getInt("xtrace.server.datastore.cache-size");
  public static final int DATASTORE_CACHE_TIMEOUT = CONFIG.getInt("xtrace.server.datastore.cache-timeout");
  
  public static final boolean REPORTING_ENABLED_DEFAULT = CONFIG.getBoolean("xtrace.client.reporting.default");
  public static final Set<String> REPORTING_ENABLED = new HashSet<String>(CONFIG.getStringList("xtrace.client.reporting.enabled"));
  public static final Set<String> REPORTING_DISABLED = new HashSet<String>(CONFIG.getStringList("xtrace.client.reporting.disabled"));
  

}
