#!/usr/bin/Rscript
library(sqldf)


#
# first input configuration parameter to the file should be 
# the path to experiment directory
#
#

# input & output variable names
args <- commandArgs(trailingOnly = TRUE)
experiment_dir=args[1]

# read config file
config_file_path <- sprintf('%sconfig', experiment_dir)
source(config_file_path, local = TRUE)

# read samples
samples_file_path <- sprintf('%ssamples.txt', experiment_dir)
samples = read.csv(samples_file_path, header = FALSE, sep='	')
sampleCount <- nrow(samples)
output_params <- c("sample_number","number_of_cycles_with_winners")

# process database

plot_pair <- function(a, b, all) {
        plot(all[,c(a,b)], ylim=range(c(0,100)))
        title(main=paste("linear scatter:", a, "/", b))
}


analysis <- function(db) {
	for(k in 1:REPLICATES){
		output <- matrix(nrow=sampleCount,ncol=length(output_params))
		colnames(output) <- output_params

		for (i in 1:sampleCount ) {

			query <- sprintf('where replicate = "%s" and sample="%s" and number_of_successful_bids != "0"', k, i)
			df = dbGetQuery(db, paste('select count(*) from', "auction_statistics", query))
			output[i,] = c(i,as.numeric(df[1,1]))
		}

		if(k==1){
			final_output = output
		}
		else {
			final_output = final_output + output
		}
	}

	final_output = final_output/REPLICATES

	plot_file_path <- sprintf('%snumber_of_auction_cycles_with_winners.pdf', experiment_dir)
	pdf(plot_file_path)

	plot_pair(output_params[1], output_params[2], final_output)

	dev.off()

}

db_path <- sprintf('%soutput.db', experiment_dir)
db <- dbConnect(SQLite(), dbname=db_path)
analysis(db)
