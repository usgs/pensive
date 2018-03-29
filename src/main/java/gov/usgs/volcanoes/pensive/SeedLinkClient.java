package gov.usgs.volcanoes.pensive;

import edu.iris.Fissures.seed.container.Blockette;
import edu.iris.Fissures.seed.container.Btime;
import edu.iris.Fissures.seed.container.Waveform;
import edu.iris.Fissures.seed.exception.SeedException;

import gov.usgs.volcanoes.core.data.Scnl;
import gov.usgs.volcanoes.core.data.Wave;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.Time;
import gov.usgs.volcanoes.core.time.TimeSpan;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import nl.knmi.orfeus.SLClient;
import nl.knmi.orfeus.seedlink.SLPacket;
import nl.knmi.orfeus.seedlink.SeedLinkException;

public class SeedLinkClient extends SLClient {

  static final String DATE_FORMAT = "yyyy,MM,dd,HH,mm,ss";

  private List<Wave> waveList;

  public SeedLinkClient(String host, int port) {
    super();
 
    slconn.setNetDelay(10);
    slconn.setNetTimout(30);
    slconn.setSLAddress(host + ":" + port);
  }

  public boolean packetHandler(int count, SLPacket slpack) throws Exception {
    System.err.println("TOMP SAYS: " + slpack);
    if (slpack == null || slpack == SLPacket.SLNOPACKET || slpack == SLPacket.SLERROR) {
      return false; // do not close the connection
    }

    // get basic packet info
    final int type = slpack.getType();

    // process INFO packets here
    // return if unterminated
    if (type == SLPacket.TYPE_SLINF) {
      return false; // do not close the connection
    }
    // process message and return if terminated
    if (type == SLPacket.TYPE_SLINFT) {
      // LOGGER.debug("received INFO packet:\n{}", slconn.getInfoString());
      if (infolevel != null) {
        return true; // close the connection
      } else {
        return false; // do not close the connection
      }
    }

    // if here, must be a blockette
    final Blockette blockette = slpack.getBlockette();
    final Waveform waveform = blockette.getWaveform();
    // if waveform and FSDH
    if (waveform != null && blockette.getType() == 999) {
      // convert waveform to wave (also done in
      // gov.usgs.swarm.data.FileDataSource)
      try {
        final Btime bTime = getBtime(blockette, 8);
        final double factor = getDouble(blockette, 10);
        final double multiplier = getDouble(blockette, 11);
        final double startTime = J2kSec.fromDate(btimeToDate(bTime));
        final double samplingRate = getSampleRate(factor, multiplier);
        final Wave wave = new Wave();
        wave.setSamplingRate(samplingRate);
        wave.setStartTime(startTime);
        wave.buffer = waveform.getDecodedIntegers();
        wave.register();
        System.err.println("TOMP SAYS: samp " + wave.numSamples());
        waveList.add(wave);
      } catch (Exception ex) {
        ex.printStackTrace();
        return true; // close the connection
      }
    }
    return false; // do not close the connection
  }

  /**
   * taken from swarm
   */

  private Date btimeToDate(Btime btime) {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    cal.set(Calendar.YEAR, btime.getYear());
    cal.set(Calendar.DAY_OF_YEAR, btime.getDayOfYear());
    cal.set(Calendar.HOUR_OF_DAY, btime.getHour());
    cal.set(Calendar.MINUTE, btime.getMinute());
    cal.set(Calendar.SECOND, btime.getSecond());
    cal.set(Calendar.MILLISECOND, btime.getTenthMill() / 10);
    return cal.getTime();
  }

  /**
   * taken from swarm
   */
  private static Btime getBtime(Blockette blockette, int fieldNum) throws SeedException {
    Object obj = blockette.getFieldVal(fieldNum);
    if (obj instanceof Btime) {
      return (Btime) obj;
    }
    return new Btime(obj.toString());
  }

  /**
   * taken from swarm
   */
  private static double getDouble(Blockette blockette, int fieldNum) throws SeedException {
    Object obj = blockette.getFieldVal(fieldNum);
    if (obj instanceof Number) {
      return ((Number) obj).doubleValue();
    }
    return Double.parseDouble(obj.toString());
  }

  /*
   * taken from Robert Casey's PDCC seed code.
   */
  private float getSampleRate(double factor, double multiplier) {
    float sampleRate = (float) 10000.0; // default (impossible) value;
    if ((factor * multiplier) != 0.0) { // in the case of log records
      sampleRate = (float) (java.lang.Math.pow(java.lang.Math.abs(factor),
          (factor / java.lang.Math.abs(factor)))
          * java.lang.Math.pow(java.lang.Math.abs(multiplier),
              (multiplier / java.lang.Math.abs(multiplier))));
    }
    return sampleRate;
  }

  public Wave getWave(Scnl scnl, TimeSpan timeSpan) {
    waveList = new ArrayList<Wave>();

    slconn.setBeginTime(Time.format(DATE_FORMAT, timeSpan.startTime));
    slconn.setEndTime(Time.format(DATE_FORMAT, timeSpan.endTime));

    String selector =
        String.format("%s_%s:%s%s.d", scnl.network, scnl.station, scnl.location, scnl.channel);
    multiselect = selector;

    System.err.println("TOMP SAYS: " + selector);
    try {
      edu.iris.Fissures.seed.container.BlocketteDecoratorFactory.reset();
      init();
      run();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    } catch (SeedLinkException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
    
//    slconn.disconnect();
//    slconn.terminate();

    return Wave.join(waveList);
  }

  /**
  *
  * Start this SLCient.
  *
  * @throws Exception
  * @throws SeedLinkException on error.
  *
  */
 public void run() throws Exception {
System.err.println("TOM SAYS RUNNING");
     if (infolevel != null) {
         slconn.requestInfo(infolevel);
     }

     // Loop with the connection manager
     SLPacket slpack = null;
     int count = 1;
     while ((slpack = slconn.collect()) != null) {
       System.err.println("TOM SAYS got packet");

         if (slpack == SLPacket.SLTERMINATE) {
             break;
         }

         try {

             // do something with packet
             boolean terminate = packetHandler(count, slpack);
             if (terminate) {
                 break;
             }

         } catch (SeedLinkException sle) {
             System.out.print(CLASS_NAME + ": " + sle);
         }

         // 20081127 AJL - test modification to prevent "Error: out of java heap space" problem identified by pwiejacz@igf.edu.pl
         if (count % 200 == 0) {
           edu.iris.Fissures.seed.container.BlocketteDecoratorFactory.reset();
         }
         if (count >= Integer.MAX_VALUE) {
             count = 1;
             System.out.println("DEBUG INFO: " + CLASS_NAME + ": Packet count reset to 1");
         } else {
             count++;
         }
     }
System.err.println("TOMP SAYS DONE");
     // Close the SeedLinkConnection
//     slconn.close();

 }

}
