package gov.usgs.volcanoes.pensive;

import gov.usgs.util.ConfigFile;
import gov.usgs.util.Util;
import gov.usgs.volcanoes.pensive.plot.SubnetPlotter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.martiansoftware.jsap.JSAPException;

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
    public static final String DEFAULT_CONFIG_FILENAME = "pensive.config";

    /** my logger */
    private static final Logger LOGGER = LoggerFactory.getLogger(Pensive.class);

    /** my configuration file */
    private ConfigFile configFile;

    /** My SPA */
    private Page page;

    /** One plot scheduler per wave server */
    private Map<String, PlotScheduler> plotScheduler;

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
        LOGGER.info("Launching Pensive ({})", Pensive.getVersion());

        page = new Page(configFile);

        createPlotSchedulers();
        assignSubnets();
        pruneSchedulers();

        boolean writeHtml = configFile.getBoolean("writeHtml", DEFAULT_WRITE_HTML);
        if (writeHtml)
            page.writeHTML();
    }

    /**
     * Create one PlotScheduler per wave server, each running in its own thread.
     */
    private void createPlotSchedulers() {
        plotScheduler = new HashMap<String, PlotScheduler>();

        for (String server : configFile.getList("waveSource")) {
            ConfigFile c = configFile.getSubConfig(server, true);
            LOGGER.info("Creating plot scheduler for " + server);
            plotScheduler.put(server, new PlotScheduler(server, c));
        }
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
                    page.addSubnet(network, subnet);
                }

                String dataSource = subnetConfig.getString("dataSource");
                PlotScheduler scheduler = plotScheduler.get(dataSource);
                LOGGER.info("Assigning subnet " + subnet + " to " + dataSource);
                scheduler.add(new SubnetPlotter(network, subnet, subnetConfig));
            }
            netConfig.putList("subnet", subnets);
        }
        configFile.putList("network", networks);

        Iterator<String> schedulerIt = plotScheduler.keySet().iterator();
        while (schedulerIt.hasNext()) {
            String server = schedulerIt.next();
            PlotScheduler ps = plotScheduler.get(server);
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
            PlotScheduler ps = plotScheduler.get(server);
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
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        for (PlotScheduler ps : plotScheduler.values()) {

            // schedule first plot immediately
            new Thread(ps).start();

            // satrt automated plots at the top of the next period
            int delay = SubnetPlotter.DURATION_S;
            delay -= (System.currentTimeMillis() / 1000) % SubnetPlotter.DURATION_S;
            LOGGER.debug("Scheduled plots start in " + delay + "ms");
            scheduler.scheduleAtFixedRate(ps, delay, SubnetPlotter.DURATION_S, TimeUnit.SECONDS);
        }
    }

    public static void createConfig() throws IOException {

        InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("pensive-example.config");
        FileOutputStream os = new FileOutputStream(DEFAULT_CONFIG_FILENAME);
        try {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
    }

    /**
     * 
     * @return version string
     */
    public static String getVersion() {
        String[] v = Util.getVersion(Pensive.class.getPackage().getName());
        String version;
        if (v != null)
            version = "Version: " + v[0] + " Built: " + v[1];
        else
            version = "No version information available.";

        return version;
    }

    /**
     * Where it all begins
     * 
     * @param args
     */
    public static void main(String[] args) {
        Args config = null;
        try {
            config = new Args(args);
        } catch (JSAPException e1) {
            System.err.println("Couldn't parse command line. Try using the --help flag.");
            System.exit(1);
        }

        if (config.createConfig) {
            try {
                LOGGER.warn("Creating example config " + DEFAULT_CONFIG_FILENAME);
                Pensive.createConfig();
            } catch (IOException e) {
                LOGGER.warn("Cannot write example config. " + e.getLocalizedMessage());
            }
            System.exit(0);
        }

        ConfigFile cf = new ConfigFile(config.configFileName);
        if (!cf.wasSuccessfullyRead()) {
            LOGGER.warn("Can't parse config file " + config.configFileName + ". Try using the --help flag.");
            System.exit(1);
        }

        Pensive pensive = new Pensive(cf);
        pensive.schedulePlots();
    }
}
