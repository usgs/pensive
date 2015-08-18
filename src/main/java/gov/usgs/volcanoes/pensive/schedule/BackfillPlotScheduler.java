package gov.usgs.volcanoes.pensive.schedule;

import java.util.Date;

import gov.usgs.util.ConfigFile;
import gov.usgs.volcanoes.pensive.PlotJob;
import gov.usgs.volcanoes.pensive.plot.SubnetPlotter;

public class BackfillPlotScheduler extends AbstractPlotScheduler {

    private Date startTime;
    private Date endTime;

    public BackfillPlotScheduler(String name, ConfigFile config) {
        super(name, config);
    }

    public void setRange(Date startTime, Date endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Schedule the next plot for each subnet.
     */
    protected void schedulePlots() {
        long duration = SubnetPlotter.DURATION_S * 1000;
        long start = startTime.getTime() - (startTime.getTime() % duration);
        long end = endTime.getTime() + (duration - (endTime.getTime() % duration));

        for (SubnetPlotter subnet : subnets) {
            try {        
                for (long plotTime = start; plotTime <= end; plotTime += duration) {
                    LOGGER.info("Scheduling subnet " + subnet.subnetName + " (" + new Date(plotTime) + " - " + new Date(plotTime + duration) + ")");
                    plotJobs.put(new PlotJob(subnet, plotTime));
                }
            } catch (InterruptedException e) {
                LOGGER.info("Interrupted. Unable to schedule " + subnet.subnetName);
            }
        }
    }
}
