#!/bin/sh
# model wrapper
# usage: model.sh sample..


### Begin user config

# Set number of agents and number of packages
NUMAGENTS=100 # 100 agents, or set to 10 if using GAMS in demo mode
NUMPACKAGES=26 # 26 packages, or set to 5 if running GAMS in demo mode

# Set the following variable to point to the root of your GAMS installation, for instance,
GAMS_DIR=
# GAMS_DIR=/Applications/GAMS/gams24.4_osx_x64_64_sfx
# GAMS_DIR=/home/sewwandi/Documents/Apps/gams/gams24.4_linux_x64_64_sfx
# GAMS_DIR=~/bin/gams

# logging verbosity (one of ERROR, WARN, INFO, DEBUG, TRACE)
LOG_LEVEL=INFO

### End user config

GAMS_JAR=${GAMS_DIR}/apifiles/Java/api/GAMSJavaAPI.jar
GAMS_MODEL=${PBS_O_WORKDIR}/../target/classes/gams/bid_selection_model.gms
JAR_CONSERVATION=${PBS_O_WORKDIR}/../target/conservation-ethics-2.0.2-SNAPSHOT-jar-with-dependencies.jar

CP=${JAR_CONSERVATION}:${GAMS_JAR}

if [ "${GAMS_DIR}" = "" ] ; then
	echo "Please set the script variable GAMS_DIR to point to the root of your GAMS installation"
	exit
fi

CE_SEED_START_VALUE=1
PM_SEED_START_VALUE=9999
GLOBAL_SEED_START_VALUE=1111111

CE_SEED=$(expr ${5} + ${CE_SEED_START_VALUE})
PM_SEED=$(expr ${5} + ${PM_SEED_START_VALUE})
GLOBAL_SEED=$(expr ${5} + ${GLOBAL_SEED_START_VALUE})

CFG='"{
programOutputFile : \"conservation.landholder.out\",
logFile : \"conservation.log\",
logLevel : \"'${LOG_LEVEL}'\",
agents:
 [
  {
   classname : io.github.agentsoz.conservation.jill.agents.Landholder,
   args : [\"\"],
   count: '${NUMAGENTS}'
  }
 ]
}"'

CMD="java -cp ${CP} io.github.agentsoz.conservation.Main -gams_dir ${GAMS_DIR} -gams_model ${GAMS_MODEL} -r 1 -c 100 -p ${NUMPACKAGES} -a ${NUMAGENTS} -profitDifferenctial 40 -profitVariability 20 -defaultMaxNumberOfBids 8 -bidAddon 5 -conservationEthicModifier ${1} -profitMotivationModifier ${2} -socialNormUpdatePercentage ${3} -highCEAgentsPercentage ${4} -targetPercentage 12 -high_participation_prob 0.8 -low_participation_prob 0.3 -staticConservationEthicModifier 0.25 -staticProfitMotivationModifier 0.25 -conservationEthicSeed ${CE_SEED} -profitMotivationSeed ${PM_SEED} -globalRandomSeed ${GLOBAL_SEED} -log_level ${LOG_LEVEL} --config ${CFG}"

echo "Started at " `date`
echo $CMD; eval $CMD
echo "Finished at" `date`
