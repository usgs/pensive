package gov.usgs.volcanoes.pensive;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.UnflaggedOption;

public class Args extends SimpleJSAP {

    public static final String DEFAULT_CONFIG_FILENAME = "pensive.config";
    public static final String PROGRAM_NAME = "java -jar net.stash.pensive.Pensive";
    public static final String EXPLANATION_PREFACE = "I am the Pensive server";

    private static final String EXPLANATION = "\n";

    private static final Parameter[] PARAMETERS = new Parameter[] {
            new Switch("create-config", 'c', "create-config",
                    "Create an example config file in the curent working directory."),
            new Switch("verbose", 'v', "verbose", "Verbose logging."),
            new UnflaggedOption("config-filename", JSAP.STRING_PARSER, DEFAULT_CONFIG_FILENAME, JSAP.NOT_REQUIRED,
                    JSAP.NOT_GREEDY, "The config file name.") };

    private JSAPResult config;
    public final boolean createConfig;
    public final String configFileName;
    public final boolean verbose;
    
    public Args(String[] args) throws JSAPException {
            super(PROGRAM_NAME, EXPLANATION, PARAMETERS);
            config = parse(args);
        if (messagePrinted()) {
            // The following error message is useful for catching the case
            // when args are missing, but help isn't printed.
            if (!config.getBoolean("help"))
                System.err.println("Try using the --help flag.");

            System.exit(1);
        }

        createConfig = config.getBoolean("create-config");
        configFileName = config.getString("config-filename");
        verbose = config.getBoolean("verbose");
    }
}
