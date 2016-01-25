===========================================================================
ABM-JADEX Library
===========================================================================

===========================================================================
1.  ABOUT

The ABM-JADEX library is an extension of Jadex, a BDI agent system, and is 
designed to facilitate communication with an ABM system.

You can viea an example integration between jadex and repast, a zombie 
simulation, in the ./examples directory.


===========================================================================
2. DOCUMENTATION

For information about Jadex see:
http://www.activecomponents.org/bin/view/Documentation/Overview


===========================================================================
3. DEPENDENCIES

All dependencies should be placed in the abm-jadex/dep directory BEFORE
attempting to compile.

3.1  Jadex System
The Jadex system must be installed on your machine before compiling.

The ABM-JADEX library was developed and tested with Jadex 2.3

For download files, see:
http://www.activecomponents.org/bin/view/Download/Overview

Once Jadex is installed make sure to modify /build.xml, where it states 
"JADEX SET UP", to the correct location of the jadex dist on your machine.


3.2  BDI-SIM library
Check the ./integrations/bdi-sim directory for instructions to obtain
the bdi-sim library jar file, bdiabm.jar.

Place in /bdi-matsim/dep/bdiabm.jar


===========================================================================
4. COMPILING

Running Apache Ant with build.xml will build and compile the abm-jadex 
library and produce abmjadex.jar, holding all the neccessary class files.

Executing the command: ant clean-all
will delete the compilation files and the produced jar/s.
