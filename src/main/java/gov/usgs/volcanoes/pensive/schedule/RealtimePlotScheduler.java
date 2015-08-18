package gov.usgs.volcanoes.pensive.schedule;

import gov.usgs.util.ConfigFile;
import gov.usgs.volcanoes.pensive.PlotJob;
import gov.usgs.volcanoes.pensive.plot.SubnetPlotter;

public class RealtimePlotScheduler extends AbstractPlotScheduler {

    public RealtimePlotScheduler(String name, ConfigFile config) {
        super(name, config);
    }

    /**
     * Schedule the next plot for each subnet.
     */
    protected void schedulePlots() {
        for (SubnetPlotter subnet : subnets) {
             try {
                LOGGER.info("Scheduling subnet " + subnet.subnetName);
                plotJobs.put(new PlotJob(subnet));
            } catch (InterruptedException e) {
                LOGGER.info("Interrupted. Unable to schedule " + subnet.subnetName);
            }
        }
    }

}
