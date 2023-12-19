#!/bin/sh

. /init-grid-ui.sh

xrdcp -f /etc/profile xroot://store-door-svc:1095//data/testfile

for i in `seq 1 1000`
do
  echo "File Nr. $i"
	xrdcp -f xroot://store-door-svc:1095//data/testfile /dev/null
done


#
# Overwrite test
#
gfal-copy -f /etc/profile root://store-door-svc:1095//data/file1
gfal-ls -l root://store-door-svc:1095//data/file1

gfal-copy -f /etc/profile root://store-door-svc:1095//data/file1
gfal-ls -l root://store-door-svc:1095//data/file1