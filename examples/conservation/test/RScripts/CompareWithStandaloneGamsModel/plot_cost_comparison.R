#!/usr/bin/Rscript
library(sqldf)


#
# first input parameter to the file should be 
# the path to experiment directory
# second input parameter should be the number of auction cycles per test run.
#

# input & output variable names
args <- commandArgs(trailingOnly = TRUE)
experiment_dir=args[1]
numCycles=as.numeric(args[2])
sample_names=c("highCEAgentsPercentage = 10%","highCEAgentsPercentage = 25%","highCEAgentsPercentage = 50%","highCEAgentsPercentage = 75%","highCEAgentsPercentage = 95%")

# process database
plot_pair <- function(gams_result, conservation_result) {
	# Add extra space to right of plot area; change clipping to figure
	par(mar=c(4.1, 4.1, 8, 1), xpd=TRUE)
        
	plot(conservation_result[,c("cycle_number",sample_names[1])],col="green",type="l", xlab="cycle_number", ylab="cost in million dollars", ylim=range(c(4.5,12)), lwd=2)
	lines(conservation_result[,c("cycle_number",sample_names[2])],col="orange", lwd=2)
	lines(conservation_result[,c("cycle_number",sample_names[3])],col="blue", lwd=2)
	lines(conservation_result[,c("cycle_number",sample_names[4])],col="pink", lwd=2)
	lines(conservation_result[,c("cycle_number",sample_names[5])],col="yellow", lwd=2)
	points(gams_result[,c("cycle_number","cost")],col="red", lwd=2)
        #title(main=paste("outputs comparison"))
	legend(20, 15, c(sample_names[1],sample_names[2], sample_names[3], sample_names[4], sample_names[5], "standalone_gams"),lty=c(1,1,1,1,1,1), col=c("green", "orange", "blue", "pink", "yellow", "red"), lwd=c(2,2,2,2,2,2)) 
}

# read samples
samples_file_path <- sprintf('%ssamples.txt', experiment_dir)
samples = read.csv(samples_file_path, header = FALSE, sep='	')
sampleCount <- nrow(samples)

analysis <- function(cons_db,gams_db) {
	plot_file_path <- sprintf('%scost_comparison.pdf', experiment_dir)
	pdf(plot_file_path)

	gams_result <- matrix(nrow=1,ncol=2)
	colnames(gams_result) <- c("cycle_number","cost")
	df = dbGetQuery(gams_db, paste('select payment from', "gams_output", sprintf('where round="100.0"')))
	g_result <- data.matrix(df)
	gams_result[1,] = c(1,mean(g_result[,1]))
	
	conservation_result <- matrix(nrow=numCycles,ncol=sampleCount+1)
	colnames(conservation_result) <- c("cycle_number",sample_names[1], sample_names[2], sample_names[3], sample_names[4], sample_names[5])
	for( c in 1:numCycles){
		conservation_result[c,1] = c
	}

	for ( s in 1:sampleCount){
		for( c in 1:numCycles){
			df = dbGetQuery(cons_db, paste('select cost_of_successful_bids from', "auction_statistics", sprintf('where sample="%s" and cycle_number="%s" and number_of_successful_bids != "0" ', s, c)))
			c_result <- data.matrix(df)
			conservation_result[c,s+1] = mean(c_result[,1])
		}
	}

	plot_pair(gams_result, conservation_result)
	dev.off()
}

# connect with conservation db
cons_db_path <- sprintf('%soutput.db', experiment_dir)
cons_db <- dbConnect(SQLite(), dbname=cons_db_path)

# connect with gams db
gams_db_path <- sprintf('%sgams_output.db', experiment_dir)
gams_db <- dbConnect(SQLite(), dbname=gams_db_path)

analysis(cons_db,gams_db)
