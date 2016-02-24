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
sampleCount <- nrow(samples)
output_params <- c("Cycle Number", "Moved from LC to HC")

# process database
plot_pair <- function(data1, data2) {
	par(mar=c(12, 6.1, 1, 4.1), xpd=TRUE)
        plot(data1[,c("Cycle Number", "Increased CE")], ylim=range(c(0,100)), col="red", ylab="Number of Agents who initially had \n a CE below threshold, \n and now a CE above threshold")
        points(data2[,c("Cycle Number", "Increased CE")], col="green")
	legend(1, -25, c("High-CE Agents=25%", "High-CE Agents=75%"), lty=c(1,1), lwd=c(1.5,1.5), col=c("red","green"))


	par(mar=c(12, 6.1, 1, 4.1), xpd=TRUE)
        plot(data1[,c("Cycle Number", "Decreased CE")], ylim=range(c(0,100)), col="red", ylab="Number of Agents who initially had \n a CE above threshold, \n and now a CE below threshold")
        points(data2[,c("Cycle Number", "Decreased CE")], col="green")
	legend(1, -25, c("High-CE Agents=25%", "High-CE Agents=75%"), lty=c(1,1), lwd=c(1.5,1.5), col=c("red","green"))
}


analysis <- function(db) {
	s1_data <- readData(db,1)
	s2_data <- readData(db,2)
	plot_pair(s1_data, s2_data)
}

readData <- function(db, sampleNumber){
	output <- matrix(nrow=cycles,ncol=3)
	colnames(output) <- c("Cycle Number", "Increased CE", "Decreased CE")

	# read agents initial Ce values
	df_1 = dbGetQuery(db, paste('select * from', "agents_ce", sprintf('where sample="%s" and cycle_number="0" and replicate="1"', sampleNumber)))
	initial_values <- round(as.numeric(data.matrix(df_1)), digits = 4)
	threshold = 50

	for (c in 1:cycles){
		df_2 = dbGetQuery(db, paste('select * from', "agents_ce", sprintf('where sample="%s" and cycle_number="%s" and replicate="1"', sampleNumber, c)))
		current_cycle <- round(as.numeric(data.matrix(df_2)), digits = 4)

		increased = 0;
		decreased = 0;

		# iterate through all agents (agents are represented in each column from column number 4)
		for(col in 4:length(current_cycle)) {

			#if agent initially had high value
			if(initial_values[col] >= threshold){
				if(current_cycle[col] < threshold){
					decreased = decreased +1
				}
			} 

			# if agent initially had low value
			if(initial_values[col] < threshold){
				if(current_cycle[col] >= threshold){
					increased = increased +1
				}
			}
		}

		output[c,] = c(c,increased, decreased)
	}


	print(output)
	return(output)
}

plot_file_path <- sprintf('%snumber_of_agents_changed_CE_category.pdf', experiment_dir)
pdf(plot_file_path)
db_path <- sprintf('%soutput.db', experiment_dir)
db <- dbConnect(SQLite(), dbname=db_path)
analysis(db)
dev.off()
