/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.pensive;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.pensive.plot.SubnetPlotter;
import gov.usgs.volcanoes.pensive.schedule.AbstractPlotScheduler;
import gov.usgs.volcanoes.pensive.schedule.BackfillPlotScheduler;
import gov.usgs.volcanoes.pensive.schedule.RealtimePlotScheduler;

import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * An application to produce a continuous collection of subnet spectrograms.
 *
 * @author Tom Parker
 */
public class Pensive {

  private static final boolean DEFAULT_WRITE_HTML = true;

  static {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  }

  /** my logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(Pensive.class);

  /** my configuration file. */
  private ConfigFile configFile;

  /** My SPA. */
  private PensiveWebApp webApp;

  /** One plot scheduler per wave server. */
  private Map<String, AbstractPlotScheduler> plotScheduler;

  /**
   * Class constructor.
   *
   * @param configFile
   *            my config file
   */
  public Pensive(final ConfigFile configFile) {

    this.configFile = configFile;
    final long now = System.currentTimeMillis();
    configFile.put("applicationLaunch", "" + now);
    LOGGER.info("Launching Pensive ({})", Version.VERSION_STRING);

    webApp = new PensiveWebApp(configFile);
    final boolean writeHtml = configFile.getBoolean("writeHtml", DEFAULT_WRITE_HTML);
    if (writeHtml) {
      webApp.writeHtml();
    }
  }

  /**
   * Class constructor used for back-filling.
   * 
   * @param configFile
   *            my config file
   * @param startTime
   *            time of first plot
   * @param endTime
   *            time of last plot. May be in the future.
   */
  public Pensive(final ConfigFile configFile, final Date startTime, final Date endTime) {
    super();
  }

  /**
   * Create one PlotScheduler per wave server, each running in its own thread.
   */
  private void createRealtimePlotSchedulers() {
    plotScheduler = new HashMap<String, AbstractPlotScheduler>();

    for (final String server : configFile.getList("waveSource")) {
      final ConfigFile c = configFile.getSubConfig(server, true);
      LOGGER.info("Creating plot scheduler for " + server);
      plotScheduler.put(server, new RealtimePlotScheduler(server, c));
    }
    assignSubnets();
    pruneSchedulers();
    schedulePlots();
    startWaveSources();
  }

  /**
   * Create one PlotScheduler per wave server, each running in its own thread.
   */
  private void createBackfillPlotSchedulers(final long startTime, final long endTime) {
    plotScheduler = new HashMap<String, AbstractPlotScheduler>();

    for (final String server : configFile.getList("waveSource")) {
      final ConfigFile c = configFile.getSubConfig(server, true);
      LOGGER.info("Creating plot scheduler for " + server);
      final BackfillPlotScheduler ps = new BackfillPlotScheduler(server, c);
      ps.setRange(startTime, endTime);
      plotScheduler.put(server, ps);
    }
    assignSubnets();
    pruneSchedulers();
    schedulePlots();
    startWaveSources();
  }

  private void startWaveSources() {
    for (final AbstractPlotScheduler ps : plotScheduler.values()) {
      ps.startWaveSources();
    }
  }

