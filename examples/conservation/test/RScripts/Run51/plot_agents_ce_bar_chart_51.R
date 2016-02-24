#!/usr/bin/Rscript
library(sqldf)

#
# first input parameter to the file should be 
# the path to experiment directory
# second input paarmeter to the file should be the number of 
# agents in the application ( default : 100)
# third input parameter should be the number of cycles in each auction teast run (default : 100)
#

# input & output variable names
args <- commandArgs(trailingOnly = TRUE)
experiment_dir=args[1]
input_names = c('agentConservationEthicModifier','agentProfitMotivationModifier','socielNormUpdatePercentage','highCEAgentsPercentage')
output_names = c('numberOfAuctionCyclesWithWinners')
numAgents = args[2]
numCycles = args[3]

# read config file
config_file_path <- sprintf('%sconfig', experiment_dir)
source(config_file_path, local = TRUE) 

# read samples
samples_file_path <- sprintf('%ssamples.txt', experiment_dir)
samples = read.csv(samples_file_path, header = FALSE, sep='	')
colnames(samples) <- input_names
sampleCount <- nrow(samples)

# process database

plot_pair <- function(a, b, c, all, title) {
	


	y_label <- sprintf('%s(red), %s(green)', b, c)
        plot(all[,c(a,b)],col="red", xlab=a, ylab=y_label, ylim=range(c(0,100)))
	points(all[,c(a,c)],col="green")
        title(main=paste(title))
}

plot_individual_agents <- function(db) { 
	colNames = integer(numAgents)
	for(k in 1:numAgents){
		colNames[k] = k
	}

	plot_file_path <- sprintf('%sagents_ce_bar_chart.pdf',experiment_dir)
	pdf(plot_file_path)

	output <- matrix(nrow=2 ,ncol=as.numeric(numAgents))
	rownames(output) <- c('Initial CE', 'Final CE')
	colnames(output) <- colNames

	threshold = 50

	for(j in 1:numAgents) {
		query_firstpart <- sprintf('select agent%s from', j)
		query_secondpart_1 <- sprintf('where replicate = "5" and sample="2" and cycle_number="0"') 
		query_secondpart_2 <- sprintf('where replicate = "5" and sample="2" and cycle_number="%s"',numCycles)
		
		df_1 = dbGetQuery(db, paste(query_firstpart, "agents_ce", query_secondpart_1))
		output_1 = as.matrix(df_1)

		df_2 = dbGetQuery(db, paste(query_firstpart, "agents_ce", query_secondpart_2))
		output_2 = as.matrix(df_2)

		output[1,j] = as.numeric(output_1[1,1]) - threshold
		output[2,j] = as.numeric(output_2[1,1]) - threshold
	}

	print(output)
	write.csv(output, file = sprintf('%sMyData.csv',experiment_dir))

	barplot(output, xlab="Agents", ylab="CE Barometer", ylim=range(c(-50,50)), col=c("blue","red"),border=c("blue","red"),legend = rownames(output), beside=TRUE, axisnames=TRUE)

	dev.off()
}

analysis <- function(db) {
	plot_individual_agents(db)
}

db_path <- sprintf('%soutput.db', experiment_dir)
db <- dbConnect(SQLite(), dbname=db_path)
analysis(db)
