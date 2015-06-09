package gov.usgs.pensive.plot;

import gov.usgs.plot.render.TextRenderer;
import gov.usgs.plot.render.wave.SliceWaveRenderer;
import gov.usgs.plot.render.wave.SpectrogramRenderer;
import gov.usgs.util.ConfigFile;

import java.awt.Dimension;
import java.awt.Font;

/**
 * 
 * @author Tom Parker
 * 
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */
public class FullPlotter extends ChannelPlotter {

    /** Font used to indicate that no data was available */
    public static final Font NO_DATA_FONT = Font.decode("dialog-PLAIN-36");

    /** height of plot decorations in pixels */
    public static final int LABEL_HEIGHT = 35;

    /** width of plot decorations in pixels */
    public static final int LABEL_WIDTH = 30;

    /**
     * Class constructor
     * 
     * @param name
     *            My name
     * 
     * @param index
     *            My position on the subnet plot
     * 
     * @param plotDimension
     *            The dimension of my plot
     * 
     * @param decorateX
     *            If true, decorate the X-axis
     * 
     * @param config
     *            My configuration stanza
     */
    public FullPlotter(String name, int index, Dimension plotDimension, boolean decorateX, ConfigFile config) {
        super(name, index, plotDimension, config);

        spectrogramRenderer.xTickValues = decorateX;
        spectrogramRenderer.xUnits = decorateX;
        spectrogramRenderer.xLabel = decorateX;

        noDataFont = NO_DATA_FONT;
    }

    /**
     * Apply any settings needed to my SpectrogramRenderer
     * 
     * @param my
     *            SpectrogramRenderer
     */
    protected void tweakSpectrogramRenderer(SpectrogramRenderer spectrogramRenderer) {
        spectrogramRenderer.yTickMarks = true;
        spectrogramRenderer.yTickValues = true;
        spectrogramRenderer.xTickMarks = true;
        spectrogramRenderer.setYLabelText(name);

        int top = index * plotDimension.height;
        spectrogramRenderer.setLocation(LABEL_WIDTH, top + waveHeight + LABEL_HEIGHT, plotDimension.width
                - (2 * LABEL_WIDTH), plotDimension.height - waveHeight);
    }

    /**
     * Apply any settings needed to my WaveRenderer
     * 
     * @param my
     *            WaveRenderer
     */
    protected void tweakWaveRenderer(SliceWaveRenderer waveRenderer) {
        int top = index * plotDimension.height + LABEL_HEIGHT;
        int width = plotDimension.width;
        waveRenderer.setLocation(LABEL_WIDTH, top, width - 2 * LABEL_WIDTH, waveHeight);

        waveRenderer.xTickMarks = true;
    }

    /**
     * Apply any settings needed to my TextRenderer
     * 
     * @param my
     *            TextRenderer
     */
    protected void tweakNoDataRenderer(TextRenderer textRenderer) {
        textRenderer.y += LABEL_HEIGHT;
    }

}
