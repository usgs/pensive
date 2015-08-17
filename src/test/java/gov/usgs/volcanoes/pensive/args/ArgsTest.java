package gov.usgs.volcanoes.pensive.args;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.junit.Before;
import org.junit.Test;

import com.martiansoftware.jsap.JSAPException;

public class ArgsTest {

    private static final String START_DATE = "201510110000";
    private static final String END_DATE = "201510120000";

    Args args;
    String[] commandLineArgs = { "-s", START_DATE, "-e", END_DATE };
    SimpleDateFormat format;

    @Before
    public void setup() throws JSAPException {

        args = new Args(commandLineArgs);
        format = new SimpleDateFormat(DateStringParser.INPUT_TIME_FORMAT);
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
