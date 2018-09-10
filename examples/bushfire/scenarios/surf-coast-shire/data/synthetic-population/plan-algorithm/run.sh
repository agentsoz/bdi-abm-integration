DIR=$(dirname "$0") # directory of this script
SCENARIO=$DIR/typical-summer-weekday # scenario location relative to script

# Call the plan generation script with the required parameters
Rscript $DIR/plan-algorithm.R \
  $SCENARIO/distributions.csv \
  $SCENARIO/location_maps.csv \
  $SCENARIO/numbers.csv \
  $SCENARIO/travel_factor.csv \
  $SCENARIO/../Locations.csv \
  $SCENARIO/plans.xml

# Call the attribute generator with required parameters

Rscript $DIR/BDI_attributes.R \
  $SCENARIO/numbers.csv \
  $SCENARIO/dependents.csv \
  $SCENARIO/thresholds.csv \
  $SCENARIO/stay.csv \
  $SCENARIO/prob_go_home.csv \
  $SCENARIO/plans.xml \
  $SCENARIO/plans.xml \
  $SCENARIO/../Refuges.csv

# rm -rf matsim/output/
# unzip matsim/matsim*
# java -cp matsim/matsim-0.9.0/matsim-0.9.0.jar org.matsim.run.Controler matsim/config.xml
# rm -r matsim/matsim-0.9.0/
