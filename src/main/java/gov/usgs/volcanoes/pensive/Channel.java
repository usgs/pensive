/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.pensive;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.core.data.SliceWave;
import gov.usgs.volcanoes.core.data.Wave;
import gov.usgs.volcanoes.core.legacy.plot.render.Renderer;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.Time;
import gov.usgs.volcanoes.pensive.plot.ChannelPlotter;
import gov.usgs.volcanoes.pensive.plot.FullPlotter;
import gov.usgs.volcanoes.pensive.plot.SubnetPlotter;
import gov.usgs.volcanoes.pensive.plot.ThumbnailPlotter;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;

/**
 * A single channel of seismic data on a single subnet plot.
 *
 * @author Tom Parker
 */
public class Channel {

  /** my logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(Channel.class);

  /** If true write data files. */
  public static final boolean DEFAULT_WRITE_DATA = false;

  /** Default format for data file suffix. */
  public static final String DEFAULT_DATA_FILE_SUFFIX_FORMAT = "_yyyyMMdd";

  /** Default data timestamp. */
  public static final String DEFAULT_DATA_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

  /** channel name in config file format. */
  public final String name;

  /** My full ChannelPlotter. */
  private final ChannelPlotter plot;

  /** My thumbnail ChannelPlotter. */
  private final ChannelPlotter thumb;

  /** If true write data files. */
  private final boolean writeData;

  /** format of data file path. */
  private final String dataFilePathFormat;

  /** format of data file name. */
  private final String dataFileSuffixFormat;

  /** Where to write data files. */
  private final String dataPathRoot;

  /** my network name. */
  private final String networkName;

  /** my subnet name. */
  private final String subnetName;

  /** data file timestamp format. */
  private final String dataTimestampFormat;

  /**
   * Class constructor.
   *
   * @param channel my channel
   * @param index my index into the plot
   * @param plotDimension Dimension of the full plot
   * @param thumbDimension Dimension of the thumbnail plot
   * @param decorateX If true decorate x-axis on full plot
   * @param config My config stanza
   * @param networkName my network name
   * @param subnetName my subnet name
   */
  public Channel(final String channel, final int index, final Dimension plotDimension,
      final Dimension thumbDimension, final boolean decorateX, final ConfigFile config,
      final String networkName, final String subnetName) {
    this.name = channel;

    plot = new FullPlotter(channel, index, plotDimension, decorateX, config);
    thumb = new ThumbnailPlotter(channel, index, thumbDimension, config);
    writeData = config.getBoolean("writeData", DEFAULT_WRITE_DATA);

    dataFilePathFormat = config.getString("dataFilePathFormat", config.getString("filePathFormat"));
    dataFileSuffixFormat =
        config.getString("dataFileSuffixFormat", DEFAULT_DATA_FILE_SUFFIX_FORMAT);
    dataPathRoot = config.getString("dataPathRoot", config.getString("pathRoot"));
    dataTimestampFormat = config.getString("dataTimestampFormat", DEFAULT_DATA_TIMESTAMP_FORMAT);

    this.networkName = networkName;
    this.subnetName = subnetName;
  }

  /**
   * Gather new wave data and offer to plotters.
   *
   * @param plotEndMs Time of last sample of waveform
   * @param dataSource Who to ask for data
   */
  public void updateWave(final long plotEndMs, final SeismicDataSource dataSource) {
    final double t2 = J2kSec.fromEpoch(plotEndMs);
    final double t1 = t2 - SubnetPlotter.DURATION_S;
    final Wave w = dataSource.getWave(name.replace('_', ' '), t1, t2);
    if (w != null && w.numSamples() > 0) {
      w.detrend();
      w.removeMean();
      SliceWave wave = new SliceWave(w);
      wave.setSlice(t1, t2);
      plot.setWave(wave);
      wave = new SliceWave(w);
      wave.setSlice(t1, t2);
      thumb.setWave(wave);
    } else {
      plot.setWave(null);
      thumb.setWave(null);
    }
  }

  /**
   * Create a full plot.
   *
   * @return The plot Renderer
   */
  public Renderer plot() {
    if (writeData) {
      writeData();
    }

    return plot.plot();
  }

  private void writeData() {
    final String csv = plot.getCsv(dataTimestampFormat);
    if (csv == null) {
      return;
    }

    final String fileBase = generateFileBase(plot.getPlotEndMs());
    final File file = new File(fileBase);

    file.getParentFile().mkdirs();

    try {
      if (!file.exists()) {
        file.createNewFile();
      }

      final FileWriter fw = new FileWriter(file, true);
      fw.append(csv);
      fw.close();
    } catch (final IOException e) {
      LOGGER.error("Error writing file. {}", e);
    }

  }

  private String generateFileBase(final long timeMs) {
    final StringBuilder sb = new StringBuilder();
    sb.append(dataPathRoot + '/');
    if (networkName != null) {
      sb.append(networkName + '/');
    }
    sb.append(subnetName + '/');
    sb.append(Time.format(dataFilePathFormat, timeMs));

    sb.append('/' + name);
    sb.append(Time.format(dataFileSuffixFormat, timeMs));
    sb.append(".dat");
    String name = sb.toString();
    name = name.replaceAll("/+", Matcher.quoteReplacement(File.separator));
    name = name.replaceAll("\\s+", "_");
    return name;
  }

  /**
   * Create a thumbnail plot.
   *
   * @return The thumbnail Renderer
   */
  public Renderer plotThumb() {
    return thumb.plot();
  }

  public void flushWave() {
    plot.setWave(null);
    thumb.setWave(null);
  }

}
