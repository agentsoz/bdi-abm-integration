#!/usr/bin/env bash

# To run in GAMS demo mode, use:
# bash -c "export NUMAGENTS=10 && export PACKAGES=5 && ./run-hilo-ce.sh"

set -e  # exit of failures

#-------------------------------------------------------------------------
# Start USER CONFIG
#-------------------------------------------------------------------------

NUMAGENTS=${NUMAGENTS:-100} # 100 agents, or set to 10 if using GAMS in demo mode
PACKAGES=${PACKAGES:-26} # 26 packages, or set to 5 if using GAMS in demo mode
REPEATS=${REPEATS:-30}
CYCLES=${CYCLES:-30}
TARGET_PERCENTAGE=${TARGET_PERCENTAGE:-12}
SIGMOID_MAX_STEP_X=${SIGMOID_MAX_STEP_X:-20}
PROFIT_MOTIVE_UPDATE_MULTIPLIER=${PROFIT_MOTIVE_UPDATE_MULTIPLIER:-0.1}
SOCIAL_NORM_UPDATE_MULTIPLIER=${SOCIAL_NORM_UPDATE_MULTIPLIER:-0.1}
VISIT_TYPE=${VISIT_TYPE:-0}
VISIT_PERCENTAGE=${TARGET_PERCENTAGE:-100}
VISIT_PERCENTAGE_PER_AGENT=${VISIT_PERCENTAGE_PER_AGENT:-100}
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

# Create the samples header file (note that the delimiter is the TAB character)
cat << EOF > $OUTDIR/samples.txt
$NUMAGENTS	$HI_CE_AGENTS_PERCENTAGE	$TARGET_PERCENTAGE	$SIGMOID_MAX_STEP_X	$PROFIT_MOTIVE_UPDATE_MULTIPLIER	$SOCIAL_NORM_UPDATE_MULTIPLIER	$VISIT_TYPE	$VISIT_PERCENTAGE	 $VISIT_PERCENTAGE_PER_AGENT
EOF

# Create the config file which is also needed by post processing scripts
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
		CMD="cd $dst && NUMPACKAGES=$PACKAGES && CYCLES=$CYCLES && . $PBS_O_WORKDIR/model.sh $replicate $NUMAGENTS $HI_CE_AGENTS_PERCENTAGE $TARGET_PERCENTAGE $SIGMOID_MAX_STEP_X $PROFIT_MOTIVE_UPDATE_MULTIPLIER $SOCIAL_NORM_UPDATE_MULTIPLIER $VISIT_TYPE	$VISIT_PERCENTAGE $VISIT_PERCENTAGE_PER_AGENT > model.out"
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

# HC25, Visit successful + unsuccessful, probabilistically directed to CE=0 (type 4)
HI_CE_AGENTS_PERCENTAGE=25 && VISIT_TYPE=4 && VISIT_PERCENTAGE=100 && VISIT_PERCENTAGE_PER_AGENT=25 && run $PBS_O_WORKDIR/`basename "$0"`.output.hice25.visit.t4p100p25
HI_CE_AGENTS_PERCENTAGE=25 && VISIT_TYPE=4 && VISIT_PERCENTAGE=100 && VISIT_PERCENTAGE_PER_AGENT=50 && run $PBS_O_WORKDIR/`basename "$0"`.output.hice25.visit.t4p100p50
HI_CE_AGENTS_PERCENTAGE=25 && VISIT_TYPE=4 && VISIT_PERCENTAGE=100 && VISIT_PERCENTAGE_PER_AGENT=75 && run $PBS_O_WORKDIR/`basename "$0"`.output.hice25.visit.t4p100p75
HI_CE_AGENTS_PERCENTAGE=25 && VISIT_TYPE=4 && VISIT_PERCENTAGE=100 && VISIT_PERCENTAGE_PER_AGENT=100 && run $PBS_O_WORKDIR/`basename "$0"`.output.hice25.visit.t4p100p100

# HC25, Visit successful + unsuccessful, probabilistically directed to CE=50 (type 3)
HI_CE_AGENTS_PERCENTAGE=25 && VISIT_TYPE=3 && VISIT_PERCENTAGE=100 && VISIT_PERCENTAGE_PER_AGENT=25 && run $PBS_O_WORKDIR/`basename "$0"`.output.hice25.visit.t3p100p25
HI_CE_AGENTS_PERCENTAGE=25 && VISIT_TYPE=3 && VISIT_PERCENTAGE=100 && VISIT_PERCENTAGE_PER_AGENT=50 && run $PBS_O_WORKDIR/`basename "$0"`.output.hice25.visit.t3p100p50
HI_CE_AGENTS_PERCENTAGE=25 && VISIT_TYPE=3 && VISIT_PERCENTAGE=100 && VISIT_PERCENTAGE_PER_AGENT=75 && run $PBS_O_WORKDIR/`basename "$0"`.output.hice25.visit.t3p100p75
HI_CE_AGENTS_PERCENTAGE=25 && VISIT_TYPE=3 && VISIT_PERCENTAGE=100 && VISIT_PERCENTAGE_PER_AGENT=100 && run $PBS_O_WORKDIR/`basename "$0"`.output.hice25.visit.t3p100p100

