package gov.usgs.volcanoes.pensive;

import java.util.Date;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.UnflaggedOption;

import gov.usgs.volcanoes.util.args.Args;
import gov.usgs.volcanoes.util.args.DateStringParser;

/**
 * Argument processor for Pensive
 * 
 * @author Tom Parker
 * 
 *         I waive copyright and related rights in the this work worldwide
 *         through the CC0 1.0 Universal public domain dedication.
 *         https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */
public class PensiveArgs extends Args {

    public static final String DEFAULT_CONFIG_FILENAME = "pensive.config";
    public static final String PROGRAM_NAME = "java -jar gov.usgs.volcanoes.pensive.Pensive";
    public static final String EXPLANATION = "I am the Pensive server\n";
    public static final String INPUT_TIME_FORMAT = "yyyyMMddHHmm";

    private static final DateStringParser DATE_PARSER = new DateStringParser(INPUT_TIME_FORMAT);
    
    private static final Parameter[] PARAMETERS = new Parameter[] {
            new Switch("create-config", 'c', "create-config",
                    "Create an example config file in the curent working directory."),
            new Switch("verbose", 'v', "verbose", "Verbose logging."),
            new FlaggedOption("startTime", DATE_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 's',
                    "startTime", "Start of backfill period\n"),
            new FlaggedOption("endTime", DATE_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'e',
                    "endTime", "End of backfill period\n"),
            new UnflaggedOption("config-filename", JSAP.STRING_PARSER, DEFAULT_CONFIG_FILENAME, JSAP.NOT_REQUIRED,
                    JSAP.NOT_GREEDY, "The config file name.") };

    private JSAPResult config;
    public final boolean createConfig;
    public final String configFileName;
    public final boolean verbose;
    public final Date startTime;
    public final Date endTime;

    public PensiveArgs(String[] args) throws JSAPException {
        super(PROGRAM_NAME, EXPLANATION, PARAMETERS);
        config = parse(args);

        createConfig = config.getBoolean("create-config");
        configFileName = config.getString("config-filename");
        verbose = config.getBoolean("verbose");

        startTime = config.getDate("startTime");
        endTime = config.getDate("endTime");
        if (!validateTimes())
            System.exit(1);
    }

    private boolean validateTimes() {
        if (startTime == null && endTime == null)
            return true;

        if (startTime != null && endTime == null) {
            System.err.println("endTime argument is missing");
            return false;
        }

        if (endTime != null && startTime == null) {
            System.err.println("startTime argument is missing");
            return false;
        }

        if (!endTime.after(startTime)) {
            System.err.println("startTime must be before endTime");
            return false;
        }
            
        return true;
    }
}