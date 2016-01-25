#!/bin/bash

DIR=`dirname "$0"`

### Begin user config

# logging verbosity (one of ERROR, WARN, INFO, DEBUG, TRACE)
LOG_LEVEL=INFO

# Set the following variable to point to the root of your GAMS installation, for instance,
GAMS_DIR=
# GAMS_DIR=/Applications/GAMS/gams24.4_osx_x64_64_sfx
 GAMS_DIR=/home/sewwandi/Documents/Apps/gams/gams24.4_linux_x64_64_sfx
# GAMS_DIR=~/bin/gams

### End user config

GAMS_JAR=${GAMS_DIR}/apifiles/Java/api/GAMSJavaAPI.jar
GAMS_MODEL=${DIR}/../target/classes/gams/bid_selection_model.gms
JAR_CONSERVATION=${DIR}/../target/conservation-ethics-1.0.0-jar-with-dependencies.jar

CP=${JAR_CONSERVATION}:${GAMS_JAR}

if [ "$GAMS_DIR" == "" ]; then
	echo "Please set the script variable GAMS_DIR to point to the root of your GAMS installation"
	exit
fi

NUMAGENTS=100
CFG='"{
programOutputFile : \"conservation.landholder.out\",
logFile : \"conservation.log\",
logLevel : '${LOG_LEVEL}',
agents:
 [
  {
   classname : io.github.agentsoz.conservation.jill.agents.Landholder, 
   args : [\"\"], 
   count: '${NUMAGENTS}'
  }
 ]
}"'

#CMD="java -cp ${CP} io.github.agentsoz.conservation.Main -gams_dir ${GAMS_DIR} -gams_model ${GAMS_MODEL} -r 1 -c 10 -p 26 -a ${NUMAGENTS} -log_level DEBUG --agent-class agentsoz.conservation.jill.agents.Landholder --num-agents 100 --outfile conservation.landholder.out --logfile conservation.log --debug-level DEBUG"

CMD="java -cp ${CP} io.github.agentsoz.conservation.Main -gams_dir ${GAMS_DIR} -gams_model ${GAMS_MODEL} -r 1 -c 10 -p 26 -a ${NUMAGENTS} -log_level DEBUG --config $CFG"

echo "Started at " `date`
echo $CMD; eval $CMD
echo "Finished at" `date`



