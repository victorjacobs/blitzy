{
  lib,
  stdenv,
  gradle_9,
  jdk17_headless,
  makeWrapper,
}:

stdenv.mkDerivation (finalAttrs: {
  pname = "blitzy";
  version = "1.0-SNAPSHOT";

  src = lib.fileset.toSource {
    root = ./..;
    fileset = lib.fileset.unions [
      (lib.fileset.maybeMissing ../.editorconfig)
      ../build.gradle.kts
      ../gradle.properties
      ../settings.gradle
      ../src
    ];
  };

  mitmCache = gradle_9.fetchDeps {
    pkg = finalAttrs.finalPackage;
    data = ./deps.json;
  };

  nativeBuildInputs = [
    gradle_9
    makeWrapper
  ];

  __darwinAllowLocalNetworking = true;

  gradleFlags = [ "-Dorg.gradle.java.home=${jdk17_headless}" ];
  gradleBuildTask = "shadowJar";

  doCheck = true;

  installPhase = ''
    runHook preInstall

    install -Dm644 build/libs/blitzy-1.0-SNAPSHOT-all.jar $out/share/blitzy/blitzy.jar
    makeWrapper ${jdk17_headless}/bin/java $out/bin/blitzy \
      --add-flags "-Xmx256M" \
      --add-flags "-jar $out/share/blitzy/blitzy.jar"

    runHook postInstall
  '';

  meta = {
    description = "Cluster Blitzortung lightning strikes and expose them as GeoJSON";
    homepage = "https://github.com/victorjacobs/blitzy";
    mainProgram = "blitzy";
    platforms = lib.platforms.unix;
  };
})
