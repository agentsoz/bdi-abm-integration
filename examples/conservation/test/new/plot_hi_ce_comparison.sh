#!/bin/bash

# executes a command
run() {
	cmd=$1
	echo $cmd; 
	eval $cmd
}

# check the args
if [ "$#" -ne 2 ]; then
	echo "usage: $0 DIR CYCLES"
	exit
fi

# get the args
testdir=$1
cycles=$2

# now plot
for test in $(find $testdir -name "run-hi-ce-comparison.sh.output.*" -print); do
	run "./plot_cost_of_auction_cycles.R $test/ $cycles"
	run "./plot_number_of_participants.R $test/ $cycles"
	run "./plot_number_of_agents.R $test/ $cycles"
	run "./plot_visits.R $test/ $cycles"
done

