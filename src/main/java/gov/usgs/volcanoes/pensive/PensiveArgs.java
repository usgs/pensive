/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.pensive;

import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.Switch;

import gov.usgs.volcanoes.core.args.Args;
import gov.usgs.volcanoes.core.args.Arguments;
import gov.usgs.volcanoes.core.args.decorator.ConfigFileArg;
import gov.usgs.volcanoes.core.args.decorator.CreateConfigArg;
import gov.usgs.volcanoes.core.args.decorator.TimeSpanArg;
import gov.usgs.volcanoes.core.args.decorator.VerboseArg;
import gov.usgs.volcanoes.core.time.TimeSpan;

import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Argument processor for Pensive.
 *
 * @author Tom Parker
 */
public class PensiveArgs {
  private static final Logger LOGGER = LoggerFactory.getLogger(PensiveArgs.class);

  private static final String EXAMPLE_CONFIG_FILENAME = "pensive-example.config";
  private static final String DEFAULT_CONFIG_FILENAME = "pensive.config";

  private static final String PROGRAM_NAME = "java -jar gov.usgs.volcanoes.pensive.Pensive";
  private static final String EXPLANATION = "I am the Pensive server\n";
  
  /** format of time on cmd line */
  public static final String INPUT_TIME_FORMAT = "yyyyMMddHHmm";

  private static final Parameter[] PARAMETERS = new Parameter[] {
	      new Switch("squelchHTML", 's', "squelchHTML", "Produce images only, no HTML."),
  };

  /** If true, log more. */
  public final boolean verbose;
  
  /** Time of first plot. May be in the future. */
  public final long startTime;
  
  /** Time of last plot. May be in the future. */
  public final long endTime;
  
  /** squelch HTML. */
  public final boolean squelchHTML;

  /** my config file. */
  public final String configFileName;

  /**
   * Class constructor.
   * @param commandLineArgs the command line arguments
   * @throws Exception when things go wrong
   */
  public PensiveArgs(final String[] commandLineArgs) throws Exception {
    Arguments args = null;
    args = new Args(PROGRAM_NAME, EXPLANATION, PARAMETERS);
    args = new ConfigFileArg(DEFAULT_CONFIG_FILENAME, args);
    args = new CreateConfigArg(EXAMPLE_CONFIG_FILENAME, args);
    args = new TimeSpanArg(INPUT_TIME_FORMAT, false, args);
    args = new VerboseArg(args);

    JSAPResult jsapResult = null;
    jsapResult = args.parse(commandLineArgs);

    verbose = jsapResult.getBoolean("verbose");
    if (verbose) {
      org.apache.log4j.Logger.getRootLogger().setLevel(Level.ALL);
      System.out.println("TOMP SAYS VERBOSE");
    } else {
      org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);
    }

    LOGGER.debug("Setting: verbose={}", verbose);

    squelchHTML = jsapResult.getBoolean("squelchHTML");
    
    final TimeSpan timeSpan = (TimeSpan) jsapResult.getObject("timeSpan");
    if (timeSpan == null) {
    		startTime = Long.MIN_VALUE;
    		endTime = Long.MAX_VALUE;
    } else {
        startTime = timeSpan.startTime;    	
        endTime = timeSpan.endTime;
    }
    LOGGER.debug("Setting: startTime={}", startTime);
    LOGGER.debug("Setting: endTime={}", endTime);

    configFileName = jsapResult.getString("config-filename");
    LOGGER.debug("Setting: config-filename={}", configFileName);

    if (jsapResult.getBoolean("create-config") || jsapResult.getBoolean("help")) {
      System.exit(1);
    }
  }
}
