#!/bin/sh

die(){
	echo $*
	exit 1
}

sleepn(){
	for x in $(seq $1); do
		echo -n "..."
		sleep 1
		echo -n $x
	done
	echo
}

help(){ 
	echo "perform batch sampling"
	echo
	echo "usage: $0 [options] EXPERIMENT_DIRECTORY"
	exit 1
}

for opt do
	optval="${opt#*=}"
	case "$opt" in
		--local)
			. ./emulate-qsub.sh ;;
		--slice-size=*)
			SLICE_SIZE=$optval ;;
		--start-sample=*)
			START_SAMPLE=$optval ;;
		--stop-sample=*)
			STOP_SAMPLE=$optval;;
		--start-replicate=*)
			START_REPLICATE=$optval;;
		--stop-replicate=*)
			STOP_REPLICATE=$optval;;
		--help|-h)
			help
			;;	
		-*)
			die "error: unknown option $opt"
			;;
		*)
			# read config file
			EXPERIMENT_DIR=$optval
			. $EXPERIMENT_DIR/config
			START_SAMPLE=1
			STOP_SAMPLE=$SAMPLES
			START_REPLICATE=1
			STOP_REPLICATE=$(expr $START_REPLICATE + $REPLICATES - 1)  # from config file
			;;
	esac
done

test -z "$EXPERIMENT_DIR" && die "error: experiment directory not specified"

echo "EXPERIMENT_DIR:     $EXPERIMENT_DIR"
echo "EXPERIMENT:         $EXPERIMENT"
echo "SAMPLES:            [${START_SAMPLE}...${STOP_SAMPLE}]"
echo "REPLICATES          [${START_REPLICATE}...${STOP_REPLICATE}]"

test $START_SAMPLE -gt $STOP_SAMPLE && die "error: start sample > stop sample"
test $START_REPLICATE -gt $STOP_REPLICATE && die "error: start replicate > stop replicate"
test $STOP_SAMPLE -gt $SAMPLES && die "error: stop sample > config/SAMPLES"
test $STOP_REPLICATE -gt $REPLICATES && die "error: stop replicate > config/REPLICATES"

sanity_check(){
	file_samples = $(cat $EXPERIMENT_DIR/samples.txt | wc -l)
	test $SAMPLES -ne $file_samples && die "config/SAMPLES does does not match wc -l samples.txt"
}
