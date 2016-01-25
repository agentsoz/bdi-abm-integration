#!/bin/sh
. ./common.sh

qsub - << EOF
#!/bin/bash
#PBS -N pross-fold-$EXPERIMENT
#PBS -l nodes=1
#PBS -M peter.ross@rmit.edu.au
#PBS -m abe
#PBS -l mem=8gb
#PBS -l walltime=24:00:00
cd \$PBS_O_WORKDIR
./postproc.sh "$EXPERIMENT_DIR"
EOF
