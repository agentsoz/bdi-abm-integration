#!/bin/sh

. ./common.sh

# sanity check
file_samples=$(cat $EXPERIMENT_DIR/samples.txt | wc -l)
#test $SAMPLES -ne $file_samples && die "error: config SAMPLES value does not match wc -l samples.txt"

submit(){
        qsub - << EOF
#!/bin/bash
#PBS -N pross-r$replicate
#PBS -l nodes=1
#PBS -M peter.ross@rmit.edu.au
#PBS -m abe
#PBS -l mem=2500mb
#PBS -l walltime=24:00:00
#_PBS_PLACEHOLDER
cd \$PBS_O_WORKDIR
./run-samples.sh $*
EOF
}

if test $REPLICATES -gt 1; then
	# submit a job for each replicate
	for replicate in $(seq $START_REPLICATE $STOP_REPLICATE); do
		echo -n "replicate $replicate: "
		submit $EXPERIMENT_DIR --start-replicate=$replicate --stop-replicate=$replicate
	done
else
	# there is only one replicate, so submit a job for a 'number of samples'
	test -z $SLICE_SIZE && die "there is only one replicate; you must specify --slice-size=NNN"
	for start in $(seq $START_SAMPLE $SLICE_SIZE $STOP_SAMPLE); do
		stop=$(expr $start + $SLICE_SIZE - 1)
		if test $stop -gt $SAMPLES; then
			stop=$SAMPLES
		fi
		echo -n "sample $start...$stop: "
		submit $EXPERIMENT_DIR --start-sample=$start --stop-sample=$stop
	done
fi
