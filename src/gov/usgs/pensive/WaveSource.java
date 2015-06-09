package gov.usgs.pensive;

import gov.usgs.pensive.plot.SubnetPlotter;
import gov.usgs.swarm.data.DataSourceType;
import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Time;

import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retrieve data and produce plot
 * 
 * @author Tom Parker
 * 
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */
public class WaveSource implements Runnable {

	/** my logger */
	private static final Logger LOGGER = LoggerFactory.getLogger("gov.usgs");

	public static final String DEFAULT_TYPE = "wws";
	public static final String DEFAULT_HOST = "localhost";
	public static final int DEFAULT_PORT = 16022;
	public static final int DEFAULT_TIMEOUT_S = 15;

	/** source of wave data */
	private final SeismicDataSource dataSource;

	/** Jobs to be plotted */
	private BlockingQueue<PlotJob> plotJobs;

	/** my name */
	public final String name;

	/**
	 * Class constructor
	 * 
	 * @param name
	 *            My name
	 * @param plotJobs
	 *            Queue containing jobs to plot
	 * @param config
	 *            My config stanza
	 */
	public WaveSource(String name, BlockingQueue<PlotJob> plotJobs, ConfigFile config) {
		this.plotJobs = plotJobs;

		this.name = name;
		String type = config.getString("type", DEFAULT_TYPE);
		String host = config.getString("host", DEFAULT_HOST);
		int port = config.getInt("port", DEFAULT_PORT);
		int timeout = config.getInt("timeout", DEFAULT_TIMEOUT_S);
		int compress = 1;

		String dsString = name + ";" + type + ":" + host + ":" + port + ":" + timeout * 1000 + ":" + compress;
		dataSource = DataSourceType.parseConfig(dsString);
		dataSource.establish();
		dataSource.setUseCache(false);
	}

	/**
	 * Take plot jobs and produce files.
	 */
	@Override
	public void run() {
		while (true) {
			PlotJob pj = null;
			try {
				pj = plotJobs.peek();
				if (pj == null)
				    continue;
				long timeMs = System.currentTimeMillis();
				if (timeMs < pj.plotTimeMs) {
				    LOGGER.trace("Wave source {} idle. Next subnet is {} in {} s", name, pj.subnet.subnetName, (pj.plotTimeMs - timeMs)/1000);
				    Thread.sleep(1000);
				    continue;
				}

				pj = plotJobs.take();
				SubnetPlotter subnet = pj.subnet;

				LOGGER.debug("Ploting {} from {}", subnet.subnetName, name);
				subnet.plot(pj.plotEndMs, dataSource);
			} catch (InterruptedException noAction) {
				continue;
			}
		}
	}
}
