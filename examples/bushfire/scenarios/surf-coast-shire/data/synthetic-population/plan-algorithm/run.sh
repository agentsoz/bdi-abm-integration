DIR=$(dirname "$0") # directory of this script
SCENARIO=$DIR/typical-summer-weekday # scenario location relative to script

# Call the plan generation script with the required parameters
Rscript $DIR/plan-algorithm.R \
  $SCENARIO/distributions.csv \
  $SCENARIO/location_maps.csv \
  $SCENARIO/numbers.csv \
  $SCENARIO/../Locations.csv \
  $SCENARIO/plans.xml

# Call the attribute generator with required parameters

Rscript $DIR/Attributes.R \
  $SCENARIO/numbers.csv \
  $SCENARIO/dependents.csv \
  $SCENARIO/thresholds.csv \
  $SCENARIO/stay.csv \
  $SCENARIO/prob_go_home.csv \
  $SCENARIO/plans.xml \
  $SCENARIO/plans.xml \
  $SCENARIO/../Refuges.csv

rm -rf output/
# unzip matsim*
# java -cp matsim-0.9.0/matsim-0.9.0.jar org.matsim.run.Controler config.xml
# rm -r matsim-0.9.0/
