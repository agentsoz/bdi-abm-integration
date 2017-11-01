#!/usr/bin/Rscript
suppressMessages(library(sqldf))
suppressMessages(library(corrplot))
suppressMessages(library(psych))

#
# first input parameter to the file should be 
# the path to experiment directory
#
# second should be the number of auction cycles in


# input & output variable names
args <- commandArgs(trailingOnly = TRUE)
experiment_dir=args[1]
cycles=args[2]
input_names = c('numAgents','highCEAgentsPercentage', 'targetPercentage', 'sigmoidMaxStepX', 'profitMotiveUpdateMultiplier', 'socialNormUpdateMultiplier', 'visitType', 'visitPercentage', 'visitPercentagePerLandholder')
output_names = c(
	"LCHP % at simulation end", "HCHP % at simulation end", "LCLP % at simulation end", "HCLP % at simulation end",
	"LCHP % over all rounds", "HCHP % over all rounds", "LCLP % over all rounds", "HCLP % over all rounds",
    "Participation % over all rounds",
    "Cost per agent over all rounds",
	"CE social norm change",
	"Visits per agent over all rounds"
)

# read config file
config_file_path <- sprintf('%sconfig', experiment_dir)
source(config_file_path, local = TRUE)

# read input
samples_file_path <- sprintf('%ssamples.txt', experiment_dir)
samples = read.csv(samples_file_path, header = FALSE, sep='	')
colnames(samples) <- input_names
sampleCount <- nrow(samples)

# draw correlation graphs database
draw_correlation_graph <- function(input, output){
	print(input)
	print(output)
	Msplit=corr.test(input, output, method="spearman", adjust="none") # use something like input[,1:3] to restrict what is plotted
	plot_file_path <- sprintf('%scorrelation_diagram.pdf', experiment_dir)
	pdf(plot_file_path,width=11.7, height=8.3) # A4 landscape
	corrplot(Msplit$r, 
		p.mat = Msplit$p, 
		method = "circle", 
		insig="p-value", 
		sig.level=0.05,
		tl.srt=75,
	)
  # use graphics.off() instead of dev.off() to avoid "null device 1" in batch mode
  graphics.off() 
}

read_output <- function(db) {

	for(r in 1:REPLICATES){
		temp_output <- matrix(nrow=sampleCount,ncol=length(output_names))
		colnames(temp_output) <- output_names

		for (i in 1:sampleCount ) {

			# read total numbers of all types of landholders
	        query <- sprintf('where replicate = "%s" and sample="%s" ORDER BY cycle_number', r, i)
            agentsTypesPerCycle = dbGetQuery(db, paste("select LCHP_agents, HCHP_agents, LCLP_agents, HCLP_agents from auction_statistics", query))
 			agentsTypesPerCycle = data.matrix(agentsTypesPerCycle)
			agentsTypesAllCycles = colSums(agentsTypesPerCycle)
            agentsAllCycles = sum(agentsTypesAllCycles)
		
			# read final numbers of all types of landholders 
			query <- sprintf('where replicate = "%s" and sample="%s" and cycle_number="%s"', r, i, cycles)
			agentsTypesLastCycle = dbGetQuery(db, paste("select LCHP_agents, HCHP_agents, LCLP_agents, HCLP_agents from auction_statistics", query))
			agentsTypesLastCycle=as.numeric(agentsTypesLastCycle)
			agentsTypesCountLastCycle = sum(agentsTypesLastCycle)

			# read participation details
	        query <- sprintf('where replicate = "%s" and sample="%s" ORDER BY cycle_number', r, i)
			participantsTypesPerCycle = dbGetQuery(db, paste('select LCHP_participants, HCHP_participants, LCLP_participants, HCLP_participants from auction_statistics', query))
 			participantsTypesPerCycle = data.matrix(participantsTypesPerCycle)
			participantsAllCycles = sum(participantsTypesPerCycle)

			# read cost of successful bids
	        query <- sprintf('where replicate = "%s" and sample="%s" ORDER BY cycle_number', r, i)
			cost <- dbGetQuery(db, paste('select cost_of_successful_bids from auction_statistics', query))
			cost <- sum(data.matrix(cost))

			# calculate change in CE social norm 
			query <- sprintf('where replicate = "%s" and sample="%s" and cycle_number="1"', r, i)
			averageNormFirstRound <- dbGetQuery(db, paste('select average_CE from auction_statistics', query))
			query <- sprintf('where replicate = "%s" and sample="%s" and cycle_number="%s"', r, i, cycles)
			averageNormLastRound <- dbGetQuery(db, paste('select average_CE from auction_statistics', query))
			changeInNorm = as.numeric(averageNormLastRound) - as.numeric(averageNormFirstRound)
	
			# calculate total visits over all rounds
	        query <- sprintf('where replicate = "%s" and sample="%s" ORDER BY cycle_number', r, i)
			visits <- dbGetQuery(db, paste('select number_of_visits from auction_statistics', query))
			visits <- sum(data.matrix(cost))

			temp_output[i,] = c(
				agentsTypesLastCycle[1]/agentsTypesCountLastCycle, 
				agentsTypesLastCycle[2]/agentsTypesCountLastCycle, 
				agentsTypesLastCycle[3]/agentsTypesCountLastCycle, 
				agentsTypesLastCycle[4]/agentsTypesCountLastCycle,
           		agentsTypesAllCycles[1]/agentsAllCycles,
           		agentsTypesAllCycles[2]/agentsAllCycles,
           		agentsTypesAllCycles[3]/agentsAllCycles,
           		agentsTypesAllCycles[4]/agentsAllCycles,
           		participantsAllCycles/agentsAllCycles,
           		cost/agentsAllCycles,
				changeInNorm,
				visits/agentsAllCycles
			)
		}

		if(r==1) {
			output = temp_output
		}
		else {
			output = output + temp_output
		}
	}

	output/REPLICATES
}

db_path <- sprintf('%soutput.db', experiment_dir)
db <- dbConnect(SQLite(), dbname=db_path)
draw_correlation_graph(samples, read_output(db));
dbDisconnect(db)

