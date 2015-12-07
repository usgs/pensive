# Pensive

[![Build Status](https://travis-ci.org/usgs/pensive.png)](https://travis-ci.org/usgs/pensive)

A Java application designed to allow visualizing, in near real-time, spectral content of continuous seismic waveforms from a number of sensors deployed on active volcanoes. The application was designed with ease of use in mind. Pensive strives to use sensible defaults in all areas while proving an expressive configuration file to permit fine-grained control when needed.

## Requirements
  * Java SE runtime. Pensive has been tested with Java virtual machines from Oracle, OpenJDK, and Apple. Any JVM that supports Java 7 or later should do the trick.
  * Source of wave data. Pensive can request data using either the Winston wave server protocol or the Earthworm WaveServerV protocol.