REM scenario location relative to script
set SCENARIO="typical-summer-weekday"
REM Location of R installation
set RLOC="C:\Program Files\R\R-3.5.1\bin\i386"

REM Call the plan generation script with the required parameters
%RLOC%\Rscript plan-algorithm.R ^
  %SCENARIO%\distributions.csv ^
  %SCENARIO%\location_maps.csv ^
  %SCENARIO%\numbers.csv ^
  Locations.csv ^
  %SCENARIO%\plans.xml

REM Call the attribute generator with required parameters
%RLOC%\Rscript Attributes.R ^
  %SCENARIO%\numbers.csv ^
  %SCENARIO%\dependents.csv ^
  %SCENARIO%\thresholds.csv ^
  %SCENARIO%\stay.csv ^
  %SCENARIO%\prob_go_home.csv ^
  %SCENARIO%\plans.xml ^
  %SCENARIO%\plans.xml ^
  Refuges.csv
