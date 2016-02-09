# ABM-JILL Integration Library

This ABM-JILL Integration library allows the use of JILL as the underlying
BDI system. For more information on the BDI-ABM integration project, 
see the top level README.



<a name="Dependencies"></a>
## Dependencies

- JILL 0.3.1-SNAPSHOT (http://agentsoz.github.io/jill)

Download the 0.3.1-SNAPSHOT version of JILL from https://github.com/agentsoz/jill.
Next, in `/jill/`, do `mvn clean install` to install the Jill library to your 
local Maven repository.



## How to Compile

1.  Install dependencies to your local maven repository using the 
    instructions given above (see [Dependencies](#Dependencies))

2.  To Build the ABM-JILL integration layer: In `/integrations/abm-jill/`, 
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



