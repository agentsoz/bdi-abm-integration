#!/bin/bash

DIR=$(dirname "$0")

# executes a command
run() {
	cmd=$1
	echo $cmd; 
	eval $cmd
}

# check the args
if [ "$#" -ne 1 ]; then
	echo "usage: $0 DIR"
	exit
fi

# get the args
testdir=$1
cycles=$2

# now plot
for test in $(find $testdir -name "run-hi-ce-comparison.sh.output.*" -print); do
	run "rm -f $test/output.db"
	run "$DIR/postproc.sh $test"
done

