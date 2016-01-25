#!/usr/bin/Rscript
library(sqldf)


#
# first input parameter to the file should be 
# the path to experiment directory
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
output_names <- c("cycle_number", "highCE_agents_percentage")

# process database

plot_pair <- function(a, b, all) {
        plot(all[,c(a,b)], ylim=range(c(0,100)))
        title(main=paste("linear scatter:", a, "/", b))
}


analysis <- function(db) {
	for (i in 1:sampleCount ) {

		for(r in 1:REPLICATES){
			query_last_part <- sprintf('where replicate = "%s" and sample="%s" ORDER BY %s', r, i, output_names[1])
			query_first_part <- sprintf('select %s, %s from', output_names[1], output_names[2])
			df = dbGetQuery(db, paste(query_first_part, "auction_statistics", query_last_part))
			result <- data.matrix(df)

			if(r == 1){
				final_result = result
			}
			else {
				final_result = final_result + result
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
