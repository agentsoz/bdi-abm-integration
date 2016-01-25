#!/usr/bin/Rscript
library(sqldf)


#
# first input parameter to the file should be 
# the path to experiment directory
# second input paarmeter to the file should be the number of 
# agents in the application ( default : 100)
# third input parameter should be the number of cycles in each auction teast run (default : 100)


# input & output variable names
args <- commandArgs(trailingOnly = TRUE)
experiment_dir=args[1]
numAgents=as.numeric(args[2])
auctionCycles=as.numeric(args[3])

# read config file
config_file_path <- sprintf('%sconfig', experiment_dir)
source(config_file_path, local = TRUE)

# read samples
samples_file_path <- sprintf('%ssamples.txt', experiment_dir)
samples = read.csv(samples_file_path, header = FALSE, sep='	')
sampleCount <- nrow(samples)
output_names <- c("cycle number", "average number of packages selected by an agent")

# process database

plot_pair <- function(a, b, all) {
        plot(all[,c(a,b)])
        title(main=paste(a, "/", b))
}


analysis <- function(db) {
	for (i in 1:sampleCount ) {
		
		for(r in 1:REPLICATES){
			output <- matrix(nrow=auctionCycles,ncol=length(output_names))
			colnames(output) <- output_names

			for(j in 1:auctionCycles ) {

				query <- sprintf('where replicate = "%s" and sample="%s" and cycle_number="%s" ORDER BY cycle_number', r, i, j)
				df = dbGetQuery(db, paste('select * from', "number_of_bids_per_agent", query))
				result <- as.matrix(df)

				sum = 0
				for(k in 1:numAgents){
					#always agent's details start from column number 4 = (k + 3)
					sum = sum + as.numeric(result[1,k+3])
				}
				sum = sum/numAgents
				output[j,] = c(j,sum)
			}

			if(r==1){
				final_result = output
			}
			else {
				final_result = final_result + output
			}
		}

		final_result = final_result/REPLICATES
		plot_pair(output_names[1], output_names[2], final_result)
	}

}

plot_file_path <- sprintf('%splots.pdf', experiment_dir)
pdf(plot_file_path)
db_path <- sprintf('%soutput.db', experiment_dir)
db <- dbConnect(SQLite(), dbname=db_path)
analysis(db)
dev.off()
