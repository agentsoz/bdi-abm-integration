#!/bin/bash

DIR=`dirname "$0"`

### Begin user config

# SIMODD repository
SIMODD_BDI_DIR=
#SIMODD_BDI_DIR=${DIR}/../../../../simodd-bdi

MAVEN_REPO=
#MAVEN_REPO=~/.m2/repository

### End user config

#JAR_BDIABM=${DIR}/../../../integrations/bdi-sim/bdiabm.jar
#JAR_ABMJACK=${DIR}/../../../integrations/abm-jack/abmjack.jar
#JAR_ABMJACK_GENACT=${DIR}/../../../integrations/abm-jack/abmjack-genact.jar
#JAR_JACK=${DIR}/../../../integrations/abm-jack/dep/jack.jar
#JAR_VACCINATION=${DIR}/../vaccination.jar

JAR_BDIABM=${MAVEN_REPO}/agentsoz/bdi-abm/0.0.1-SNAPSHOT/bdi-abm-0.0.1-SNAPSHOT.jar
JAR_ABMJACK=${MAVEN_REPO}/agentsoz/abm-jack/0.0.1-SNAPSHOT/abm-jack-0.0.1-SNAPSHOT.jar
JAR_JACK=${MAVEN_REPO}/com/aosgrp/jack/5.6/jack-5.6.jar
JAR_VACCINATION=${MAVEN_REPO}/agentsoz/vaccination/0.0.1-SNAPSHOT/vaccination-0.0.1-SNAPSHOT.jar


CP=${JAR_BDIABM}:${JAR_ABMJACK}:${JAR_JACK}:${JAR_VACCINATION}

if [ "$SIMODD_BDI_DIR" == "" ]; then
	echo "Please set the script variable SIMODD_BDI_DIR to point to the root of your simodd_bdi installation"
	exit
fi
if [ "$MAVEN_REPO" == "" ]; then
        echo "Please set the script variable MAVEN_REPO to point to your local Maven repository (e.g., ~/.m2/repository)"
        exit
fi

# Launch the ABM
echo "Starting the simodd_bdi ABM system"
( cd ${SIMODD_BDI_DIR}/test_bdi && python main.py -v& )
echo "Waiting for connection to be up (2 second pause)"
sleep 2

# Connect the BDI system
CMD="java -cp ${CP} agentsoz.vaccination.Program -h"
echo $CMD; $CMD
CMD="java -cp ${CP} agentsoz.vaccination.Program -host localhost -port 2653"
echo "Started at " `date`
echo $CMD; $CMD
echo "Finished at" `date`


