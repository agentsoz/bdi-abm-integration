#!/bin/bash

DIR=$(dirname "$0")

if [ "$#" -ne 2 ]; then
	echo "usage: $0 INDIR OUTDIR"
	exit
fi

INDIR=$1
OUTDIR=$2
TESTNAME=$(basename "$INDIR")
mkdir -p $OUTDIR # create the dir to store the results
rm -rf $OUTDIR/* # remove everytihng in it (if dir already existed)

PDFS=$(find $INDIR -name "*.pdf" -print)
for path in $PDFS ; do
  CMD="cp --parents $path $OUTDIR"
  echo $CMD; eval $CMD
done

PDFS=$(find $OUTDIR -name "*.pdf" -print)
for path in $PDFS ; do
  #CMD="gs -q -dNOPAUSE -dBATCH -sDEVICE=jpeg -r200 -sOutputFile='$path.p%0d.jpg' $path"
  CMD="convert -quality 100 -geometry 1200x1200 -density 300x300 $path $path.p%0d.png"
  echo $CMD; eval $CMD
done

