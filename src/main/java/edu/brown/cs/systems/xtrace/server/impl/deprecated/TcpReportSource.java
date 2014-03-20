package edu.brown.cs.systems.xtrace.server.impl.deprecated;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import edu.brown.cs.systems.xtrace.server.api.DataStore;
import edu.brown.cs.systems.xtrace.server.api.MetadataStore;
import edu.brown.cs.systems.xtrace.server.api.Report;

/**
 * String based TCP report server.
 * 
 * It does a lot of string comprehension :( Let's not do this again...
 * 
 * @author Matei Zaharia
 * @author George Porter
 * @author Jonathan Mace
 */
@Deprecated
public class TcpReportSource extends Thread {
  private static final Logger LOG = Logger.getLogger(TcpReportSource.class);
  private static final int MAX_REPORT_LENGTH = 256 * 1024;

  private volatile boolean alive = true;
  private final int tcpport;
  private final ServerSocket serversock;
  private final DataStore data;
  private final MetadataStore metadata;
  private final ReportHandler handler;

  private BlockingQueue<String> q = new LinkedBlockingQueue<String>();

  public TcpReportSource(int port, DataStore data, MetadataStore metadata) throws IOException {
    this.tcpport = port;
    this.serversock = new ServerSocket(port);
    this.data = data;
    this.metadata = metadata;
    this.handler = new ReportHandler();
  }

  public void shutdown() {
    try {
      if (alive) {
        alive = false;
        serversock.close();
        LOG.info("TcpReportSource successfully shut down");
      }
    } catch (IOException e) {
      LOG.warn("IOException closing TcpReportSource server socket", e);
    }
  }

  @Override
  public void run() {
    this.handler.start();
    LOG.info("TcpReportSource listening on port " + tcpport);
    try {
      while (alive && !Thread.currentThread().isInterrupted()) {
        Socket sock = serversock.accept();
        new TcpClientHandler(sock).start();
      }
    } catch (IOException e) {
      if (alive)
        LOG.warn("IOException while accepting a TCP client", e);
    } finally {
      shutdown();
    }
  }

  private class TcpClientHandler extends Thread {

    private Socket sock;

    public TcpClientHandler(Socket sock) {
      this.sock = sock;
    }

    @Override
    public void run() {
      LOG.info("Starting TcpClientHandler for " + sock.getInetAddress() + ":" + sock.getPort());
      byte[] buf = new byte[MAX_REPORT_LENGTH];
      try {
        DataInputStream in = new DataInputStream(sock.getInputStream());
        while (alive && !Thread.currentThread().isInterrupted()) {
          int length = in.readInt();
          if (length <= 0 || length > MAX_REPORT_LENGTH) {
            LOG.info("Closing ReadReportsThread for " + sock.getInetAddress() + ":" + sock.getPort() + " due to bad length: " + length);
            sock.close();
            return;
          }
          in.readFully(buf, 0, length);
          String message = new String(buf, 0, length, "UTF-8");
          q.offer(message);
        }
        sock.close();
        LOG.info("Closing ReadReportsThread for " + sock.getInetAddress() + ":" + sock.getPort());
      } catch (EOFException e) {
        LOG.info("Closing ReadReportsThread for " + sock.getInetAddress() + ":" + sock.getPort() + " normally (EOF)");
      } catch (Exception e) {
        LOG.warn("Closing ReadReportsThread for " + sock.getInetAddress() + ":" + sock.getPort(), e);
      }
    }
  }

  private class ReportHandler extends Thread {
    @Override
    public void run() {
      while (alive && !Thread.currentThread().isInterrupted()) {
        try {
          String next = q.take();
          try {
            Report report = Report2.parse(next);

            if (report == null)
              LOG.warn("TcpReportSource received bad report, ignoring: " + next);

            data.reportReceived(report);
            metadata.reportReceived(report);

          } catch (Exception e) {
            LOG.warn("TcpReportSource ReportHandler Exception processing report", e);
          }
        } catch (InterruptedException e) {
        }
      }
    }
  }
}
