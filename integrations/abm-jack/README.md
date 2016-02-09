# ABM-JACK Integration Library

This ABM-JACK Integration library allows the use of JACK as the underlying
BDI system. For more information on the BDI-ABM integration project, 
see the top level README.


<a name="Dependencies"></a>
## Dependencies

- JACK 5.6 (aosgrp.com/products/jack)

Make sure JACK is installed on your system. Then assuming JACKROOT is the 
root directory of the installation, issue the following command to install
the JACK library in your local Maven repository:

   > mvn install:install-file \
      -Dfile=JACKROOT/lib/jack.jar \
      -DgroupId=com.aosgrp \
      -DartifactId=jack \
      -Dversion=5.6 \
      -Dpackaging=jar



## How to Compile

1.  Install dependencies to your local maven repository using the 
    instructions given above (see [Dependencies](#Dependencies))

2.  To Build the ABM-JACK integration layer: In `/integrations/abm-jack/`, 
    do `mvn clean install`


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