  /**
   * Assign subnets to a wave server.
   */
  private void assignSubnets() {
    final List<String> networks = configFile.getList("network");
    if (networks == null) {
      throw new RuntimeException("No network directives found.");
    }

    final Iterator<String> networkIt = networks.iterator();
    while (networkIt.hasNext()) {
      final String network = networkIt.next();
      final ConfigFile netConfig = configFile.getSubConfig(network, true);
      final List<String> subnets = netConfig.getList("subnet");
      if (subnets == null) {
        LOGGER.info("No subnet directives for network " + network + " found. Skipping.");
        networkIt.remove();
        continue;
      }

      final Iterator<String> subnetIt = subnets.iterator();
      while (subnetIt.hasNext()) {
        final String subnet = subnetIt.next();
        final ConfigFile subnetConfig = netConfig.getSubConfig(subnet, true);
        if (subnetConfig.getList("channel") == null) {
          LOGGER.warn("No channel directives for subnet " + subnet + " found. Skipping.");
          subnetIt.remove();
          continue;
        } else {
          webApp.addSubnet(network, subnet);
        }

        final String dataSource = subnetConfig.getString("dataSource");
        if (dataSource != null) {
          final AbstractPlotScheduler scheduler = plotScheduler.get(dataSource);
          LOGGER.info("Assigning subnet " + subnet + " to " + dataSource);
          scheduler.add(new SubnetPlotter(network, subnet, subnetConfig));
        } else {
          LOGGER.error("Cannot find dataSource for subnet {}. I'll skip it this time.", subnet);
        }
      }
      netConfig.putList("subnet", subnets);
    }
    configFile.putList("network", networks);

    final Iterator<String> schedulerIt = plotScheduler.keySet().iterator();
    while (schedulerIt.hasNext()) {
      final String server = schedulerIt.next();
      final AbstractPlotScheduler ps = plotScheduler.get(server);
      if (ps.subnetCount() < 1) {
        LOGGER.warn("No subnets feeding from " + ps.name + ". I'll prune it.");
        schedulerIt.remove();
      }
    }
  }

  /**
   * Prune PlotSchedulers that have no subnets assigned to them.
   */
  private void pruneSchedulers() {
    final Iterator<String> schedulerIt = plotScheduler.keySet().iterator();
    while (schedulerIt.hasNext()) {
      final String server = schedulerIt.next();
      final AbstractPlotScheduler ps = plotScheduler.get(server);
      if (ps.subnetCount() < 1) {
        LOGGER.warn("No subnets feeding from " + ps.name + ". I'll prune it.");
        schedulerIt.remove();
      }
    }
  }

  /**
   * Schedule a recurring call to produce plot jobs.
   */
  private void schedulePlots() {
    for (final AbstractPlotScheduler ps : plotScheduler.values()) {

      // schedule first plot immediately
      new Thread(ps).start();

      if (ps instanceof RealtimePlotScheduler) {
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        // satrt automated plots at the top of the next period
        int delay = SubnetPlotter.DURATION_S;
        delay -= (System.currentTimeMillis() / 1000) % SubnetPlotter.DURATION_S;
        LOGGER.debug("Scheduled plots start in " + delay + "ms");
        scheduler.scheduleAtFixedRate(ps, delay, SubnetPlotter.DURATION_S, TimeUnit.SECONDS);
      }
    }
  }

  /**
   * Write the webapp.
   */
  public void writeWebApp() {
    final boolean writeHtml = configFile.getBoolean("writeHtml", DEFAULT_WRITE_HTML);
    if (writeHtml) {
      webApp.writeHtml();
    }
  }

  /**
   * Stop plotting.
   */
  public void stop() {
    for (final AbstractPlotScheduler ps : plotScheduler.values()) {
      ps.stop();
    }
  }

  /**
   * Where it all begins.
   *
   * @param args
   *            command line args
   * @throws Exception
   *             when things go wrong
   */
  public static void main(final String[] args) throws Exception {
    final PensiveArgs config = new PensiveArgs(args);

    ConfigFile cf = null;
    cf = new ConfigFile(config.configFileName);
    if (!cf.wasSuccessfullyRead()) {
      LOGGER.error(
          "Couldn't find config file " + config.configFileName
              + ". Use '-c' to create an example config.");
      System.exit(1);
    }

    if (config.squelchHTML) {
      cf.put("writeHtml", "false", false);
    }

    final Pensive pensive = new Pensive(cf);
    if (config.startTime < 0) {
      pensive.createRealtimePlotSchedulers();
    } else {
      pensive.createBackfillPlotSchedulers(config.startTime, config.endTime);
      pensive.stop();
    }
    pensive.writeWebApp();

  }
}
