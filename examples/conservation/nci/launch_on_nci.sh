#!/bin/bash

# Use this script to upload the distribution to the NCI account

NCI_USER=dxs595
NCI_PROJECT=ij2


SERVER=$NCI_USER@r-dm.nci.org.au
SERVERDIR=/short/$NCI_PROJECT/$NCI_USER/conservation
DIR=`dirname "$0"`


# Prepare the package dir

PACKDIR=$DIR/conservation
rm -rf $PACKDIR

mkdir -p $PACKDIR/target/classes/gams
cp $DIR/../target/conservation-ethics-2.0.2-SNAPSHOT-jar-with-dependencies.jar \
    $PACKDIR/target
cp $DIR/../target/classes/gams/bid_selection_model.gms \
    $PACKDIR/target/classes/gams/bid_selection_model.gms

mkdir -p $PACKDIR/test/output
cp $DIR/../test/*.{py,sh} $PACKDIR/test
cp $DIR/../test/output/config $PACKDIR/test/output
cp $DIR/../test/output/samples.txt $PACKDIR/test/output


# Replace the PBS headers
STRMATCH=$(<$DIR/pbs_header.txt)
grep -v "^#PBS" $DIR/../test/qsub-run-samples.sh \
    | perl -p0e "s/#_PBS_PLACEHOLDER/\"$STRMATCH\"/se" \
    > $PACKDIR/test/qsub-run-samples.sh




rsync -avz --delete $PACKDIR/ ${SERVER}:${SERVERDIR}

#ssh $SERVER "rm -rf ${SERVERDIR} && mkdir -p ${SERVERDIR}/target/classes/gams"
#rsync -avz \
#    $DIR/../target/conservation-ethics-2.0.2-SNAPSHOT-jar-with-dependencies.jar \
#    ${SERVER}:${SERVERDIR}/target/
#rsync -avz \
#    $DIR/../target/classes/gams/bid_selection_model.gms \
#    ${SERVER}:${SERVERDIR}/target/classes/gams/bid_selection_model.gms
#rsync -avz \
#    --exclude log/ \
#    $DIR/../test \
#    ${SERVER}:${SERVERDIR}

