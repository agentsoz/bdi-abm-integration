# Bushfire Evacuation Tutorial

This project contains code for an example bushfire evacuation application
using the BDI-ABM framework. The agents are coded in JACK, while the simulation
is carried out in MATSim. Conceptually, an agent is split across the two systems
with its "brain" in the JACK system, and its "body" in MATSim.

For more information on the BDI-ABM integration project,
see the top level README.


## Dependencies


This program depends on the following libraries:

* BDI-ABM-Integration (`/integrations/bdi-abm`)
* BDI-MATSim-Integration (`/integrations/bdi-matsim`)
* ABM-JACK-Integration (`/integrations/abm-jack`)


## How to Compile

### Command Line

1.  Build the bdi-abm-integration layer: In the repository root `/`, do
    `mvn clean install -N`
2.  Build the BDI-ABM library: See `/integrations/bdi-abm/README.md`
    for instructions
3.  Build the BDI-MATSim library: See `/integrations/bdi-matsim/README.md`
    for instructions
4.  Build the ABM-JACK library: See `/integrations/abm-jack/README.md`
    for instructions
5.  Build this example bushfire application: In `/examples/bushfire-tutorial`, do
    `mvn clean install`

### Eclipse

Ensure that you have the corrent version of Eclipse installed. See
`../../README.md` for details. Then import and build the following
Eclipse projects:

*  `/integrations/bdi-abm`
*  `/integrations/bdi-matsim`
*  `/integrations/abm-jack`
*  `/examples/bushfire-tutorial`



## How to Run


From the `/examples/bushfire-tutorial` directory, and assuming you have
already built the application, do:

        > cd target
        > unzip bushfire-x.x.x-SNAPSHOT-minimal.zip
        > cd bushfire-x.x.x-SNAPSHOT
        > ./run.sh



## License


BDI-ABM Integration Package
Copyright (C) 2014 - 2017 by its authors. See AUTHORS file.

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

