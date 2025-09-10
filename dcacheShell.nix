{pkgs ? import <nixpkgs> {}}:
pkgs.mkShell {
  packages = with pkgs; [
    maven
    rpm
    bash
    ruby
    openssl
    jdk17
    apacheHttpd
    zsh # Not needed for dcache. But i like zsh
  ];
  shellHook =
    /*
    bash
    */
    ''
      zsh
      exit
    '';
}