# HC25, Visit successful + unsuccessful only (type 2)
HI_CE_AGENTS_PERCENTAGE=25 && VISIT_TYPE=2 && VISIT_PERCENTAGE=100 && VISIT_PERCENTAGE_PER_AGENT=25 && run $PBS_O_WORKDIR/`basename "$0"`.output.hice25.visit.t2p100p25
HI_CE_AGENTS_PERCENTAGE=25 && VISIT_TYPE=2 && VISIT_PERCENTAGE=100 && VISIT_PERCENTAGE_PER_AGENT=50 && run $PBS_O_WORKDIR/`basename "$0"`.output.hice25.visit.t2p100p50
HI_CE_AGENTS_PERCENTAGE=25 && VISIT_TYPE=2 && VISIT_PERCENTAGE=100 && VISIT_PERCENTAGE_PER_AGENT=75 && run $PBS_O_WORKDIR/`basename "$0"`.output.hice25.visit.t2p100p75
HI_CE_AGENTS_PERCENTAGE=25 && VISIT_TYPE=2 && VISIT_PERCENTAGE=100 && VISIT_PERCENTAGE_PER_AGENT=100 && run $PBS_O_WORKDIR/`basename "$0"`.output.hice25.visit.t2p100p100

# HC25, Visit successful only (type 1)
HI_CE_AGENTS_PERCENTAGE=25 && VISIT_TYPE=1 && VISIT_PERCENTAGE=100 && VISIT_PERCENTAGE_PER_AGENT=25 && run $PBS_O_WORKDIR/`basename "$0"`.output.hice25.visit.t1p100p25
HI_CE_AGENTS_PERCENTAGE=25 && VISIT_TYPE=1 && VISIT_PERCENTAGE=100 && VISIT_PERCENTAGE_PER_AGENT=50 && run $PBS_O_WORKDIR/`basename "$0"`.output.hice25.visit.t1p100p50
HI_CE_AGENTS_PERCENTAGE=25 && VISIT_TYPE=1 && VISIT_PERCENTAGE=100 && VISIT_PERCENTAGE_PER_AGENT=75 && run $PBS_O_WORKDIR/`basename "$0"`.output.hice25.visit.t1p100p75
HI_CE_AGENTS_PERCENTAGE=25 && VISIT_TYPE=1 && VISIT_PERCENTAGE=100 && VISIT_PERCENTAGE_PER_AGENT=100 && run $PBS_O_WORKDIR/`basename "$0"`.output.hice25.visit.t1p100p100

# HC25, No visits (type 0)
HI_CE_AGENTS_PERCENTAGE=25 && VISIT_TYPE=0 && VISIT_PERCENTAGE=100 && VISIT_PERCENTAGE_PER_AGENT=100 && run $PBS_O_WORKDIR/`basename "$0"`.output.hice25.visit.t0p100p100

# HC75, all visit types
HI_CE_AGENTS_PERCENTAGE=75 && VISIT_TYPE=4 && VISIT_PERCENTAGE=100 && VISIT_PERCENTAGE_PER_AGENT=100 && run $PBS_O_WORKDIR/`basename "$0"`.output.hice75.visit.t4p100p100
HI_CE_AGENTS_PERCENTAGE=75 && VISIT_TYPE=3 && VISIT_PERCENTAGE=100 && VISIT_PERCENTAGE_PER_AGENT=100 && run $PBS_O_WORKDIR/`basename "$0"`.output.hice75.visit.t3p100p100
HI_CE_AGENTS_PERCENTAGE=75 && VISIT_TYPE=2 && VISIT_PERCENTAGE=100 && VISIT_PERCENTAGE_PER_AGENT=100 && run $PBS_O_WORKDIR/`basename "$0"`.output.hice75.visit.t2p100p100
HI_CE_AGENTS_PERCENTAGE=75 && VISIT_TYPE=1 && VISIT_PERCENTAGE=100 && VISIT_PERCENTAGE_PER_AGENT=100 && run $PBS_O_WORKDIR/`basename "$0"`.output.hice75.visit.t1p100p100
HI_CE_AGENTS_PERCENTAGE=75 && VISIT_TYPE=0 && VISIT_PERCENTAGE=100 && VISIT_PERCENTAGE_PER_AGENT=100 && run $PBS_O_WORKDIR/`basename "$0"`.output.hice75.visit.t0p100p100
