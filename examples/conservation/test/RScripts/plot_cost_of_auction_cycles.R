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

plot_pair <- function(a, b, all) {
        plot(all[,c(a,b)])
        title(main=paste("linear scatter:", a, "/", b))
}


analysis <- function(db) {
	for (i in 1:sampleCount ) {
		final_result <- matrix(nrow=numCycles,ncol=length(output_names))
		colnames(final_result) <- output_names

		for( c in 1:numCycles){
			query_last_part <- sprintf('where sample="%s" and cycle_number="%s" and number_of_successful_bids != "0" ', i, c)
			query_first_part <- sprintf('select %s from', output_names[2])
			df = dbGetQuery(db, paste(query_first_part, "auction_statistics", query_last_part))
			result <- data.matrix(df)
			final_result[c,] = c(c,mean(result[,1]))
		}

		plot_pair(output_names[1], output_names[2], final_result)
	}

}

plot_file_path <- sprintf('%svariation_of_cost.pdf', experiment_dir)
pdf(plot_file_path)
db_path <- sprintf('%soutput.db', experiment_dir)
db <- dbConnect(SQLite(), dbname=db_path)
analysis(db)
dev.off()
