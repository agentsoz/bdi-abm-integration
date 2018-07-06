This algorithm will produce a MATSim plan XML file based on different agent types and the distribution of their activities over a 24 hr period.

##INPUT FILES

The Locations.csv file is exported directly from a Locations.shp file in QGIS, and gives a list of locations in the region that agents can make 
trips to and from. The only information in it that the algorithm needs is the columns "LandUse", "xcoord" and "ycoord". If the user would like 
to use a different csv to the example provided, these three bits of informations should have titles matching "LandUse", "xcoord" and "ycoord".  

The input.csv file is where the distribution tables are stored. For example, an agent type "Resident" might have the following distribution table:

Resident, 100
home, 90, 90, 85, 75, 30, 20, 15, 10, 25, 50, 80, 85
						    (EvacZone)
work,  5,  5, 10, 15, 50, 60, 60, 50, 40, 30, 10, 10
						    (Business District,Caravan Park,Hotel,Golf Club)
beach, 0,  0,  0,  0,  5,  5, 10, 15,  5,  0,  0,  0
					            (Beach)
shops, 0,  0,  0,  5, 10, 10, 10, 20, 25, 15,  5,  0
						    (Shops)
other, 5,  5,  5,  5,  5,  5,  5,  5,  5,  5,  5,  5
				                    (University,Secondary,Tafe,Kindergarten,Primary)

The first line denotes the type "Resident" and the desired number of this agent type (can be zero).

The subesquent line pairs denote an activity, its distribution and the "LandUse" locations in the Locations.csv file that this activity is associated 
with. (These locations should always be in brackets).

An activity may have any label, but must match with one of the specified activities in the MATSim config file. In general, it is easier to keep 
activity labels consistent across agent types, and instead vary the "LandUse" locations that they map to.
 
It is not crucial to have the "LandUse" locations tabbed to the end of the distribution table, but is done so for formatting/readability of the distribution table.

Each table column must sum to 100 (the algorithm checks this).

#RUNNING THE ALGORITHM
The algorithm may be run on the command line with the arguments
"Rscript plan-algorithm.R *input file* *locations file* *output file*"

Note that the output file will be in XML format, and ready to run in MATSim.

An executable script "run.sh" is provided which will run the algorithm with the provided input files.   
    
