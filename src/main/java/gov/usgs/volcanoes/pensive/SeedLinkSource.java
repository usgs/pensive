package gov.usgs.volcanoes.pensive;

import gov.usgs.volcanoes.core.data.HelicorderData;
import gov.usgs.volcanoes.core.data.Scnl;
import gov.usgs.volcanoes.core.data.Wave;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.TimeSpan;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.swarm.data.GulperListener;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;

import java.io.File;
import java.util.List;

public class SeedLinkSource extends SeismicDataSource {

  private SeedLinkClient seedLinkClient;
  private String host;
  private int port;

  public SeedLinkSource(String host, int port) {
    super();
    seedLinkClient = new SeedLinkClient(host, port);
  }

  @Override
  public List<String> getChannels() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Wave getWave(String station, double t1, double t2) {
    Scnl scnl;
    try {
      scnl = Scnl.parse(station, " ");
    } catch (UtilException e) {
      e.printStackTrace();
      return null;
    }
    TimeSpan timeSpan = new TimeSpan(J2kSec.asEpoch(t1), J2kSec.asEpoch(t2));

    return seedLinkClient.getWave(scnl, timeSpan);
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
  public void parse(String params) {
    throw new UnsupportedOperationException();    
  }

}
