{
  description = "Lightning strike clustering service";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs =
    {
      self,
      nixpkgs,
      flake-utils,
    }:
    flake-utils.lib.eachDefaultSystem (
      system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
        blitzy = pkgs.callPackage ./nix/package.nix { };
      in
      {
        packages = {
          inherit blitzy;
          default = blitzy;
        };

        checks.default = blitzy;

        devShells.default = pkgs.mkShell {
          packages = [
            pkgs.jdk17
            pkgs.gradle_9
          ];

          shellHook = ''
            export JAVA_HOME=${pkgs.jdk17}
          '';
        };

        formatter = pkgs.nixfmt-tree;
      }
    )
    // {
      nixosModules = {
        blitzy =
          { lib, pkgs, ... }:
          {
            imports = [ ./nix/module.nix ];

            services.blitzy.package = lib.mkDefault self.packages.${pkgs.stdenv.hostPlatform.system}.blitzy;
          };
        default = self.nixosModules.blitzy;
      };
    };
}
