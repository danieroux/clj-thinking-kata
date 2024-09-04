{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-24.05";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils } @ inputs:

    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };
      in
      rec {
        formatter = pkgs.nixpkgs-fmt;

        devShells.default = pkgs.mkShellNoCC {
          shellHook = ''
            echo
            just --list --unsorted
            echo
            echo "Start with:"
            echo
            echo just repl
          '';

          packages = [ pkgs.clojure pkgs.just ];
        };
      });
}
