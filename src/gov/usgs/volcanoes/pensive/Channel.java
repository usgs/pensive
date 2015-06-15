package gov.usgs.volcanoes.pensive;

import gov.usgs.plot.data.SliceWave;
import gov.usgs.plot.data.Wave;
import gov.usgs.plot.render.Renderer;
import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Time;
import gov.usgs.util.Util;
import gov.usgs.volcanoes.pensive.plot.ChannelPlotter;
import gov.usgs.volcanoes.pensive.plot.FullPlotter;
import gov.usgs.volcanoes.pensive.plot.SubnetPlotter;
import gov.usgs.volcanoes.pensive.plot.ThumbnailPlotter;

import java.awt.Dimension;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A single channel of seismic data on a single subnet plot.
 * 
 * @author Tom Parker
 * 
 *         I waive copyright and related rights in the this work worldwide
 *         through the CC0 1.0 Universal public domain dedication.
 *         https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */
public class Channel {

    /** my logger */
    private static final Logger LOGGER = LoggerFactory.getLogger(Channel.class);

    /** do I write data files? */
    public static final boolean DEFAULT_WRITE_DATA = true;

    /** Default format for data file suffix */
    public static final String DEFAULT_DATA_FILE_SUFFIX_FORMAT = "_yyyyMMdd";

    /** Default data timestamp */
    public static final String DEFAULT_DATA_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    /** channel name in config file format */
    public final String name;

    /** My full ChannelPlotter */
    private final ChannelPlotter plot;

    /** My thumbnail ChannelPlotter */
    private final ChannelPlotter thumb;

    /** Do I write data files? */
    private final boolean writeData;

    /** format of data file path */
    private final String dataFilePathFormat;

    /** format of data file name */
    private final String dataFileSuffixFormat;

    /** Where to write data files */
    private final String dataPathRoot;

    /** my network name */
    private final String networkName;

    /** my subnet name */
    private final String subnetName;

    /** data file timestamp format */
    private final String dataTimestampFormat;

    /**
     * Class constructor
     * 
     * @param channel
     *            my channel
     * @param index
     *            my index into the plot
     * @param plotDimension
     *            Dimension of the full plot
     * @param thumbDimension
     *            Dimension of the thumbnail plot
     * @param decorateX
     *            If true decorate x-axis on full plot
     * @param config
     *            My config stanza
     */
    public Channel(String channel, int index, Dimension plotDimension, Dimension thumbDimension, boolean decorateX,
            ConfigFile config, String networkName, String subnetName) {
        this.name = channel;

        plot = new FullPlotter(channel, index, plotDimension, decorateX, config);
        thumb = new ThumbnailPlotter(channel, index, thumbDimension, config);
        writeData = config.getBoolean("writeData", DEFAULT_WRITE_DATA);

        dataFilePathFormat = config.getString("dataFilePathFormat", config.getString("filePathFormat"));
        dataFileSuffixFormat = config.getString("dataFileSuffixFormat", DEFAULT_DATA_FILE_SUFFIX_FORMAT);
        dataPathRoot = config.getString("dataPathRoot", config.getString("pathRoot"));
        dataTimestampFormat = config.getString("dataTimestampFormat", DEFAULT_DATA_TIMESTAMP_FORMAT);

        this.networkName = networkName;
        this.subnetName = subnetName;
    }

    /**
     * Gather new wave data and offer to plotters
     * 
     * @param plotEndMs
     *            Time of last sample of waveform
     * @param dataSource
     *            Who to ask for data
     */
    public void updateWave(long plotEndMs, SeismicDataSource dataSource) {
        double t2 = Util.ewToJ2K(plotEndMs / 1000);
        double t1 = t2 - SubnetPlotter.DURATION_S;
        Wave w = dataSource.getWave(name.replace('_', ' '), t1, t2);
        SliceWave wave = null;
        if (w != null) {
            w.detrend();
            w.removeMean();
            wave = new SliceWave(w);
            wave.setSlice(t1, t2);
        }
        plot.setWave(wave);
        thumb.setWave(wave);
    }

    /**
     * Create a full plot
     * 
     * @return The plot Renderer
     */
    public Renderer plot() {
        if (writeData)
            writeData();

        return plot.plot();
    }

    private void writeData() {
        String csv = plot.getCSV(dataTimestampFormat);
        if (csv == null)
            return;

        String fileBase = generateFileBase(plot.getPlotEndMS());
        File file = new File(fileBase);

        file.getParentFile().mkdirs();

        try {
            if (!file.exists())
                file.createNewFile();

            FileWriter fw = new FileWriter(file, true);
            fw.append(csv);
            fw.close();
        } catch (IOException e) {
        }

    }

    private String generateFileBase(long timeMS) {
        StringBuilder sb = new StringBuilder();
        sb.append(dataPathRoot + '/');
        if (networkName != null)
            sb.append(networkName + '/');
        sb.append(subnetName + '/');
        sb.append(Time.format(dataFilePathFormat, timeMS));

        sb.append('/' + name);
        sb.append(Time.format(dataFileSuffixFormat, timeMS));
        sb.append(".dat");
        String name = sb.toString();
        name = name.replaceAll("/+", Matcher.quoteReplacement(File.separator));
        name = name.replaceAll("\\s+", "_");
        return name;
    }

    /**
     * Create a thumbnail plot
     * 
     * @return The thumbnail Renderer
     */
    public Renderer plotThumb() {
        return thumb.plot();
    }

}