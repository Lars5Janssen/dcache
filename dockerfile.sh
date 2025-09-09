#/usr/bin/env bash
docker kill dcbuild
docker rm dcbuild
docker build -t dcachebuildcon . 
docker run --name dcbuild dcachebuildcon:latest
