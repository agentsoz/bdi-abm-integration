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
	par(mar=c(12, 4.1, 1, 4.1), xpd=TRUE)
        plot(data1[,c("Cycle Number", "Increased CE")], ylim=range(c(0,100)), col="red", ylab="Number of Agents Increaded Their CE")
        points(data2[,c("Cycle Number", "Increased CE")], col="green")
	legend(1, -25, c("High-CE Agents=25%", "High-CE Agents=75%"), lty=c(1,1), lwd=c(1.5,1.5), col=c("red","green"))


	par(mar=c(12, 4.1, 1, 4.1), xpd=TRUE)
        plot(data1[,c("Cycle Number", "Decreased CE")], ylim=range(c(0,100)), col="red", ylab="Number of Agents Decreased Their CE")
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
	for (c in 1:cycles){
		df_1 = dbGetQuery(db, paste('select * from', "agents_ce", sprintf('where sample="%s" and cycle_number="%s" and replicate="1"', sampleNumber, c-1)))
		df_2 = dbGetQuery(db, paste('select * from', "agents_ce", sprintf('where sample="%s" and cycle_number="%s" and replicate="1"', sampleNumber, c)))
		previous_cycle <- round(as.numeric(data.matrix(df_1)), digits = 4)
		current_cycle <- round(as.numeric(data.matrix(df_2)), digits = 4)

		increased = 0;
		decreased = 0;

		for(col in 4:length(current_cycle)) {
			if(current_cycle[col] > previous_cycle[col]){
				increased = increased +1
			}

			if(current_cycle[col] < previous_cycle[col]){
				decreased = decreased +1
			}
		}

		output[c,] = c(c,increased, decreased)
	}


	print(output)
	return(output)
}

plot_file_path <- sprintf('%sagents_CE_changes.pdf', experiment_dir)
pdf(plot_file_path)
db_path <- sprintf('%soutput.db', experiment_dir)
db <- dbConnect(SQLite(), dbname=db_path)
analysis(db)
dev.off()
