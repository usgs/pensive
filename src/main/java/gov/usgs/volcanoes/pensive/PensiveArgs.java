package gov.usgs.volcanoes.pensive;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.ParseException;

import gov.usgs.volcanoes.util.args.Args;
import gov.usgs.volcanoes.util.args.Arguments;
import gov.usgs.volcanoes.util.args.decorator.ConfigFileArg;
import gov.usgs.volcanoes.util.args.decorator.CreateConfigArg;
import gov.usgs.volcanoes.util.args.decorator.DateRangeArg;
import gov.usgs.volcanoes.util.args.decorator.VerboseArg;

/**
 * Argument processor for Pensive
 * 
 * @author Tom Parker
 * 
 *         I waive copyright and related rights in the this work worldwide
 *         through the CC0 1.0 Universal public domain dedication.
 *         https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */
public class PensiveArgs {
    private static final Logger LOGGER = LoggerFactory.getLogger(PensiveArgs.class);

    public static final String EXAMPLE_CONFIG_FILENAME = "pensive-example.config";
    public static final String DEFAULT_CONFIG_FILENAME = "pensive.config";

    public static final String PROGRAM_NAME = "java -jar gov.usgs.volcanoes.pensive.Pensive";
    public static final String EXPLANATION = "I am the Pensive server\n";
    public static final String INPUT_TIME_FORMAT = "yyyyMMddHHmm";

    private static final Parameter[] PARAMETERS = new Parameter[] {};

    public final boolean verbose;
    public final long startTime;
    public final long endTime;
    public final String configFileName;

    public PensiveArgs(String[] commandLineArgs) throws Exception {
        Arguments args = null;
        args = new Args(PROGRAM_NAME, EXPLANATION, PARAMETERS);
        args = new ConfigFileArg(DEFAULT_CONFIG_FILENAME, args);
        args = new CreateConfigArg(EXAMPLE_CONFIG_FILENAME, args);
        args = new DateRangeArg(INPUT_TIME_FORMAT, args);
        args = new VerboseArg(args);

        JSAPResult jsapResult = null;
        jsapResult = args.parse(commandLineArgs);

        verbose = jsapResult.getBoolean("verbose");
        LOGGER.debug("Setting: verbose={}", verbose);

        Date startDate = jsapResult.getDate("startTime");
        if (startDate == null)
            startTime = Long.MIN_VALUE;
        else
            startTime = jsapResult.getDate("startTime").getTime();
        LOGGER.debug("Setting: startTime={}", startTime);

        Date endDate = jsapResult.getDate("endTime");
        if (endDate == null)
            endTime = Long.MIN_VALUE;
        else
            endTime = jsapResult.getDate("endTime").getTime();
        LOGGER.debug("Setting: endTime={}", endTime);

        configFileName = jsapResult.getString("config-filename");
        LOGGER.debug("Setting: config-filename={}", configFileName);

        if (jsapResult.getBoolean("create-config"))
            System.exit(1);
    }
}