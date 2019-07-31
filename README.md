# Blitzy

[![Docker Cloud Build Status](https://img.shields.io/docker/cloud/build/vjacobs/blitzy.svg)](https://hub.docker.com/r/vjacobs/blitzy)

Connects to [blitzortung.org](http://blitzortung.org) API, clusters lighting strikes using *🌈data mining🌈* and exposes the clusters through a GeoJSON endpoint.

## Building

```bash
./gradlew shadowJar
```

## Running

```bash
docker run -p 8080:8080 vjacobs/blitzy
```

The GeoJSON file is now available at [localhost:8080/blitzortung.geojson](http://localhost:8080/blitzortung.geojson).

### Configuration

Configuration is done through the following environment variables:

* `TOP_LEFT_COORDINATE` and `BOTTOM_RIGHT_COORDINATE`: Top left and bottom right coordinates of the area monitored. Defaults to roughly Europe. Format of both variables is `lat,lon`. E.g. `63.14,-18.11`.
* `CLUSTERING_INTERVAL` Time between clustering runs in milliseconds, defaults to 1 minute.
* `LIGHTNING_STRIKE_TTL` TTL (milliseconds) for lightning strikes, how long are they kept in memory. Defaults to 10 minutes.
* `CLUSTERING_EPS` Epsilon for DBSCAN (the clustering algorithm). Distance between lightning strikes (in meters) to consider them part of the same cluster. Defaults to 10000.0m.
* `CLUSTERING_MIN_PTS` Minpts for DBSCAN. Minimum number of lightning strikes required to consider it a cluster. Defaults to 25.
