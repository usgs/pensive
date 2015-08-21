package gov.usgs.volcanoes.pensive.plot;

import gov.usgs.plot.render.TextRenderer;
import gov.usgs.plot.render.wave.SliceWaveRenderer;
import gov.usgs.plot.render.wave.SpectrogramRenderer;
import gov.usgs.volcanoes.util.configFile.ConfigFile;

import java.awt.Dimension;
import java.awt.Font;

/**
 * 
 * @author Tom Parker
 * 
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */
public class ThumbnailPlotter extends ChannelPlotter {

    /** Font used to indicate no data available */
    public static final Font NO_DATA_FONT = Font.decode("dialog-PLAIN-12");

    /**
     * Class constructor
     * 
     * @param name
     *            My name
     * @param index
     *            My position on the subnet plot
     * @param plotDimension
     *            My configuration stanza
     * @param config
     */
    public ThumbnailPlotter(String name, int index, Dimension plotDimension, ConfigFile config) {
        super(name, index, plotDimension, config);

        noDataFont = NO_DATA_FONT;
    }

    /**
     * Apply any settings needed to the Spectrogram renderer
     * 
     * @param spectrogramRenderer
     *            my SpectrogramRenderer
     */
    protected void tweakSpectrogramRenderer(SpectrogramRenderer spectrogramRenderer) {

        // y-axis labels will sometimes not be displayed if x-axis tick marks
        // are not displayed. Note sure why.
        spectrogramRenderer.yTickMarks = false;
        spectrogramRenderer.yTickValues = false;
        spectrogramRenderer.xTickMarks = false;
        spectrogramRenderer.xTickValues = false;
        spectrogramRenderer.xUnits = false;
        spectrogramRenderer.xLabel = false;

        int top = index * plotDimension.height + waveHeight;
        spectrogramRenderer.setLocation(0, top, plotDimension.width, plotDimension.height - waveHeight);
    }

    /**
     * Apply any settings needed to the WaveRenderer
     * 
     * @param waveRenderer
     *            my WaveRenderer
     */
    @Override
    protected void tweakWaveRenderer(SliceWaveRenderer waveRenderer) {
        int top = index * plotDimension.height;
        int width = plotDimension.width;
        waveRenderer.setLocation(0, top, width, waveHeight);

    }

    /**
     * Apply any tweaks needed to the textRenderer
     * 
     * @param textRenderer
     *            My text renderer
     */
    protected void tweakNoDataRenderer(TextRenderer textRenderer) {
        // no tweaks needed
    }

}
