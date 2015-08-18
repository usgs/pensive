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
        long firstPlot = startTime.getTime() - (startTime.getTime() % duration) + duration;
        long lastPlot = endTime.getTime();
        long offset = endTime.getTime() % duration;
        if (offset > 0)
            lastPlot += duration - offset;

        LOGGER.debug("Scheduling backfill plots ({} - {})", new Date(firstPlot), new Date(lastPlot));
        for (SubnetPlotter subnet : subnets) {
            try {        
                for (long plotTime = firstPlot; plotTime <= lastPlot; plotTime += duration) {
                    LOGGER.info("Scheduling subnet " + subnet.subnetName + " (" + new Date(plotTime - duration)  + " - " + new Date(plotTime) + ")");
                    plotJobs.put(new PlotJob(subnet, plotTime));
                }
            } catch (InterruptedException e) {
                LOGGER.info("Interrupted. Unable to schedule " + subnet.subnetName);
            }
        }
    }
}
