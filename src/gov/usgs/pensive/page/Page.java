package gov.usgs.pensive.page;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;
import gov.usgs.pensive.Pensive;
import gov.usgs.pensive.plot.SubnetPlotter;
import gov.usgs.util.ConfigFile;

/**
 * 
 * @author Tom Parker
 * 
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */
public class Page {
    /** my logger */
    private static final Logger LOGGER = LoggerFactory.getLogger(Page.class);

    public static final String DEFAULT_PATH_ROOT = "html/";
    
    /** filename of html file */
    public static final String FILENAME = "index.html";

    /** Freemarker settings */
    private Map<String, Object> root;
    
    /** my configuration */
    private Configuration cfg;
    
    /** My subnets */
    private Map<String, List<String>> subnets;
    
    /** root of output*/
    private final String pathRoot;
    
    /**
     * Class constructor
     * 
     * @param config
     *          My configuration stanza
     */
    public Page(ConfigFile config) {
        pathRoot = config.getString("pathRoot", DEFAULT_PATH_ROOT);
        
        root = new HashMap<String, Object>();
        
        subnets = new HashMap<String, List<String>>();
        root.put("subnets", subnets);
        
        root.put("refreshPeriod", SubnetPlotter.DURATION_S);
        root.put("filePathFormat", config.getString("filePathFormat", SubnetPlotter.DEFAULT_FILE_PATH_FORMAT));
        root.put("fileSuffixFormat", config.getString("fileSuffixFormat", SubnetPlotter.DEFAULT_FILE_SUFFIX_FORMAT));
        root.put("selectedNetwork", config.getString("selectedNetwork"));
        root.put("version", Pensive.getVersion());
        
        try {
            initializeTemplateEngine();
        } catch (IOException e) {
            LOGGER.error("cannot write HTML");
        }

    }

    /**
     * Initialize FreeMarker
     * @throws IOException
     */
    protected void initializeTemplateEngine() throws IOException {
        cfg = new Configuration();
        cfg.setTemplateLoader(new ClassTemplateLoader(getClass(), "/gov/usgs/pensive/page"));
        DefaultObjectWrapper obj = new DefaultObjectWrapper();
        obj.setExposeFields(true);
        cfg.setObjectWrapper(obj);

        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
        cfg.setIncompatibleImprovements(new Version(2, 3, 20));
    }
    
    /**
     * Write my html page
     */
    public void writeHTML() {
        try {
            File pRoot = new File(pathRoot);
            if (!pRoot.exists())
                pRoot.mkdirs();
            
            Template template = cfg.getTemplate("pensive.html");
            String file = pathRoot + '/' + FILENAME;
            file.replace("/+", "/");
            file.replace("/", Matcher.quoteReplacement(File.separator));
            FileWriter fw = new FileWriter(file);
            template.process(root, fw);
            fw.close();
        } catch (IOException e) {
            LOGGER.error(e.getLocalizedMessage());
        } catch (TemplateException e) {
            LOGGER.error(e.getLocalizedMessage());
        }
    }

    /** add a subnet to a network list */
    public void addSubnet(String network, String subnet) {
    	List<String> s = subnets.get(network);
    	if (s == null) {
    		s = new ArrayList<String>();
    		subnets.put(network, s);
    	}
        s.add(subnet);
    }
}
