#!/usr/bin/Rscript
library(sqldf)


#
# first input configuration parameter to the file should be 
# the path to experiment directory
# second inputparameter : number of auction cycles per test
#

# input & output variable names
args <- commandArgs(trailingOnly = TRUE)
experiment_dir=args[1]
cycles=as.numeric(args[2])
numAgents = args[3]

# read config file
config_file_path <- sprintf('%sconfig', experiment_dir)
source(config_file_path, local = TRUE)

# read samples
samples_file_path <- sprintf('%ssamples.txt', experiment_dir)
samples = read.csv(samples_file_path, header = FALSE, sep='	')

analysis <- function(db) {
	s1_data <- readData(db,1)

	par(mar=c(10.6, 6.1, 1.3, 1), xpd=TRUE)
	boxplot(s1_data, ylab ="Percentage of agents increased/decreased \ntheir CE above/below threshold", las = 2)
	title('Initial high CE agents% = 25%')
}

readData <- function(db, sampleNumber){
	output <- matrix(nrow=REPLICATES,ncol=20)
	colnames(output) <- c("Increased CE, cycle 10", "Increased CE, cycle 20", "Increased CE, cycle 30", "Increased CE, cycle 40", "Increased CE, cycle 50", "Increased CE, cycle 60", "Increased CE, cycle 70", "Increased CE, cycle 80", "Increased CE, cycle 90", "Increased CE, cycle 100", "Decreased CE, cycle 10", "Decreased CE, cycle 20", "Decreased CE, cycle 30", "Decreased CE, cycle 40", "Decreased CE, cycle 50", "Decreased CE, cycle 60", "Decreased CE, cycle 70", "Decreased CE, cycle 80", "Decreased CE, cycle 90", "Decreased CE, cycle 100")
	threshold = 50

	for (r in 1:REPLICATES){
		# read agents initial Ce values
		df_1 = dbGetQuery(db, paste('select * from', "agents_ce", sprintf('where sample="%s" and cycle_number="0" and replicate="%s"', sampleNumber, r)))
		initial_values <- round(as.numeric(data.matrix(df_1)), digits = 4)

		initially_high = 0
		initially_low = 0
		# calculate how many agents initially had high CE.
		for(col in 4:length(initial_values)) {
			if(as.numeric(initial_values[col])<threshold){
				initially_low = initially_low + 1
			} else {
				initially_high = initially_high + 1
			}
		}

		for(c in 10:cycles){
			df_2 = dbGetQuery(db, paste('select * from', "agents_ce", sprintf('where sample="%s" and cycle_number="%s" and replicate="%s"', sampleNumber, c, r)))
			last_cycle <- round(as.numeric(data.matrix(df_2)), digits = 4)

			increased = 0;
			decreased = 0;

			# iterate through all agents (agents are represented in each column from column number 4)
			for(col in 4:length(last_cycle)) {

				#if agent initially had high value
				if(initial_values[col] >= threshold){
					if(last_cycle[col] < threshold){
						decreased = decreased +1
					}
				} 

				# if agent initially had low value
				if(initial_values[col] < threshold){
					if(last_cycle[col] >= threshold){
						increased = increased +1
					}
				}
			}

			output[r,c/10] = increased * 100/initially_low
			output[r,c/10 + 10] = decreased * 100/initially_high
		}
	}
	return(output)
}

plot_file_path <- sprintf('%spercentage_of_agents_changed_CE_category_each_cycle_box.pdf', experiment_dir)
pdf(plot_file_path)
db_path <- sprintf('%soutput.db', experiment_dir)
db <- dbConnect(SQLite(), dbname=db_path)
analysis(db)
dev.off()
