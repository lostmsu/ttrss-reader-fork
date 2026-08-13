{ pkgs ? import <nixpkgs> {
    config = {
      android_sdk.accept_license = true;
      allowUnfree = true;
    };
  }
}:

let
  buildToolsVersion = "34.0.0";

  androidComposition = pkgs.androidenv.composeAndroidPackages {
    buildToolsVersions = [ buildToolsVersion ];
    platformVersions = [ "34" ];
    includeEmulator = false;
    includeNDK = false;
    includeSystemImages = false;
  };

  androidSdk = androidComposition.androidsdk;
  androidSdkRoot = "${androidSdk}/libexec/android-sdk";
in
pkgs.mkShell {
  packages = [
    androidSdk
    pkgs.jdk17
  ];

  ANDROID_HOME = androidSdkRoot;
  ANDROID_SDK_ROOT = androidSdkRoot;
  JAVA_HOME = pkgs.jdk17.home;

  shellHook = ''
    export GRADLE_OPTS="''${GRADLE_OPTS:+$GRADLE_OPTS }-Dorg.gradle.project.android.aapt2FromMavenOverride=${androidSdkRoot}/build-tools/${buildToolsVersion}/aapt2"
  '';
}
