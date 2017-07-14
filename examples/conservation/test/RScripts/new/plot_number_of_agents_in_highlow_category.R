#!/usr/bin/Rscript
suppressMessages(library(sqldf))


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
y_axis_params <- c("HC_agents", "HP_agents")

# process database

plot_pair <- function(all) {
	y_label <- sprintf('HC_agents(blue), HP_agents(red)')
        plot(all[,c(x_axis_param,y_axis_params[1])],col="blue", xlab=x_axis_param, ylab=y_label, ylim=range(c(0,100)))
	points(all[,c(x_axis_param,y_axis_params[2])],col="red")
        title("linear scatter:number of agents in each category")
}


analysis <- function(db) {
	plot_file_path <- sprintf('%snumber_of_agents_in_highlow_categories.pdf', experiment_dir)
	pdf(plot_file_path)

	for (i in 1:sampleCount ) {
		for(k in 1:REPLICATES){
			query_last_part <- sprintf('where replicate = "%s" and sample="%s" ORDER BY %s', k, i, x_axis_param)
			query_first_part <- sprintf('select cycle_number, HCLP_agents, LCHP_agents, HCHP_agents from')
			df = dbGetQuery(db, paste(query_first_part, "auction_statistics", query_last_part))
			
			if(k==1){
				result = data.matrix(df)
			} 
			else {
				result = result + data.matrix(df)
			}
		}


		result = result/REPLICATES

		output <- matrix(ncol=3, nrow=nrow(result))
		colnames(output) <- c("cycle_number", "HC_agents", "HP_agents")
		output[,1] = result[,1]
		output[,2] = result[,2]+result[,4]
		output[,3] = result[,3]+result[,4]
		plot_pair(output)
	}

	graphics.off()
}

db_path <- sprintf('%soutput.db', experiment_dir)
db <- dbConnect(SQLite(), dbname=db_path)
analysis(db)
