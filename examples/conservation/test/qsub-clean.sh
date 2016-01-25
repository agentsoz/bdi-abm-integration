#!/bin/sh
. ./common.sh

qsub - << EOF
#!/bin/bash
#PBS -N pross-clean
#PBS -l nodes=1
#PBS -M peter.ross@rmit.edu.au
#PBS -m abe
#PBS -l mem=1gb
#PBS -l walltime=12:00:00
cd \$PBS_O_WORKDIR
rm -rf "$EXPERIMENT_DIR/log"
EOF
