This algorithm will produce a MATSim plan XML file based on different agent types and the distribution of their activities over a 24 hr period.

##INPUT FILES

The Locations.csv file is exported directly from a Locations.shp file in QGIS, and gives a list of locations in the region that agents can make 
trips to and from. The only information in it that the algorithm needs is the columns "LandUse", "xcoord" and "ycoord". If the user would like 
to use a different csv to the example provided, these three bits of informations should have titles matching "LandUse", "xcoord" and "ycoord".  

There are 3 other required input files: "distributions", "location_maps", and "numbers".
The distributions file specifies where each agent type is. For example, an agent type "Resident" might have the following distribution table:

Resident
home, 90, 90, 85, 75, 30, 20, 15, 10, 25, 50, 80, 85
work,  5,  5, 10, 15, 50, 60, 60, 50, 40, 30, 10, 10
beach, 0,  0,  0,  0,  5,  5, 10, 15,  5,  0,  0,  0
shops, 0,  0,  0,  5, 10, 10, 10, 20, 25, 15,  5,  0
other, 5,  5,  5,  5,  5,  5,  5,  5,  5,  5,  5,  5
				                    
where each column represents a two hour block of the 24 hour period, and the numbers in the table represent the percentage of the "Resident" 
population undertaking the activity at that time. Each table column must sum to 100 (the algorithm checks this).

Each row represents an activity that that agent type can do. An activity may have any label, but must match with one of the specified activities 
in the MATSim config file. In general, it is easier to keep activity labels consistent across agent types, and instead vary the "LandUse" locations 
that they map to. NB at present, "home" is reserved as the first and last destination for ALL agents. This means that every agent should have their 
first activity listed as "home". The activity "work" is coded to have double duration, to make more realistic plans.  
 
The location_maps file is necessary to map the activities defined in the distributions file to locations in the Locations.csv file. 

The numbers file simply denotes the required number of each agent type.
#RUNNING THE ALGORITHM
The algorithm may be run on the command line with the arguments
"Rscript plan-algorithm.R *distributions file* *location_maps file* *numbers file* *Locations file* *output file*"

Note that the output file will be in XML format, and ready to run in MATSim.

An executable script "run.sh" is provided which will run the algorithm with the provided input files.   
    
