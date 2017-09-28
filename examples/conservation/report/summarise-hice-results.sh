#!/bin/bash

DIR=$(dirname "$0")

if [ "$#" -ne 1 ]; then
	echo "usage: $0 INDIR"
	exit
fi

INDIR=$1

for name in 'number_of_agents' 'number_of_participants' 'social_norm' 'cost_of_auction_cycles' 'number_of_visits'; do
  str=""
  PDFS=$(find $INDIR -name "$name.pdf" -print | grep "hice25" | sort)
  for path in $PDFS ; do
    str+="$path "
  done
  CMD="pdfunite $str $INDIR/$name.all.pdf"
  echo $CMD; eval $CMD
done
