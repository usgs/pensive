package gov.usgs.volcanoes.pensive;

import edu.iris.dmc.seedcodec.Codec;
import edu.iris.dmc.seedcodec.CodecException;
import edu.iris.dmc.seedcodec.DecompressedData;
import edu.iris.dmc.seedcodec.UnsupportedCompressionType;
import edu.sc.seis.seisFile.ChannelTimeWindow;
import edu.sc.seis.seisFile.SeisFileException;
import edu.sc.seis.seisFile.fdsnws.FDSNDataSelectQuerier;
import edu.sc.seis.seisFile.fdsnws.FDSNDataSelectQueryParams;
import edu.sc.seis.seisFile.fdsnws.FDSNWSException;
import edu.sc.seis.seisFile.mseed.Blockette;
import edu.sc.seis.seisFile.mseed.Blockette1000;
import edu.sc.seis.seisFile.mseed.Btime;
import edu.sc.seis.seisFile.mseed.DataHeader;
import edu.sc.seis.seisFile.mseed.DataRecord;
import edu.sc.seis.seisFile.mseed.DataRecordIterator;
import edu.sc.seis.seisFile.mseed.SeedFormatException;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.core.data.HelicorderData;
import gov.usgs.volcanoes.core.data.Wave;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.swarm.data.GulperListener;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class FdsnwsSource extends SeismicDataSource {

  private final String dataselectUri;
  private final String user;
  private final String password;

  public FdsnwsSource(final ConfigFile config) {
    this.dataselectUri = config.getString("dataselectUrl");
    this.user = config.getString("user", null);
    this.password = config.getString("password", null);
  }


  @Override
  public List<String> getChannels() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void parse(String params) {
    throw new UnsupportedOperationException();
  }

  private FDSNDataSelectQueryParams configureParamsOld(String station, double t1, double t2) {
    System.out.println("Station: " + station);
    String[] parts = station.split("\\s+");
    FDSNDataSelectQueryParams queryParams = new FDSNDataSelectQueryParams();
    queryParams.setStartTime(J2kSec.asDate(t1));
    queryParams.setEndTime(J2kSec.asDate(t2));
    queryParams.appendToNetwork(parts[2]);
    queryParams.appendToStation(parts[0]);
    queryParams.appendToChannel(parts[1]);
    if (parts.length > 3) {
      queryParams.appendToLocation(parts[3]);
    }

    URI uri;
    try {
      uri = new URI(dataselectUri);
      queryParams.setScheme(uri.getScheme());
      queryParams.setPort(uri.getPort());
      queryParams.setFdsnwsPath(uri.getPath());
      queryParams.setHost(uri.getHost());
      System.out.println(queryParams.formURI());
    } catch (URISyntaxException e) {
      throw new RuntimeException(
          "Cannot parse URI " + dataselectUri + ": " + e.getLocalizedMessage());
    }

    return queryParams;
  }

  private FDSNDataSelectQueryParams configureParams() {
    FDSNDataSelectQueryParams queryParams = new FDSNDataSelectQueryParams();

    URI uri;
    try {
      uri = new URI(dataselectUri);
      queryParams.setScheme(uri.getScheme());
      queryParams.setPort(uri.getPort());
      queryParams.setFdsnwsPath(uri.getPath());
      queryParams.setHost(uri.getHost());
      System.out.println(queryParams.formURI());
    } catch (URISyntaxException e) {
      throw new RuntimeException(
          "Cannot parse URI " + dataselectUri + ": " + e.getLocalizedMessage());
    }

    return queryParams;
  }

  private static double btimeToJ2k(Btime btime) {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    cal.set(Calendar.YEAR, btime.getYear());
    cal.set(Calendar.DAY_OF_YEAR, btime.getDayOfYear());
    cal.set(Calendar.HOUR_OF_DAY, btime.getHour());
    cal.set(Calendar.MINUTE, btime.getMin());
    cal.set(Calendar.SECOND, btime.getSec());
    cal.set(Calendar.MILLISECOND, btime.getTenthMilli() / 10);
    Date date = cal.getTime();

    return J2kSec.fromDate(date);
  }

  private static Wave createWave(DataRecord dr, Blockette1000 b1000)
      throws UnsupportedCompressionType, CodecException {
    DecompressedData decomp =
        new Codec().decompress(b1000.getEncodingFormat(), dr.getData(),
            dr.getHeader().getNumSamples(), b1000.getWordOrder() == 0);

    Wave wave = new Wave();
    DataHeader dh = dr.getHeader();
    wave.setSamplingRate(dh.getSampleRate());
    wave.setStartTime(btimeToJ2k(dh.getStartBtime()));
    wave.buffer = decomp.getAsInt();
    wave.register();
    return wave;
  }

  private Wave readResponse(DataRecordIterator it)
      throws SeedFormatException, IOException, UnsupportedCompressionType, CodecException {
    List<Wave> waves = new ArrayList<Wave>();
    if (!it.hasNext()) {
      System.out.println("No Data");
    }
    while (it.hasNext()) {
      DataRecord dr = it.next();
      for (Blockette blockette : dr.getBlockettes(1000)) {
        if (blockette instanceof Blockette1000) {
          waves.add(createWave(dr, (Blockette1000) blockette));
        }
      }

    }
    return Wave.join(waves);
  }


  @Override
  public Wave getWave(String station, double t1, double t2) {
    FDSNDataSelectQueryParams queryParams = configureParams();
    String[] parts = station.split("\\s+");
    String sta = parts[0];
    String cha = parts[1];
    String net = parts[2];
    String loc = "";
    if (parts.length > 3) {
      loc = parts[3];
    }
    
    ChannelTimeWindow channelTime = new ChannelTimeWindow(net, sta, loc, cha, J2kSec.asDate(t1),  J2kSec.asDate(t2));
    List<ChannelTimeWindow> request = new ArrayList<ChannelTimeWindow>();
    request.add(channelTime);
    FDSNDataSelectQuerier querier = new FDSNDataSelectQuerier(queryParams, request);
    if (user != null) {
      querier.enableRestrictedData(user, password);
    }   

    DataRecordIterator it = null;
    Wave wave = null;
    try {
      it = querier.getDataRecordIterator();
      wave = readResponse(it);
    } catch (FDSNWSException e) {
      System.out.println("No data found for " + station);
      e.printStackTrace();
    } catch (SeisFileException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (UnsupportedCompressionType e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (CodecException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } finally {
      if (it != null) {
        it.close();
      }
    }

    return wave;

  }

  @Override
  public HelicorderData getHelicorder(String station, double t1, double t2, GulperListener gl) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toConfigString() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() {
    // do nothing
  }
}
