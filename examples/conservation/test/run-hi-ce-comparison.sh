#!/usr/bin/env bash

# To run in GAMS demo mode, use:
# bash -c "export NUMAGENTS=10 && export PACKAGES=5 && ./run-hilo-ce.sh"

set -e  # exit of failures

#-------------------------------------------------------------------------
# Start USER CONFIG
#-------------------------------------------------------------------------

NUMAGENTS=${NUMAGENTS:-100} # 100 agents, or set to 10 if using GAMS in demo mode
PACKAGES=${PACKAGES:-26} # 26 packages, or set to 5 if using GAMS in demo mode
REPEATS=${REPEATS:-20}
CYCLES=${CYCLES:-30}
TARGET_PERCENTAGE=${TARGET_PERCENTAGE:-12}
SIGMOID_MAX_STEP_X=${SIGMOID_MAX_STEP_X:-20}
PROFIT_MOTIVE_UPDATE_MULTIPLIER=${PROFIT_MOTIVE_UPDATE_MULTIPLIER:-0.1}
SOCIAL_NORM_UPDATE_MULTIPLIER=${SOCIAL_NORM_UPDATE_MULTIPLIER:-0.1}
#-------------------------------------------------------------------------
# End USER CONFIG
#-------------------------------------------------------------------------



#-------------------------------------------------------------------------
realpath() {
	[[ $1 = /* ]] && echo "$1" || echo "$PWD/${1#./}"
}

run() {
OUTDIR=$1
rm -rf $OUTDIR
mkdir $OUTDIR

# Create the config file which is also needed by post processing scripts
cat << EOF > $OUTDIR/samples.txt
$NUMAGENTS	$HI_CE_AGENTS_PERCENTAGE	$TARGET_PERCENTAGE	$SIGMOID_MAX_STEP_X
EOF

cat << EOF > $OUTDIR/config
SAMPLES=1
REPLICATES=$REPEATS
EOF
. $OUTDIR/config

sample=1
cat $OUTDIR/samples.txt | while read x; do
	for ((replicate=1; replicate<=REPLICATES; replicate++)); do
 		dst="$OUTDIR/log/archive-$sample-$replicate"
		mkdir -p "$dst"
		CMD="cd $dst && NUMPACKAGES=$PACKAGES && CYCLES=$CYCLES && . $PBS_O_WORKDIR/model.sh $NUMAGENTS $HI_CE_AGENTS_PERCENTAGE $TARGET_PERCENTAGE $SIGMOID_MAX_STEP_X $PROFIT_MOTIVE_UPDATE_MULTIPLIER $SOCIAL_NORM_UPDATE_MULTIPLIER $replicate > model.out"
		echo $CMD
		eval $CMD
		rm -f _gams*
		rm -f conservation.in.*csv
		rm -f conservation.out.*csv
	done
	sample=$(expr $sample + 1)
done
}
#-------------------------------------------------------------------------


#-------------------------------------------------------------------------
# START HERE
#-------------------------------------------------------------------------

DIR=`dirname "$0"`
PBS_O_WORKDIR=`realpath $DIR`
export PBS_O_WORKDIR=$PBS_O_WORKDIR


HI_CE_AGENTS_PERCENTAGE=25 && run $PBS_O_WORKDIR/`basename "$0"`.output.hice25
HI_CE_AGENTS_PERCENTAGE=75 && run $PBS_O_WORKDIR/`basename "$0"`.output.hice75

