package gov.usgs.pensive.plot;

import gov.usgs.plot.Plot;
import gov.usgs.plot.PlotException;
import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Time;
import gov.usgs.volcanoes.pensive.Channel;

import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A single subnet.
 * 
 * @author Tom Parker
 * 
 *         I waive copyright and related rights in the this work worldwide
 *         through the CC0 1.0 Universal public domain dedication.
 *         https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */
public class SubnetPlotter {

    /** my logger */
    private static final Logger LOGGER = LoggerFactory.getLogger(SubnetPlotter.class);

    public static final String DEFAULT_PATH_ROOT = "html";
    public static final String DEFAULT_FILE_PATH_FORMAT = "yyyy/DDD";
    public static final String DEFAULT_FILE_SUFFIX_FORMAT = "_yyyyMMdd-HHmm";

    public static final int DEFAULT_PLOT_WIDTH = 576;
    public static final int DEFAULT_PLOT_HEIGHT = 756;
    public static final int DEFAULT_THUMB_WIDTH = 151;
    public static final int DEFAULT_THUMB_HEIGHT = 198;
    public static final int DEFAULT_EMBARGO = 5;

    /** width of plot decorations in pixels */
    public static final int LABEL_HEIGHT = 35;

    /** height of plot decorations in pixels */
    public static final int LABEL_WIDTH = 30;

    /** The duration of a single plot */
    public static final int DURATION_S = 10 * 60;

    /** Root of plot directory */
    private final String pathRoot;

    /** format of file path */
    private final String filePathFormat;

    /** format of file name */
    private final String fileSuffixFormat;

    /** Network this subnet belongs to */
    public final String networkName;

    /** my name */
    public final String subnetName;

    /** Delay image production by this amount */
    public final int embargoMs;

    /** plot dimension */
    private final Dimension plotDimension;

    /** thumbnail dimension */
    private final Dimension thumbDimension;

    /** Channels on this plot */
    private final List<Channel> channels;

    /** height of a single channel plot */
    private int channelHeight;

    /**
     * Class constructor
     * 
     * @param networkName
     *            My network name
     * 
     * @param subnetName
     *            My subnet name
     * 
     * @param config
     *            My configuration stanza
     */
    public SubnetPlotter(String networkName, String subnetName, ConfigFile config) {
        this.subnetName = subnetName;
        this.networkName = networkName;

        pathRoot = config.getString("pathRoot", DEFAULT_PATH_ROOT);
        filePathFormat = config.getString("filePathFormat", DEFAULT_FILE_PATH_FORMAT);
        fileSuffixFormat = config.getString("fileSuffixFormat", DEFAULT_FILE_SUFFIX_FORMAT);
        embargoMs = config.getInt("embargo", DEFAULT_EMBARGO) * 1000;

        plotDimension = new Dimension();
        plotDimension.width = config.getInt("plotWidth", DEFAULT_PLOT_WIDTH);
        plotDimension.height = config.getInt("plotHeight", DEFAULT_PLOT_HEIGHT);

        thumbDimension = new Dimension();
        thumbDimension.width = config.getInt("thumbWidth", DEFAULT_THUMB_WIDTH);
        thumbDimension.height = config.getInt("thumbHeight", DEFAULT_THUMB_HEIGHT);

        channels = createChannels(config.getSubConfig(subnetName, true));
    }

    /**
     * Create Channel objects for each channel on this plot
     * 
     * @param config
     *            My configuration stanza
     * 
     * @return List object conatining my channels
     */
    private List<Channel> createChannels(ConfigFile config) {

        List<Channel> channels = new ArrayList<Channel>();
        List<String> chans = config.getList("channel");

        Dimension plotChanDimension = new Dimension();
        plotChanDimension.height = (plotDimension.height - 2 * FullPlotter.LABEL_HEIGHT) / chans.size();
        plotChanDimension.width = plotDimension.width;

        Dimension thumbChanDimension = new Dimension();
        thumbChanDimension.height = thumbDimension.height / chans.size();
        thumbChanDimension.width = thumbDimension.width;

        int idx = 0;
        for (String channel : chans) {
            boolean decorateX = (idx == chans.size() - 1) ? true : false;
            ConfigFile c = config.getSubConfig(channel, true);
            Channel chan = new Channel(channel, idx, plotChanDimension, thumbChanDimension, decorateX, c, networkName,
                    subnetName);
            channels.add(chan);
            idx++;
        }

        return channels;
    }

    /**
     * Produce both a full and a thumbnail PNG representing my subnet
     * 
     * @param plotEndMs
     *            time of last sample on plot
     * 
     * @param dataSource
     *            source of wave data
     */
    public void plot(long plotEndMs, SeismicDataSource dataSource) {
        Plot plot = new Plot(plotDimension.width, plotDimension.height);
        Plot thumb = new Plot(thumbDimension.width, thumbDimension.height);

        for (Channel channel : channels) {
            channel.updateWave(plotEndMs, dataSource);
            plot.addRenderer(channel.plot());
            thumb.addRenderer(channel.plotThumb());
        }

        String fileBase = generateFileBase(plotEndMs);
        new File(fileBase).getParentFile().mkdirs();

        writePNG(plot, fileBase + ".png");
        writePNG(thumb, fileBase + "_thumb.png");
    }

    /**
     * Write a plot to a PNG file
     * 
     * @param plot
     *            The plot to write
     * 
     * @param fileName
     *            The file to write
     */
    private void writePNG(Plot plot, String fileName) {
        LOGGER.debug("writing " + fileName);
        try {
            plot.writePNG(fileName);
        } catch (PlotException e) {
            LOGGER.error("Cannot write " + fileName + ": " + e.getLocalizedMessage());
        }
    }

    /**
     * Generate a file file by applying a SimpleDateFormat
     * 
     * @param timeMs
     *            end time of plot
     * @return generated file path
     */
    private String generateFileBase(long timeMs) {
        StringBuilder sb = new StringBuilder();
        sb.append(pathRoot + '/');
        if (networkName != null)
            sb.append(networkName + '/');
        sb.append(subnetName + '/');
        sb.append(Time.format(filePathFormat, timeMs));

        sb.append('/' + subnetName);
        sb.append(Time.format(fileSuffixFormat, timeMs));

        String name = sb.toString();
        name = name.replaceAll("/+", Matcher.quoteReplacement(File.separator));

        return name;
    }
}