# BDI-GAMS Integration Library

This BDI-GAMS Integration library allows the use of GAMS as the underlying
ABM. For more information on the BDI-ABM integration project, 
see the top level README.


<a name="Dependencies"></a>
## Dependencies

- GAMS 24.4 (www.gams.com)

Make sure GAMS is installed on your system. Then assuming GAMSROOT is the 
root directory of the installation, issue the following command to install
the GAMS Java API library in your local Maven repository:

   > mvn install:install-file \
      -Dfile=GAMSROOT/apifiles/Java/api/GAMSJavaAPI.jar \
      -DgroupId=com.gams \
      -DartifactId=gams \
      -Dversion=24.4 \
      -Dpackaging=jar



## How to Compile

1.  Install dependencies to your local maven repository using the 
    instructions given above (see [Dependencies](#Dependencies))

2.  To Build the ABM-GAMS integration layer: In `/integrations/abm-gams/`, 
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


