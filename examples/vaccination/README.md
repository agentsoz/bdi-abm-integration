# Vaccination modelling


Combined work with Nic Geard <nicholas.geard@unimelb.edu.au>.

A model of parental decision making and behaviour about childhood 
vaccination. The ABM is implemented in Python, and exists in 
https://bitbucket.org/ngeard/simodd-bdi. The parent decision making is
offloaded to the BDI counterparts (in JACK) that exists only for a 
portion of the agents life (from pregnancy to a few years after that).

## Dependencies


This program depends on the following libraries:

* BDI-ABM-Integration (`/integrations/bdi-abm`) 
* ABM-JACK-Integration (`/integrations/abm-jack`)


## How to Compile

1. Install the parent POM:

   > cd ../.. && mvn clean install --non-recursive && cd -

2. Build the BDI-ABM integration layer:

   > cd ../../integrations/bdi-abm && mvn clean install && cd -

3. Build the ABM-JACK integration layer (assuming you have 
   first installed the ABM-JACK dependencies as per 
   `../../../integrations/bdi-abm/README.md`):
 
   > cd ../../integrations/abm-jack && mvn clean install && cd -
   
4. Finally, build the vaccination application:

   > mvn clean install

## How to Run


To run from the command line:

   > ./test/run.sh



## License


BDI-ABM Integration Library
Copyright (C) 2014, 2015 by its authors. See AUTHORS file.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

For contact information, see AUTHORS file.

