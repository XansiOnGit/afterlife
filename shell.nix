{ pkgs ? import <nixpkgs> {} }:
pkgs.mkShell {
  packages = [ pkgs.jdk25 ];
  LD_LIBRARY_PATH = "/run/opengl-driver/lib:" + pkgs.lib.makeLibraryPath (with pkgs; [
    libGL glfw openal libpulseaudio libxkbcommon wayland vulkan-loader udev
    libx11 libxcursor libxext libxrandr libxxf86vm libxi libxrender libxtst
  ]);
}
