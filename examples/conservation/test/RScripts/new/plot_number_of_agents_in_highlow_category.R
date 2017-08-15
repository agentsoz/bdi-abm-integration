#!/usr/bin/Rscript
suppressMessages(library(sqldf))
library(ggplot2)
library(reshape2)


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
y_axis_params <- c("HP_agents", "HC_agents")

# process database

plot_pair <- function(all, labels) {
	melted = melt(all, id.vars="cycle_number")
	gg <- ggplot(data=melted, aes(x=cycle_number, y=value, group=variable, shape=variable, color=variable)) + 
		geom_line() +
		geom_point(size=3) +
		ylim(0,100) +
		theme_bw() +
  		theme(
			legend.title=element_text(size=12,face="bold"),
        	axis.title=element_text(size=12,face="bold"), 
			plot.title=element_text(size=12,face="bold",hjust=0.5),
        	aspect.ratio=5/5
			) +
  		xlab("auction cycle") +
  		ylab("number of agents") +
		ggtitle(paste("Sample:", labels)) +
  		guides(colour=guide_legend(title="")) +
  		guides(shape=guide_legend(title=""))
	show(gg)
}


analysis <- function(db) {
	plot_file_path <- sprintf('%snumber_of_agents_in_highlow_categories.pdf', experiment_dir)
	pdf(plot_file_path)

	for (i in 1:sampleCount ) {
		for(k in 1:REPLICATES){
			query_last_part <- sprintf('where replicate = "%s" and sample="%s" ORDER BY %s', k, i, x_axis_param)
			query_first_part <- sprintf('select cycle_number, HCLP_agents, LCHP_agents, HCHP_agents, LCLP_agents from')
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
		slabels <- matrix(ncol=1, nrow=nrow(result))
		colnames(output) <- c(x_axis_param, y_axis_params)
		colnames(slabels) <- c("Sample")
		output[,1] = result[,1]
		output[,2] = result[,3]+result[,4]
		output[,3] = result[,2]+result[,4]
		output = output[order(output[,"cycle_number"]),]
		print(output)

		slabels[,1] = paste(samples[i,],collapse=" ")
		plot_pair(as.data.frame(output), slabels)
	}

	graphics.off()
}

db_path <- sprintf('%soutput.db', experiment_dir)
db <- dbConnect(SQLite(), dbname=db_path)
analysis(db)
