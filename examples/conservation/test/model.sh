#!/bin/sh
# model wrapper
# usage: model.sh sample..


### Begin user config
NUMAGENTS=${1} # 100 agents, or set to 10 if using GAMS in demo mode
highCEAgentsPercentage=${2}
targetPercentage=${3}
sigmoidMaxStepX=${4}
profitMotiveUpdateMultiplier=${5}
socialNormUpdateMultiplier=${6} 
GLOBAL_SEED=${7}
NUMPACKAGES=${NUMPACKAGES:-26} # 26 packages, or set to 5 if running GAMS in demo mode
CYCLES=${CYCLES:-30}

# Set the following variable to point to the root of your GAMS installation, for instance,
GAMS_DIR=
if [ -d /Applications/GAMS/gams24.4_osx_x64_64_sfx ]; then
    GAMS_DIR=/Applications/GAMS/gams24.4_osx_x64_64_sfx
elif [ -d ~/bin/gams ]; then
    GAMS_DIR=~/bin/gams
fi

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

CMD="java -cp ${CP} io.github.agentsoz.conservation.Main -gams_dir ${GAMS_DIR} -gams_model ${GAMS_MODEL} -r 1 -c $CYCLES -p ${NUMPACKAGES} -a ${NUMAGENTS} -profitDifferenctial 40 -profitVariability 20 -defaultMaxNumberOfBids 8 -bidAddon 5 -highCEAgentsPercentage ${highCEAgentsPercentage} -targetPercentage ${targetPercentage} -high_participation_prob 0.8 -low_participation_prob 0.3 -globalRandomSeed ${GLOBAL_SEED} -log_level ${LOG_LEVEL} -visitPolicy NEVER -sigmoidMaxStepX ${sigmoidMaxStepX} -socialNormUpdateMultiplier ${socialNormUpdateMultiplier} -profitMotiveUpdateMultiplier ${profitMotiveUpdateMultiplier} -- --config ${CFG}"

echo "Started at " `date`
echo $CMD; eval $CMD
echo "Finished at" `date`
