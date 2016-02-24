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
        plot(data1[,c(output_params[1], output_params[2])], ylim=range(c(0,20)), col="red", ylab="Number of Agents Moved from LC to HC Category")
        points(data2[,c(output_params[1], output_params[2])], col="green")
	legend(1, -7, c("High-CE Agents=25%", "High-CE Agents=75%"), lty=c(1,1), lwd=c(1.5,1.5), col=c("red","green"))
}


analysis <- function(db) {
	s1_data <- readData(db,1)
	s2_data <- readData(db,2)
	plot_pair(s1_data, s2_data)
}

readData <- function(db, sampleNumber){
	output <- matrix(nrow=cycles,ncol=2)
	colnames(output) <- output_params 
	for (c in 1:cycles){
		df = dbGetQuery(db, paste('select CAST(MovedUpCE as REAL) from', "auction_statistics", sprintf('where sample="%s" and cycle_number="%s"', sampleNumber, c)))
		result <- data.matrix(df)
		output[c,] = c(c,mean(result[,1]))
	}

	return(output)
}

plot_file_path <- sprintf('%sagents_moved_from_LC_to_HC.pdf', experiment_dir)
pdf(plot_file_path)
db_path <- sprintf('%soutput.db', experiment_dir)
db <- dbConnect(SQLite(), dbname=db_path)
analysis(db)
dev.off()
