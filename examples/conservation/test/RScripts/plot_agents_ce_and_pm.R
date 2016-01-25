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

aggregate_plots <- function(db){
	print("Aggregate plots are drawn by taking the average of all replicate test rounds")
	plot_file_path <- sprintf('%sagents_averagece_and_averagepm.pdf', experiment_dir)
	pdf(plot_file_path)

	for (i in 1:sampleCount ) {
		output <- matrix(nrow=as.numeric(numCycles),ncol=3)

		for(j in 1:REPLICATES){
			query <- sprintf('where replicate = "%s" and sample="%s" ORDER BY %s', j, i, "cycle_number")
			df = dbGetQuery(db, paste("select cycle_number, average_CE, average_PM from", "auction_statistics", query))
			if(j==1){
				output = data.matrix(df)
			}
			else {
				output = output + data.matrix(df)
			}
		}
		
		output = output/REPLICATES

		if(i==1){
			plot_title <- sprintf('average CE and PM for all 33 parameter combinations\nparameter combination : %s', i)
		} else {
			plot_title <- sprintf('parameter combination : %s', i)
		}
		
		plot_pair("cycle_number", "average_CE", "average_PM", output, plot_title)
	}

	dev.off()	
}

plot_individual_agents <- function(db) { 
	print("The individual agents' CE and PM variations are drawn only for first replicate test run")
	for (i in 1:sampleCount ) {
		plot_file_path <- sprintf('%sagents_ce_and_pm_sample%s.pdf', experiment_dir, i)
		pdf(plot_file_path)

		for(j in 1:numAgents) {
			query_firstpart <- sprintf('select cycle_number, agent%s from', j)
			query_secondpart <- sprintf('where replicate = "1" and sample="%s"', i)
			column_name_ce = sprintf('agent%s ce', j)
			column_name_pm = sprintf('agent%s pm', j)

			df_1 = dbGetQuery(db, paste(query_firstpart, "agents_ce", query_secondpart))
			output_1 = as.matrix(df_1)

			df_2 = dbGetQuery(db, paste(query_firstpart, "agents_pm", query_secondpart))
			output_2 = as.matrix(df_2)

			output <- cbind(output_1, output_2[,2])
			colnames(output) <- c("cycle number",column_name_ce, column_name_pm)

			if(j == 1){
				query <- sprintf('where replicate = "1" and sample="%s" and cycle_number="1"', i)

				df = dbGetQuery(db, paste("select LCHP_agents, HCHP_agents, HCLP_agents from", "auction_statistics", query))
				result = as.matrix(df)
				highPMpercentage = as.numeric(result[1,1]) + as.numeric(result[1,2])
				highCEpercentage = as.numeric(result[1,3]) + as.numeric(result[1,2])

				title <- sprintf('parameter combination:%s\ninitial high CE agents percentage: %s\ninitial high PM agents percentage: %s\n agent %s\n', i, highCEpercentage, highPMpercentage, j)
				setTitle = "false"
			} else {
				title <- sprintf('agent %s', j);
			}

			plot_pair("cycle number", column_name_ce, column_name_pm, output, title)
		}
		dev.off()
	}
}

analysis <- function(db) {
	plot_individual_agents(db)
	aggregate_plots(db)
}

db_path <- sprintf('%soutput.db', experiment_dir)
db <- dbConnect(SQLite(), dbname=db_path)
analysis(db)
