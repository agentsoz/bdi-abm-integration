#!/bin/sh

. ./common.sh
sleep 1

run=1
sample=1
cat $EXPERIMENT_DIR/samples.txt | while read x; do
	if test $sample -ge $START_SAMPLE && test $sample -le $STOP_SAMPLE; then
		for replicate in $(seq $START_REPLICATE $STOP_REPLICATE); do
			currentDir=$(pwd)
 			dst="$EXPERIMENT_DIR/log/archive-$sample-$replicate"
			echo $dst
			test -d "$PBS_O_WORKDIR/$dst" && die "$dst" already exists
			mkdir -p "$PBS_O_WORKDIR/$dst"
			cd $PBS_O_WORKDIR/$dst
			$PBS_O_WORKDIR/model.sh $x $replicate || die_run
			rm _gams*
			rm conservation.in.*
			rm conservation.out.*
			run=$(expr $run + 1)
		done
	fi
	sample=$(expr $sample + 1)
done

echo "DONE"
