DIR=$(dirname "$0") # directory of this script
SCENARIO=$DIR/typical-summer-weekday # scenario location relative to script

# Call the plan generation script with the required parameters
Rscript $DIR/plan-algorithm.R \
  $SCENARIO/distributions.csv \
  $SCENARIO/location_maps.csv \
  $SCENARIO/numbers.csv \
  $SCENARIO/../Locations.csv \
  $SCENARIO/plans.xml

unzip matsim*
java -cp matsim-0.9.0/matsim-0.9.0.jar org.matsim.run.Controler config.xml
