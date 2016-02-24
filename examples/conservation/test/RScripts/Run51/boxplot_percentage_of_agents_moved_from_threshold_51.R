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

# process database
plot_pair <- function(data1, data2) {
	par(mar=c(12, 6.1, 1, 4.1), xpd=TRUE)
        plot(data1[,c("Cycle Number", "Increased CE")], ylim=range(c(0,100)), col="red", ylab="Percentage of Agents who initially had \n a CE below threshold, \n and now a CE above threshold")
        points(data2[,c("Cycle Number", "Increased CE")], col="green")
	legend(1, -25, c("High-CE Agents=25%", "High-CE Agents=75%"), lty=c(1,1), lwd=c(1.5,1.5), col=c("red","green"))


	par(mar=c(12, 6.1, 1, 4.1), xpd=TRUE)
        plot(data1[,c("Cycle Number", "Decreased CE")], ylim=range(c(0,100)), col="red", ylab="Percentage of Agents who initially had \n a CE above threshold, \n and now a CE below threshold")
        points(data2[,c("Cycle Number", "Decreased CE")], col="green")
	legend(1, -25, c("High-CE Agents=25%", "High-CE Agents=75%"), lty=c(1,1), lwd=c(1.5,1.5), col=c("red","green"))
}


analysis <- function(db) {
	s1_data <- readData(db,1)

	print(s1_data[,c("Increased CE")])
	print('==================')
	print(s1_data[,c("Increased CE")])
	
	par(mar=c(4.1, 6.1, 4.1, 4.1), xpd=TRUE)
	boxplot(s1_data, ylab ="Percentage of agents increased/decreased \ntheir CE above/below threshold \nat the end of 100 auction cycles")
	title('Initial high CE agents% = 25%')
}

readData <- function(db, sampleNumber){
	output <- matrix(nrow=REPLICATES,ncol=2)
	colnames(output) <- c("Increased CE", "Decreased CE")
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


		df_2 = dbGetQuery(db, paste('select * from', "agents_ce", sprintf('where sample="%s" and cycle_number="100" and replicate="%s"', sampleNumber, r)))
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

		output[r,] = c(increased * 100/initially_low, decreased* 100/ initially_high)
	}

	print(output)
	return(output)
}

plot_file_path <- sprintf('%spercentage_of_agents_changed_CE_category_box.pdf', experiment_dir)
pdf(plot_file_path)
db_path <- sprintf('%soutput.db', experiment_dir)
db <- dbConnect(SQLite(), dbname=db_path)
analysis(db)
dev.off()
