# Blitzy

Connects to [blitzortung.org](http://blitzortung.org) API, clusters lighting strikes using *🌈data mining🌈* and exposes the clusters through a GeoJSON endpoint.

## Building

```bash
./gradlew shadowJar
```

With Nix:

```bash
nix build
```

## Running

```bash
docker run -p 8080:8080 vjacobs/blitzy
```

The GeoJSON file is now available at [localhost:8080/blitzortung.geojson](http://localhost:8080/blitzortung.geojson).

Prometheus metrics are exposed at [localhost:8080/metrics](http://localhost:8080/metrics). They include current and total lightning strike counts, tracked cluster counts and sizes, clustering duration and failures, discarded messages, and WebSocket health. A minimal scrape configuration is:

```yaml
scrape_configs:
  - job_name: blitzy
    static_configs:
      - targets: [localhost:8080]
```

### NixOS

Add the flake module to your NixOS configuration and enable the service:

```nix
{
  inputs.blitzy.url = "github:victorjacobs/blitzy";

  outputs = { nixpkgs, blitzy, ... }: {
    nixosConfigurations.my-host = nixpkgs.lib.nixosSystem {
      system = "x86_64-linux";
      modules = [
        blitzy.nixosModules.default
        {
          services.blitzy = {
            enable = true;
            listenAddress = "0.0.0.0";
            listenPort = 8080;
            openFirewall = true;
          };
        }
      ];
    };
  };
}
```

### Configuration

Configuration is done through the following environment variables:

* `LISTEN_ADDRESS` Address on which the HTTP server listens. Defaults to `0.0.0.0`.
* `LISTEN_PORT` Port on which the HTTP server listens. Defaults to `8080`.
* `TOP_LEFT_COORDINATE` and `BOTTOM_RIGHT_COORDINATE`: Top left and bottom right coordinates of the area monitored. Defaults to roughly Europe. Format of both variables is `lat,lon`. E.g. `63.14,-18.11`.
* `CLUSTERING_INTERVAL` Time between clustering runs in milliseconds, defaults to 1 minute.
* `LIGHTNING_STRIKE_TTL` TTL (milliseconds) for lightning strikes, how long are they kept in memory. Defaults to 10 minutes.
* `CLUSTERING_EPS` Epsilon for DBSCAN (the clustering algorithm). Distance between lightning strikes (in meters) to consider them part of the same cluster. Defaults to 10000.0m.
* `CLUSTERING_MIN_PTS` Minpts for DBSCAN. Minimum number of lightning strikes required to consider it a cluster. Defaults to 25.
