{
  config,
  lib,
  pkgs,
  ...
}:

let
  cfg = config.services.blitzy;
in
{
  options.services.blitzy = {
    enable = lib.mkEnableOption "Blitzy lightning strike clustering service";

    package = lib.mkOption {
      type = lib.types.package;
      default = pkgs.callPackage ./package.nix { };
      defaultText = lib.literalExpression "pkgs.callPackage ./nix/package.nix { }";
      description = "The Blitzy package to run.";
    };

    openFirewall = lib.mkOption {
      type = lib.types.bool;
      default = false;
      description = "Whether to open the service port in the firewall.";
    };

    settings = {
      topLeftCoordinate = lib.mkOption {
        type = lib.types.str;
        default = "63.14,-18.11";
        description = "Top-left coordinate of the monitored area as latitude,longitude.";
      };

      bottomRightCoordinate = lib.mkOption {
        type = lib.types.str;
        default = "30.54,22.89";
        description = "Bottom-right coordinate of the monitored area as latitude,longitude.";
      };

      clusteringInterval = lib.mkOption {
        type = lib.types.ints.positive;
        default = 60 * 1000;
        description = "Time between clustering runs in milliseconds.";
      };

      lightningStrikeTtl = lib.mkOption {
        type = lib.types.ints.positive;
        default = 10 * 60 * 1000;
        description = "Time in milliseconds to retain lightning strikes.";
      };

      clusteringEps = lib.mkOption {
        type = lib.types.addCheck lib.types.float (value: value > 0);
        default = 10000.0;
        description = "DBSCAN epsilon distance in metres.";
      };

      clusteringMinPts = lib.mkOption {
        type = lib.types.ints.positive;
        default = 25;
        description = "Minimum number of lightning strikes in a DBSCAN cluster.";
      };
    };
  };

  config = lib.mkIf cfg.enable {
    networking.firewall.allowedTCPPorts = lib.optionals cfg.openFirewall [ 8080 ];

    systemd.services.blitzy = {
      description = "Blitzy lightning strike clustering service";
      wantedBy = [ "multi-user.target" ];
      wants = [ "network-online.target" ];
      after = [ "network-online.target" ];

      environment = {
        TOP_LEFT_COORDINATE = cfg.settings.topLeftCoordinate;
        BOTTOM_RIGHT_COORDINATE = cfg.settings.bottomRightCoordinate;
        CLUSTERING_INTERVAL = toString cfg.settings.clusteringInterval;
        LIGHTNING_STRIKE_TTL = toString cfg.settings.lightningStrikeTtl;
        CLUSTERING_EPS = toString cfg.settings.clusteringEps;
        CLUSTERING_MIN_PTS = toString cfg.settings.clusteringMinPts;
      };

      serviceConfig = {
        DynamicUser = true;
        ExecStart = lib.getExe cfg.package;
        Restart = "on-failure";

        CapabilityBoundingSet = "";
        LockPersonality = true;
        NoNewPrivileges = true;
        PrivateDevices = true;
        PrivateTmp = true;
        ProtectClock = true;
        ProtectControlGroups = true;
        ProtectHome = true;
        ProtectHostname = true;
        ProtectKernelLogs = true;
        ProtectKernelModules = true;
        ProtectKernelTunables = true;
        ProtectSystem = "strict";
        RestrictAddressFamilies = [
          "AF_INET"
          "AF_INET6"
          "AF_UNIX"
        ];
        RestrictNamespaces = true;
        RestrictRealtime = true;
        SystemCallArchitectures = "native";
        UMask = "0077";
      };
    };
  };
}
