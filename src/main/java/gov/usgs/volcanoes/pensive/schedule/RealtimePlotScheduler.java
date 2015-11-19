/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.pensive.schedule;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.pensive.PlotJob;
import gov.usgs.volcanoes.pensive.plot.SubnetPlotter;

public class RealtimePlotScheduler extends AbstractPlotScheduler {

  public RealtimePlotScheduler(final String name, final ConfigFile config) {
    super(name, config);
  }

  /**
   * Schedule the next plot for each subnet.
   */
  @Override
  protected void schedulePlots() {
    for (final SubnetPlotter subnet : subnets) {
      try {
        LOGGER.info("Scheduling subnet " + subnet.subnetName);
        plotJobs.put(new PlotJob(subnet));
      } catch (final InterruptedException e) {
        LOGGER.info("Interrupted. Unable to schedule " + subnet.subnetName);
      }
    }
  }

}
