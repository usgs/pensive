package gov.usgs.volcanoes.pensive;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.martiansoftware.jsap.JSAPException;

import gov.usgs.volcanoes.util.configFile.ConfigFile;
import gov.usgs.volcanoes.pensive.plot.SubnetPlotter;
import gov.usgs.volcanoes.pensive.schedule.AbstractPlotScheduler;
import gov.usgs.volcanoes.pensive.schedule.BackfillPlotScheduler;
import gov.usgs.volcanoes.pensive.schedule.RealtimePlotScheduler;

/**
 * An application to produce a continuous collection of subnet spectrograms.
 * 
 * @author Tom Parker
 * 
 *         I waive copyright and related rights in the this work worldwide
 *         through the CC0 1.0 Universal public domain dedication.
 *         https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */
public class Pensive {

    public static final boolean DEFAULT_WRITE_HTML = true;

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    /** my logger */
    private static final Logger LOGGER = LoggerFactory.getLogger(Pensive.class);

    /** my configuration file */
    private ConfigFile configFile;

    /** My SPA */
    private PensiveWebApp webApp;

    /** One plot scheduler per wave server */
    private Map<String, AbstractPlotScheduler> plotScheduler;

    /**
     * Class constructor
     * 
     * @param configFile
     *            my config file
     */
    public Pensive(ConfigFile configFile) {

        this.configFile = configFile;
        long now = System.currentTimeMillis();
        configFile.put("applicationLaunch", "" + now);
        LOGGER.info("Launching Pensive ({})", PensiveVersion.VERSION_STRING);

        webApp = new PensiveWebApp(configFile);
        boolean writeHtml = configFile.getBoolean("writeHtml", DEFAULT_WRITE_HTML);
        if (writeHtml)
            webApp.writeHTML();
    }

    public Pensive(ConfigFile configFile, Date startTime, Date endTime) {
        super();
    }

    /**
     * Create one PlotScheduler per wave server, each running in its own thread.
     */
    private void createRealtimePlotSchedulers() {
        plotScheduler = new HashMap<String, AbstractPlotScheduler>();

        for (String server : configFile.getList("waveSource")) {
            ConfigFile c = configFile.getSubConfig(server, true);
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
    private void createBackfillPlotSchedulers(long startTime, long endTime) {
        plotScheduler = new HashMap<String, AbstractPlotScheduler>();

        for (String server : configFile.getList("waveSource")) {
            ConfigFile c = configFile.getSubConfig(server, true);
            LOGGER.info("Creating plot scheduler for " + server);
            BackfillPlotScheduler ps = new BackfillPlotScheduler(server, c);
            ps.setRange(startTime, endTime);
            plotScheduler.put(server, ps);
        }
        assignSubnets();
        pruneSchedulers();
        schedulePlots();
        startWaveSources();
    }

    private void startWaveSources() {
        for (AbstractPlotScheduler ps : plotScheduler.values())
            ps.startWaveSources();
    }

    /**
     * Assign subnets to it wave server
     */
    private void assignSubnets() {
        List<String> networks = configFile.getList("network");
        if (networks == null)
            throw new RuntimeException("No network directives found.");

        Iterator<String> networkIt = networks.iterator();
        while (networkIt.hasNext()) {
            String network = networkIt.next();
            ConfigFile netConfig = configFile.getSubConfig(network, true);
            List<String> subnets = netConfig.getList("subnet");
            if (subnets == null) {
                LOGGER.info("No subnet directives for network " + network + " found. Skipping.");
                networkIt.remove();
                continue;
            }

            Iterator<String> subnetIt = subnets.iterator();
            while (subnetIt.hasNext()) {
                String subnet = subnetIt.next();
                ConfigFile subnetConfig = netConfig.getSubConfig(subnet, true);
                if (subnetConfig.getList("channel") == null) {
                    LOGGER.warn("No channel directives for subnet " + subnet + " found. Skipping.");
                    subnetIt.remove();
                    continue;
                } else {
                    webApp.addSubnet(network, subnet);
                }

                String dataSource = subnetConfig.getString("dataSource");
                AbstractPlotScheduler scheduler = plotScheduler.get(dataSource);
                LOGGER.info("Assigning subnet " + subnet + " to " + dataSource);
                scheduler.add(new SubnetPlotter(network, subnet, subnetConfig));
            }
            netConfig.putList("subnet", subnets);
        }
        configFile.putList("network", networks);

        Iterator<String> schedulerIt = plotScheduler.keySet().iterator();
        while (schedulerIt.hasNext()) {
            String server = schedulerIt.next();
            AbstractPlotScheduler ps = plotScheduler.get(server);
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
        Iterator<String> schedulerIt = plotScheduler.keySet().iterator();
        while (schedulerIt.hasNext()) {
            String server = schedulerIt.next();
            AbstractPlotScheduler ps = plotScheduler.get(server);
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
        for (AbstractPlotScheduler ps : plotScheduler.values()) {

            // schedule first plot immediately
            new Thread(ps).start();

            if (ps instanceof RealtimePlotScheduler) {
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                // satrt automated plots at the top of the next period
                int delay = SubnetPlotter.DURATION_S;
                delay -= (System.currentTimeMillis() / 1000) % SubnetPlotter.DURATION_S;
                LOGGER.debug("Scheduled plots start in " + delay + "ms");
                scheduler.scheduleAtFixedRate(ps, delay, SubnetPlotter.DURATION_S, TimeUnit.SECONDS);
            }
        }
    }

    public void writeWebApp() {
        boolean writeHtml = configFile.getBoolean("writeHtml", DEFAULT_WRITE_HTML);
        if (writeHtml)
            webApp.writeHTML();
    }

    public void stop() {
        for (AbstractPlotScheduler ps : plotScheduler.values())
            ps.stop();
    }

    /**
     * Where it all begins
     * 
     * @param args
     */
    public static void main(String[] args) {
        PensiveArgs config = new PensiveArgs(args);

        ConfigFile cf = null;
		try {
			cf = new ConfigFile(config.configFileName);
		} catch (FileNotFoundException e) {
			LOGGER.error("Couldn't find config file " + config.configFileName + ". Use '-c' to create an example config.");
			System.exit(1);
		}
		
        Pensive pensive = new Pensive(cf);
        if (config.startTime < 0)
            pensive.createRealtimePlotSchedulers();
        else {
            pensive.createBackfillPlotSchedulers(config.startTime, config.endTime);
            pensive.stop();
        }
        pensive.writeWebApp();

    }
}
