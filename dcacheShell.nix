{pkgs ? import <nixpkgs> {}}:
pkgs.mkShell {
  packages = with pkgs; [
    maven
    bash
    # ruby
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
      # zsh -c 'oh-my-posh toggle os'
      # oh-my-posh toggle os
      # zsh 'oh-my-posh toggle os'
      zsh
      exit
    '';
}
