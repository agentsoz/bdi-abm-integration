#!/usr/bin/Rscript
library(sqldf)


#
# first input parameter to the file should be 
# the path to experiment directory
# second input parameter should be the number of auction rounds in the
# simulation.
#

# input & output variable names
args <- commandArgs(trailingOnly = TRUE)
experiment_dir=args[1]
numCycles=as.numeric(args[2])

# read config file
config_file_path <- sprintf('%sconfig', experiment_dir)
source(config_file_path, local = TRUE)

# read samples
samples_file_path <- sprintf('%ssamples.txt', experiment_dir)
samples = read.csv(samples_file_path, header = FALSE, sep='	')
sampleCount <- nrow(samples)
output_names <- c("cycle_number", "cost_of_successful_bids")

# process database

plot_pair <- function(result1, result2) {
	y_label <- "Cost"
	x_label <- "Auction Cycle Number"

	# Add extra space to right of plot area; change clipping to figure
	par(mar=c(9, 4.1, 1, 1), xpd=TRUE)

        plot(result1[,c(output_names[1],output_names[2])],col="red", xlab=x_label, ylab=y_label, ylim=range(c(5.2,10.5)))
	points(result2[,c(output_names[1],output_names[2])],col="green")
	legend(25, 3.8, c("High-CE Agents=25%", "High-CE Agents=75%"), lty=c(1,1), lwd=c(1.5,1.5), col=c("red","green"))
	
}

analysis <- function(db) {
	s25_result=getCostResults(db, 2)
	s75_result=getCostResults(db, 4)
	plot_pair(s25_result, s75_result)
}

getCostResults <- function(db, sampleNumber){
	final_result <- matrix(nrow=numCycles,ncol=length(output_names))
	colnames(final_result) <- output_names

	for( c in 1:numCycles){
		query_last_part <- sprintf('where sample="%s" and cycle_number="%s" and number_of_successful_bids != "0" ', sampleNumber, c)
		query_first_part <- sprintf('select %s from', output_names[2])
		df = dbGetQuery(db, paste(query_first_part, "auction_statistics", query_last_part))
		result <- data.matrix(df)
		final_result[c,] = c(c,mean(result[,1]))
	}
	return(final_result)
}

plot_file_path <- sprintf('%svariation_of_cost_customised.pdf', experiment_dir)
pdf(plot_file_path)
db_path <- sprintf('%soutput.db', experiment_dir)
db <- dbConnect(SQLite(), dbname=db_path)
analysis(db)
dev.off()
