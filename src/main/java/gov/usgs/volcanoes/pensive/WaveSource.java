/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.pensive;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.pensive.plot.SubnetPlotter;
import gov.usgs.volcanoes.swarm.data.DataSourceType;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Retrieve data and produce plot.
 *
 * @author Tom Parker
 */
public class WaveSource implements Runnable {

  /** my logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger("gov.usgs");

  /** default wave server type. */
  public static final String DEFAULT_TYPE = "wws";
  
  /** default wave server. */
  public static final String DEFAULT_HOST = "localhost";
  
  /** default port. */
  public static final int DEFAULT_PORT = 16022;
  
  /** default timeout. */
  public static final int DEFAULT_TIMEOUT_S = 15;

  private boolean shouldRun;

  /** source of wave data. */
  private final SeismicDataSource dataSource;

  /** Jobs to be plotted. */
  private final BlockingQueue<PlotJob> plotJobs;

  /** my name. */
  public final String name;

  /**
   * Class constructor.
   *
   * @param name My name
   * @param plotJobs Queue containing jobs to plot
   * @param config My config stanza
   */
  public WaveSource(final String name, final BlockingQueue<PlotJob> plotJobs,
      final ConfigFile config) {
    shouldRun = true;
    this.plotJobs = plotJobs;
    this.name = name;

    final String type = config.getString("type", DEFAULT_TYPE);
    final String host = config.getString("host", DEFAULT_HOST);
    final int port = config.getInt("port", DEFAULT_PORT);
    final int timeout = config.getInt("timeout", DEFAULT_TIMEOUT_S);
    final int compress = 1;
    
    String dsString = null;
    if (type.equals("wws")) {
      dsString = String.format("%s;wws:%s:%d:%d:%d", name, host, port, timeout * 1000, compress);
      dataSource = DataSourceType.parseConfig(dsString);
    } else if (type.equals("wsv")) {
      dsString = String.format("%s;ws:%s:%d:%d:1800:1000:UTC", name, host, port, timeout * 1000);
      dataSource = DataSourceType.parseConfig(dsString);
    } else if (type.equals("fdsnws")) {
      dataSource = new FdsnwsSource(config);
    } else {
      throw new RuntimeException("Unknown wave server type");
    }
    dataSource.setUseCache(false);
  }

  /**
   * stop thread.
   */
  public void stop() {
    shouldRun = false;
  }

  /**
   * Take plot jobs and produce files.
   */
  public void run() {
    while (shouldRun || !plotJobs.isEmpty()) {
      PlotJob pj = null;
      try {
        pj = plotJobs.poll(2, TimeUnit.SECONDS);
        if (pj == null) {
          continue;
        }

        if (pj.plotTimeMs > System.currentTimeMillis()) {
          plotJobs.put(pj);
          Thread.sleep(1000);
          continue;
        }

        final SubnetPlotter subnet = pj.subnet;

        LOGGER.info("Plotting subnet {} from {} scheduled for {}", subnet.subnetName, name,
            new Date(pj.plotTimeMs));
        subnet.plot(pj.plotEndMs, dataSource);
      } catch (final InterruptedException noAction) {
        continue;
      }
    }
    LOGGER.debug("Exiting WaveSource thread ({}). Job queue: {}", name, plotJobs.size());
  }
}
