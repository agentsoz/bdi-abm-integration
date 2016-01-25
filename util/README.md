# Util Package

This package contains a number of standalone utilities developed to convert data and provide other functions.



## How to Compile

### Command Line

In `/util`, do `mvn clean install`



## Different Utilities

### MatsimPopulationFileCreator

A simple tool to create a matsim population file by randomly distributing agents within a bound rectangle.

To run:

java -cp target/util-0.0.1-SNAPSHOT.jar agentsoz.util.MatsimPopulationFileCreator -p <population> -ur <urx> <ury> -ll <llx> <lly> -o <output_file>

where <population> is the number of residents to generate

<output_file> is the file to write matsim population

<urx> and <ury> are the x any coordinates for the upper right corner of a rectangle (longitude and latitdude)

<llx> and <lly> are the x any coordinates for the lower left corner of a rectangle (longitude and latitdude)

Example coordinates (for Halls Gap) are:

	* top left: 142.5177, -37.13657
	* top right: 142.525, -37.13664
	* bottom left: 142.5171, -37.14509
	* bottom right: 142.5256, -37.14532
	 
     
### MoveFire

A tool to move a fire text file for the bushfire simulator. It simply applies an offset (translation) to existing fire coordinates or changes the time of fire events by a set ratio.

To run:

java -cp target/util-0.0.1-SNAPSHOT.jar agentsoz.util.MoveFire -in <input_file> -out <output_file> -lat <lat> -long <long> -time <ratio>

where:
<input_file> is the name of the existing fire file to read
<output_file> is the name of the file to write
<lat> is the offset to apply to the latitude (y) coordinate of the fire
<long> is the offset to apply to the longitude (x) coordinate of the fire
<ratio> is a multiplier for the time to change the duration of the fire



### NetworkGenerator:

A tool to convert an open street map file into a matsim network file. It depends on the matsim library so a reference to that needs to be included in the classpath.

To run:

java -cp <matsim jar file>:.target/util-0.0.1-SNAPSHOT.jar agentsoz.util.NetworkGenerator 
-i <input file.osm> 
-o <output file.xml>  
[-wkt <coordinate transformation string>]

where:
<input_osm_file> is the input open street map file
<output_xml_file>  is the output matsim network file
<coordinate transformation string> the ESRI well known string based on the UTM zone of the network. For Australia, the ESRI WKT can be generated using menu in lower left of http://spatialreference.org/ref/epsg/28354/ page. Default value is set to MGA zone 54

for example:

java -cp ../integrations/bdi-matsim/dep/matsim-0.7.0-SNAPSHOT.jar:./target/util-0.0.1-SNAPSHOT.jar agentsoz.util.NetworkGenerator -i input/halls_gap.osm -o output/halls_gap_network.xml -wkt "PROJCS["GDA94 / MGA zone 54",GEOGCS["GDA94",DATUM["D_GDA_1994",SPHEROID["GRS_1980",6378137,298.257222101]],PRIMEM["Greenwich",0],UNIT["Degree",0.017453292519943295]],PROJECTION["Transverse_Mercator"],PARAMETER["latitude_of_origin",0],PARAMETER["central_meridian",141],PARAMETER["scale_factor",0.9996],PARAMETER["false_easting",500000],PARAMETER["false_northing",10000000],UNIT["Meter",1]]"



### PopulationGenerator:

A tool to convert an address shape file into a matsim population file.

It expects the following files to exist as iput files, where <address> is the name of the town:

<address>.dbf
<address>.sbn
<address>.sbx
<address>.shp
<address>.shx

To run:

java -cp <matsim jar file>:./target/util-0.0.1-SNAPSHOT.jar agentsoz.util.PopulationGenerator 
-a <address>.shp 
-n <number> 
-o <output_file>
[-p <person_id_prefix>]
[-e <education_file:agents>]
[-w <work_file:agents>]


Where:

<address>.shp is the name of the input shape file. Note the ".shp" is required but <address> is used to find the other 4 files required.

<number> is the number of residents to place at home addresses

<output_file> is the name of the population file to generate (xml)

<education_file> is the (optional) name of a file identifying some addresses as schools and <agents> places that many agents at that location

<work_file> is the (optional) name of a file identifying some addresses as workplaces  and <agents> places that many agents at that location

<person_id_prefix> is the prefix for IDs of residents

**NOTE** at the time of writing this doco, the education and workplace fundtionality has not been tested/ verified.

For example:

java -cp ../integrations/bdi-matsim/dep/MATSim_r28802.jar:./target/util-0.0.1-SNAPSHOT.jar agentsoz.util.PopulationGenerator -n 500 -a input/halls_gap.shp -o output/halls_gap_population.xml -p halls_gap



