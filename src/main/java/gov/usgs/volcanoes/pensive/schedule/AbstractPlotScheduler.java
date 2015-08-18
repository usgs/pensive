package gov.usgs.volcanoes.pensive.schedule;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.util.ConfigFile;
import gov.usgs.volcanoes.pensive.PlotJob;
import gov.usgs.volcanoes.pensive.WaveSource;
import gov.usgs.volcanoes.pensive.plot.SubnetPlotter;

/**
 * Create a pool of connections to a single server and assign plot jobs to
 * connections as they become available.
 * 
 * @author Tom Parker
 * 
 *         I waive copyright and related rights in the this work worldwide
 *         through the CC0 1.0 Universal public domain dedication.
 *         https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */
public abstract class AbstractPlotScheduler implements Runnable {

    /** my logger */
    protected static final Logger LOGGER = LoggerFactory.getLogger("gov.usgs");

    /** number of concurrent connection to the wave server */
    public static final int DEFAULT_NUMTHREADS = 5;

    /** pool of plotters, each with their own wave server connection */
    private List<WaveSource> waveSources;

    private List<Thread> threads;
    
    /** Queue of plot jobs awaiting an available plotter */
    protected final BlockingQueue<PlotJob> plotJobs;

    /** list of subnets that feed from my wave server */
    protected final List<SubnetPlotter> subnets;

    /** number of connections to my wave server */
    private final int numThreads;

    /** name of this server */
    public final String name;

    private Date startDate;
    
    private Date endDate;
    
    private ConfigFile config;
    
    abstract protected void schedulePlots();
    
    /**
     * Class constructor
     * 
     * @param name
     *            name given to this wave server in the config file
     * @param config
     *            My configuration stanza
     */
    public AbstractPlotScheduler(String name, ConfigFile config) {

        this.name = name;
        numThreads = config.getInt("threads", DEFAULT_NUMTHREADS);
        subnets = new LinkedList<SubnetPlotter>();
        plotJobs = new LinkedBlockingQueue<PlotJob>();
        threads = new LinkedList<Thread>();
        this.config = config;
    }

    /**
     * Start threads
     */
    public void startWaveSources() {
        waveSources = new ArrayList<WaveSource>();
        for (int i = 0; i < numThreads; i++) {
            String n = name + "-" + i;
            WaveSource ws = new WaveSource(n, plotJobs, config);
            waveSources.add(ws);
            Thread t = new Thread(ws);
            t.setName(n);
            t.start();
            threads.add(t);
        }
    	
    }
    /**
     * 
     * @param subnet
     *            The subnet to be added
     */
    public void add(SubnetPlotter subnet) {
        subnets.add(subnet);
    }

    /**
     * 
     * @return count of subnets I have
     */
    public int subnetCount() {
        return subnets.size();
    }

    public void stop() {
    	for (WaveSource ws : waveSources) {
    		ws.stop();
    	}
    }
     
    /**
     * Schedule the next set of plots. Try to catch all exceptions,
     * ScheduledExecutorService does the wrong thing with exceptions.
     */
    public void run() {
        try {
            LOGGER.info("Scheduling plots for " + name);
            schedulePlots();
        } catch (Exception e) {
            LOGGER.error("Caught exception heading for scheduler. " + e.getLocalizedMessage());
        }
    }
}