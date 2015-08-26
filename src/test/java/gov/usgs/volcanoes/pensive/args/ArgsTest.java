package gov.usgs.volcanoes.pensive.args;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.martiansoftware.jsap.JSAPException;

import gov.usgs.volcanoes.pensive.PensiveArgs;

public class ArgsTest {

    private static final String START_DATE = "201510110000";
    private static final String END_DATE = "201510120000";

    PensiveArgs args;
    String[] commandLineArgs = { "-s", START_DATE, "-e", END_DATE };
    SimpleDateFormat format;

    @Before
    public void setup() throws JSAPException {

        args = new PensiveArgs(commandLineArgs);
        format = new SimpleDateFormat(PensiveArgs.INPUT_TIME_FORMAT);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Test
    public void startTimeArg() throws ParseException {
        long givenTime = format.parse(START_DATE).getTime();
        long derivedTime = args.startTime.getTime();

        assertEquals(derivedTime, givenTime);
    }

    @Test
    public void endTimeArg() throws ParseException {
        long givenTime = format.parse(END_DATE).getTime();
        long derivedTime = args.endTime.getTime();

        assertEquals(derivedTime, givenTime);
    }

}
