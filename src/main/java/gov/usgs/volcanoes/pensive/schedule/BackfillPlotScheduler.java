/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.pensive.schedule;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.pensive.PlotJob;
import gov.usgs.volcanoes.pensive.plot.SubnetPlotter;

import java.util.Date;

public class BackfillPlotScheduler extends AbstractPlotScheduler {

  private long startTime;
  private long endTime;

  public BackfillPlotScheduler(final String name, final ConfigFile config) {
    super(name, config);
  }

  public void setRange(final long startTime, final long endTime) {
    this.startTime = startTime;
    this.endTime = endTime;
  }

  /**
   * Schedule the next plot for each subnet.
   */
  @Override
  protected void schedulePlots() {
    final long duration = SubnetPlotter.DURATION_S * 1000;
    final long firstPlot = startTime - (startTime % duration) + duration;
    long lastPlot = endTime;
    final long offset = endTime % duration;
    if (offset > 0) {
      lastPlot += duration - offset;
    }
    
    LOGGER.debug("Scheduling backfill plots ({} - {})", new Date(firstPlot), new Date(lastPlot));
    for (final SubnetPlotter subnet : subnets) {
      try {
        for (long plotTime = firstPlot; plotTime <= lastPlot; plotTime += duration) {
          LOGGER.info("Scheduling subnet " + subnet.subnetName + " ("
              + new Date(plotTime - duration) + " - " + new Date(plotTime) + ")");
          plotJobs.put(new PlotJob(subnet, plotTime));
        }
      } catch (final InterruptedException e) {
        LOGGER.info("Interrupted. Unable to schedule " + subnet.subnetName);
      }
    }
  }
}
