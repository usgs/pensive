/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.pensive;

import gov.usgs.volcanoes.pensive.plot.SubnetPlotter;

/**
 * A single plot job.
 *
 * @author Tom Parker
 */
public class PlotJob implements Comparable<PlotJob> {

  /** if true replot the previous run */
  public static final boolean DO_PRINT_PREVIOUS = true;
  public static final boolean NO_PRINT_PREVIOUS = false;

  /** Time of last sample plotted. */
  public final long plotEndMs;

  /** when to create the plot. */
  public final long plotTimeMs;

  /** my subnet. */
  public final SubnetPlotter subnet;

  /**
   * Class constructor which uses the most recent time slice as the time of
   * the last sample to be plotted.
   *
   * @param subnet my subnet
   */
  public PlotJob(final SubnetPlotter subnet) {
    this(subnet, false);
  }

  /**
   * Class constructor which uses the most recent time slice as the time of
   * the last sample to be plotted.
   *
   * @param subnet my subnet
   */
  public PlotJob(final SubnetPlotter subnet, boolean oneBehind) {
    this.subnet = subnet;
    long plotEndMs = findPlotEnd();
    if (oneBehind) {
      plotEndMs -= SubnetPlotter.DURATION_S * 1000;
    }
    this.plotEndMs = plotEndMs;
    plotTimeMs = plotEndMs + subnet.embargoMs;
  }

  /**
   * Class constructor with a specific plot time.
   * 
   * @param subnet my subnet
   * @param plotEndMs The end of the plot
   */
  public PlotJob(final SubnetPlotter subnet, final long plotEndMs) {
    this.subnet = subnet;
    this.plotEndMs = plotEndMs;
    plotTimeMs = plotEndMs + subnet.embargoMs;
  }

  /**
   * Calculate the time of the last sample in the most recent time slice.
   *
   * @return Time of the last sample in the most recent time slice.
   */
  private long findPlotEnd() {
    long startTime = System.currentTimeMillis();
    startTime -= startTime % (SubnetPlotter.DURATION_S * 1000);

    return startTime;
  }

  /**
   * Order plots by increasing last sample time.
   *
   * @param other The PlotJob to compare to
   */
  public int compareTo(final PlotJob other) {
    return (int) (plotTimeMs - other.plotTimeMs);
  }
}
