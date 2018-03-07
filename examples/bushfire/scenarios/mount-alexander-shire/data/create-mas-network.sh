#!/bin/bash

###
# Script to generate a new MATSim network for Mount Alexander Shire.
# Author: Dhirendra Singh, 7/Feb/18
#
# All files are saved relative to the directory of this script.
# Steps:
# 1. Download and install the latest osmosis binary if not already there
# 2. Download and unzip the latest map of Australia (OSM)
# 3. Create a detailed map of the Shire
# 4. Create a larger map of big roads to the major nearby cities
# 5. Combine the two into a final OSM map
# 6. Generate the MATSim network from the above
#
###

DIR=$(dirname "$0")

# download the latest osmosis binary if needed
OSMOSIS_ZIP=osmosis-latest.tgz
OSMOSIS_DIR=$DIR/osmosis
OSMOSIS_EXE=$DIR/osmosis/bin/osmosis
OSMOSIS_WEB=https://bretth.dev.openstreetmap.org/osmosis-build/${OSMOSIS_ZIP}
if [ ! -d $OSMOSIS_DIR ] ; then
  cd $DIR
  printf "\nGetting $OSMOSIS_WEB ...\n\n"
  wget -O $OSMOSIS_ZIP $OSMOSIS_WEB
  mkdir $OSMOSIS_DIR
  mv $OSMOSIS_ZIP $OSMOSIS_DIR
  cd $OSMOSIS_DIR
  tar xvfz $OSMOSIS_ZIP
  rm -f OSMOSIS_ZIP
  chmod a+x bin/osmosis
  printf "\nInstalled latest osmosis in $OSMOSIS_EXE\n\n"
  cd -
fi

# download the latest OSM extract for Australia if needed from:
# http://download.gisgraphy.com/openstreetmap/pbf/AU.tar.bz2
AU_PBF=AU.pbf
OSM_ZIP=AU.tar.bz2
OSM_WEB=http://download.gisgraphy.com/openstreetmap/pbf/$OSM_ZIP
if [ ! -f $DIR/$AU_PBF ] ; then
  cd $DIR
  printf "\nGetting $OSM_WEB ...\n\n"
  #wget -O $OSM_ZIP http://download.gisgraphy.com/openstreetmap/pbf/$OSM_ZIP
  printf "\nExtracting PBF from archive...\n\n"
  tar -jxvf $DIR/$OSM_ZIP
  mv AU $AU_PBF
  cd -
fi

# Generate the detailed map for the Shire and nearby areas
if [ ! -f $DIR/.mas-allroads.pbf ] ; then
  printf "\nExtracting detailed map for Mount Alexander Shire and nearby areas...\n\n"
  $OSMOSIS_EXE --rb file=$DIR/$AU_PBF \
    --bounding-box top=-36.7224 left=143.6957 bottom=-37.2697 right=144.4839 \
    completeWays=true --used-node --tf accept-ways \
    highway=motorway,motorway_link,trunk,trunk_link,primary,primary_link,secondary,secondary_link,tertiary,tertiary_link,residential,unclassified  \
    --wb $DIR/.mas-allroads.pbf
fi

# Generate a larger map of all the big roads to major cities
if [ ! -f $DIR/.mas-bigroads.pbf ] ; then
  printf "\nExtracting larger map of all the big roads to major cities...\n\n"
  $OSMOSIS_EXE --rb file=$DIR/$AU_PBF \
    --bounding-box top=-33.934 left=140.691 bottom=-37.884 right=149.381 \
    completeWays=true --used-node --tf accept-ways \
    highway=motorway,motorway_link,trunk,trunk_link \
    --wb $DIR/.mas-bigroads.pbf
fi

# Merge the two into a final map
if [ ! -f $DIR/.mas-merged-network.osm ] ; then
printf "\nMerging the two into a final map...\n\n"
$OSMOSIS_EXE --rb file=$DIR/.mas-allroads.pbf --rb file=$DIR/.mas-bigroads.pbf --merge \
  --wx $DIR/.mas-merged-network.osm
  cp $DIR/.mas-merged-network.osm $DIR/mount_alexander_shire_network.osm

fi

# Generate the MATSim network from the final map
printf "\nCreating the final MATSim network...\n\n"
cp $DIR/.mas-merged-network.osm $DIR/../../..
cd $DIR/../../..
mvn exec:java -Dexec.mainClass="io.github.agentsoz.util.NetworkGenerator" \
  -Dexec.args="-i .mas-merged-network.osm -o mount_alexander_shire_network.xml -wkt EPSG:28355"
cd -
rm -f $DIR/../../../.mas-merged-network.osm
mv $DIR/../../../mount_alexander_shire_network.xml $DIR
printf "\nAll done. New Mount Alexander Shire network is in $DIR/mount_alexander_shire_network.{xml,osm}\n\n"
