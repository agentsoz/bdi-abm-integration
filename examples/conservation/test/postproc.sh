#!/bin/sh

if test -z "$*"; then
   echo "usage: $0 EXPERIMENT_DIRECTORY"
   exit 1
fi

EXPERIMENT_DIR="$1"
. $EXPERIMENT_DIR/config

if test -e "$EXPERIMENT_DIR/output.db"; then
	echo "database already exists"
	exit 1
fi

# decide what python interpretter to use
# (important, because trifid is stuck on python2.7)
PYTHON="python"
if test "$(hostname)" = "sewwandi-OptiPlex-9020"; then
	PYTHON="python2.7"
fi
$PYTHON ./postproc.py $EXPERIMENT_DIR
