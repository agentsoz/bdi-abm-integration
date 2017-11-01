#!/bin/bash


# user defined list of tests
tests=(
	run-hi-ce-comparison.sh.output.hice25
	run-hi-ce-comparison.sh.output.hice25.visit.t1p100
	run-hi-ce-comparison.sh.output.hice25.visit.t2p100
	run-hi-ce-comparison.sh.output.hice75
	run-hi-ce-comparison.sh.output.hice75.visit.t1p100
	run-hi-ce-comparison.sh.output.hice75.visit.t2p100
)

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
for test in ${tests[@]}; do
	run "./plot_cost_of_auction_cycles.R $testdir/$test/ $cycles"
	run "./plot_number_of_participants.R $testdir/$test/ $cycles"
	run "./plot_number_of_agents.R $testdir/$test/ $cycles"
done

