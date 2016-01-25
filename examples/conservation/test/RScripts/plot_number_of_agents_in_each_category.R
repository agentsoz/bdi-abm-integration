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
y_axis_params <- c("LCLP_agents", "HCLP_agents", "LCHP_agents", "HCHP_agents")

# process database

plot_pair <- function(all) {
	y_label <- sprintf('LCLP(orange), HCLP(green), LCHP(red), HCHP(blue)')
        plot(all[,c(x_axis_param,y_axis_params[1])],col="orange", xlab=x_axis_param, ylab=y_label, ylim=range(c(0,100)))
	points(all[,c(x_axis_param,y_axis_params[2])],col="green")
	points(all[,c(x_axis_param,y_axis_params[3])],col="red")
	points(all[,c(x_axis_param,y_axis_params[4])],col="blue")
        title("linear scatter:number of agents in each category")
}


analysis <- function(db) {
	plot_file_path <- sprintf('%snumber _of_agents_in_each_category.pdf', experiment_dir)
	pdf(plot_file_path)

	for (i in 1:sampleCount ) {
		for(k in 1:REPLICATES){
			query_last_part <- sprintf('where replicate = "%s" and sample="%s" ORDER BY %s', k, i, x_axis_param)
			query_first_part <- sprintf('select %s, %s, %s, %s, %s from', x_axis_param, y_axis_params[1], y_axis_params[2], y_axis_params[3], y_axis_params[4])
			df = dbGetQuery(db, paste(query_first_part, "auction_statistics", query_last_part))
			
			if(k==1){
				result = data.matrix(df)
			} 
			else {
				result = result + data.matrix(df)
			}
		}


		result = result/REPLICATES
		plot_pair(result)
	}

	dev.off()
}

db_path <- sprintf('%soutput.db', experiment_dir)
db <- dbConnect(SQLite(), dbname=db_path)
analysis(db)
