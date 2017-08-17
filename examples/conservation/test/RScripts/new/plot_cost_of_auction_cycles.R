#!/usr/bin/Rscript
suppressMessages(library(sqldf))
library(ggplot2)
library(reshape2)


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

#plot_pair <- function(a, b, all) {
#        plot(all[,c(a,b)])
#        title(main=paste("linear scatter:", a, "/", b))
#}
plot_pair <- function(all, labels) {
	melted = melt(all, id.vars="cycle_number")
	gg <- ggplot(data=melted, aes(x=cycle_number, y=value, group=variable, shape=variable, color=variable)) + 
		geom_line() +
		geom_point(size=3) +
		#ylim(0,100) +
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
	plot_file_path <- sprintf('%scost_of_auction_cycles.pdf', experiment_dir)
	pdf(plot_file_path)
	for (i in 1:sampleCount ) {

		final_result <- matrix(nrow=numCycles,ncol=length(output_names))
		colnames(final_result) <- output_names

		for( c in 1:numCycles){
			query_last_part <- sprintf('where sample="%s" and cycle_number="%s" and number_of_successful_bids != "0" ', i, c)
			query_first_part <- sprintf('select %s from', output_names[2])
			query <- paste(query_first_part, "auction_statistics", query_last_part)
			df = dbGetQuery(db, query)
			result <- data.matrix(df)
			if(nrow(df) == 0) {
				final_result[c,] = c(c,0)
			} else {
				final_result[c,] = c(c,mean(result[,1]))
			}
		}
		print(final_result)
		#plot_pair(output_names[1], output_names[2], final_result)
		slabels <- matrix(ncol=1, nrow=nrow(final_result))
		slabels[,1] = paste(samples[i,],collapse=" ")
		plot_pair(as.data.frame(final_result), slabels)


	}
	graphics.off()
}

db_path <- sprintf('%soutput.db', experiment_dir)
db <- dbConnect(SQLite(), dbname=db_path)
analysis(db)

