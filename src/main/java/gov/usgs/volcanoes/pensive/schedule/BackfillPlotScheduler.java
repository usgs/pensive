package gov.usgs.volcanoes.pensive.schedule;

import java.util.Date;

import gov.usgs.volcanoes.util.configFile.ConfigFile;
import gov.usgs.volcanoes.pensive.PlotJob;
import gov.usgs.volcanoes.pensive.plot.SubnetPlotter;

public class BackfillPlotScheduler extends AbstractPlotScheduler {

    private long startTime;
    private long endTime;

    public BackfillPlotScheduler(String name, ConfigFile config) {
        super(name, config);
    }

    public void setRange(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Schedule the next plot for each subnet.
     */
    protected void schedulePlots() {
        long duration = SubnetPlotter.DURATION_S * 1000;
        long firstPlot = startTime - (startTime % duration) + duration;
        long lastPlot = endTime;
        long offset = endTime % duration;
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
