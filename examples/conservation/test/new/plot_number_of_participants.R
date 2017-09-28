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
y_axis_params <- c("LCHP_participants", "HCHP_participants", "LCLP_participants", "HCLP_participants", "HP_participants", "HC_participants", "All_participants")


# process database

plot_pair <- function(all, labels) {
	melted = melt(all, id.vars="cycle_number")
	gg <- ggplot(data=melted, aes(x=cycle_number, y=value, group=variable, shape=variable, color=variable)) + 
		geom_line() +
		geom_point(size=3) +
		#ylim(0,100) +
		scale_shape_manual(values = c(15, 17, 19, 8, 0, 2, 5)) +
		theme_bw() +
  		theme(
			legend.title=element_text(size=12,face="bold"),
        	axis.title=element_text(size=12,face="bold"), 
			plot.title=element_text(size=12,face="bold",hjust=0.5),
        	aspect.ratio=5/5
			) +
  		xlab("auction cycle") +
  		ylab("number of participants") +
		ggtitle(paste("Sample:", labels)) +
  		guides(colour=guide_legend(title="")) +
  		guides(shape=guide_legend(title=""))
	show(gg)

}


analysis <- function(db) {
	plot_file_path <- sprintf('%snumber_of_participants.pdf', experiment_dir)
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

		output <- matrix(ncol=8, nrow=nrow(result))
		slabels <- matrix(ncol=1, nrow=nrow(result))
		colnames(output) <- c(x_axis_param, y_axis_params)
		colnames(slabels) <- c("Sample")
		output[,1] = result[,1]
		output[,2] = result[,2]
		output[,3] = result[,3]
		output[,4] = result[,4]
		output[,5] = result[,5]
		output[,6] = result[,2]+result[,3]
		output[,7] = result[,3]+result[,5]
		output[,8] = result[,2]+result[,3]+result[,4]+result[,5]
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

