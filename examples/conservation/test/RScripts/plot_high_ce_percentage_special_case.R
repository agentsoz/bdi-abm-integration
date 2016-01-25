#!/usr/bin/Rscript
library(sqldf)


#
# first input parameter to the file should be 
# the path to experiment directory
#


# input & output variable names
args <- commandArgs(trailingOnly = TRUE)
experiment_dir=args[1]
output_names = c('highCE_agents_percentage','number_of_successful_bids')

# read config file
config_file_path <- sprintf('%sconfig', experiment_dir)
source(config_file_path, local = TRUE)

# process database

plot_pair <- function(a, b, all, title) {
        plot(all[,c(a,b)])
        title(title)
}


analysis <- function(db, input) {
	combined_output <- matrix(nrow=0,ncol=length(output_names))
        colnames(combined_output) <- output_names
	
	for (i in 1:REPLICATES ) {
		query1 <- sprintf('where cycle_number != "0" and replicate = "%s"', i)
		df1 = dbGetQuery(db, paste('select highCE_agents_percentage, number_of_successful_bids from', "auction_statistics", query1))
		output = as.matrix(df1)
		

		query2 <- sprintf('where replicate = "%s"', i)
		df2 = dbGetQuery(db, paste('select CE_seed, PM_seed from', "config_parameters", query2))
		seeds = as.matrix(df2)
		title <- sprintf('ce_seed=%s/pm_seed=%s', seeds[1,1], seeds[1,2])
		
		plot_pair(output_names[1], output_names[2], output, title)

		combined_output = rbind(combined_output, output);
	}

	plot_pair(output_names[1], output_names[2], combined_output, 'combined output')
}

plot_file_path <- sprintf('%shighCEAgentsPercentage_vs_numberOfSuccessfulBids.pdf', experiment_dir)
pdf(plot_file_path)
db_path <- sprintf('%soutput.db', experiment_dir)
db <- dbConnect(SQLite(), dbname=db_path)
analysis(db, samples)
dev.off()
