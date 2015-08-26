package gov.usgs.volcanoes.pensive;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.ParseException;
import com.martiansoftware.jsap.Switch;

import gov.usgs.volcanoes.util.args.Args;
import gov.usgs.volcanoes.util.args.Arguments;
import gov.usgs.volcanoes.util.args.decorator.ConfigFileArg;
import gov.usgs.volcanoes.util.args.decorator.CreateConfigArg;
import gov.usgs.volcanoes.util.args.parser.DateStringParser;

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

    private static final DateStringParser DATE_PARSER = new DateStringParser(INPUT_TIME_FORMAT);
    
    private static final Parameter[] PARAMETERS = new Parameter[] {
            new Switch("verbose", 'v', "verbose", "Verbose logging."),
            new FlaggedOption("startTime", DATE_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 's',
                    "startTime", "Start of backfill period\n"),
            new FlaggedOption("endTime", DATE_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'e',
                    "endTime", "End of backfill period\n")};

    public final boolean verbose;
    public final Date startTime;
    public final Date endTime;
    public final String configFileName;

    public PensiveArgs(String[] commandLineArgs) {
    	Arguments args = new Args(PROGRAM_NAME, EXPLANATION, PARAMETERS);
    	try {
			args = new ConfigFileArg(DEFAULT_CONFIG_FILENAME, args);
			args = new CreateConfigArg(EXAMPLE_CONFIG_FILENAME, args);
		} catch (JSAPException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	
    	JSAPResult jsapResult = null;
		try {
			jsapResult = args.parse(commandLineArgs);
		} catch (ParseException e) {
			LOGGER.error("Cannot parse command line.");
			System.exit(1);
		}
		
        verbose = jsapResult.getBoolean("verbose");
        LOGGER.debug("Setting: verbose={}", verbose);

        startTime = jsapResult.getDate("startTime");
        LOGGER.debug("Setting: startTime={}", startTime);
        
        endTime = jsapResult.getDate("endTime");
        LOGGER.debug("Setting: endTime={}", endTime);

        configFileName = jsapResult.getString("config-filename");
        LOGGER.debug("Setting: config-filename={}", configFileName);

        if (!validateTimes())
            System.exit(1);
        
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