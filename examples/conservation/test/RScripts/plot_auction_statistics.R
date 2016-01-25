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
x_axis_param="cycle_number"
y_axis_params <- c("cost_of_all_bids", "cost_of_successful_bids", "number_of_bids", "number_of_successful_bids", "number_of_participans")

# process database

plot_pair <- function(a, b, all) {
        plot(all[,c(a,b)])
        title(main=paste("linear scatter:", a, "/", b))
}


analysis <- function(db) {
	for (j in 1:length(y_axis_params)){
		plot_file_path <- sprintf('%s%s.pdf', experiment_dir, y_axis_params[j])
		pdf(plot_file_path)

		for (i in 1:sampleCount ) {
			for(k in 1:REPLICATES){
				query_last_part <- sprintf('where replicate = "%s" and sample="%s" ORDER BY %s', k, i, x_axis_param)
				query_first_part <- sprintf('select %s, %s from', x_axis_param, y_axis_params[j])
				df = dbGetQuery(db, paste(query_first_part, "auction_statistics", query_last_part))
				
				if(k==1){
					result = data.matrix(df)
				} 
				else {
					result = result + data.matrix(df)
				}
			}


			result = result/REPLICATES
			plot_pair(x_axis_param, y_axis_params[j], result)
		}

		dev.off()
	}

}

db_path <- sprintf('%soutput.db', experiment_dir)
db <- dbConnect(SQLite(), dbname=db_path)
analysis(db)
