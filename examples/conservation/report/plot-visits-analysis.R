#!/usr/bin/Rscript
suppressMessages(library(sqldf))
library(ggplot2)
library(reshape2)
options(width=200)

# get the args
args <- commandArgs(trailingOnly = TRUE)

csvname <- args[1]

# read the csv
csv <- read.csv(file=csvname, header=TRUE, sep=",")
csv <- csv[c("Visit.type", "Visited.percentage", "Cost.relative.to.baseline")]
print(csv)

plot_file_path <- sprintf('%s.pdf', csvname)
pdf(plot_file_path)

	gg <- ggplot(data=csv, aes(x=Visited.percentage, y=Cost.relative.to.baseline, group=Visit.type, shape=Visit.type, color=Visit.type)) + 
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
  		xlab("Percentage of eligible landholders visited") +
  		ylab("Cost relative to baseline") +
		ggtitle(paste("Impact of visits in HC25 on total cost over all rounds")) +
  		guides(colour=guide_legend(title="")) +
  		guides(shape=guide_legend(title=""))
show(gg)
graphics.off()

