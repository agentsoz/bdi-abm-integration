#!/usr/bin/Rscript
library(sqldf)
library(corrplot)
require(psych)

#
# first input parameter to the file should be 
# the path to experiment directory
#


# input & output variable names
args <- commandArgs(trailingOnly = TRUE)
experiment_dir=args[1]
input_names = c('agentConservationEthicModifier','agentProfitMotivationModifier','socielNormUpdatePercentage','highCEAgentsPercentage')
output_names = c('number of cycles with winners','HC agents in cycle 100', 'HP agents in cycle 100', 'HC agents in final winning cycle', 'HP agents in final winning cycle', 'normalized cost of final round with winners', "average CE at 100th cycle - initial average CE", "average CE at last winning cycle - initial average CE", "accumulated LCLP participants at last winning cycle", "accumulated HCLP participants at last winning cycle", "accumulated LCHP participants at last winning cycle", "accumulated HCHP participants at last winning cycle", "accumulated LCLP winners at last winning cycle", "accumulated HCLP winners at last winning cycle", "accumulated LCHP winners at last winning cycle", "accumulated HCHP winners at last winning cycle", "number of agents changed groups (CE)", "number of agents changed groups (PM)")

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
	Msplit=corr.test(input, output, method="spearman", adjust="none")
	plot_file_path <- sprintf('%scorrelation_diagram.pdf', experiment_dir)
	pdf(plot_file_path)
	corrplot(Msplit$r, p.mat = Msplit$p, method = "circle")

	dev.off()
	print("Successful")
}

read_output <- function(db) {

	for(r in 1:REPLICATES){
		temp_output <- matrix(nrow=sampleCount,ncol=length(output_names))
		colnames(temp_output) <- output_names

		for (i in 1:sampleCount ) {

			# read the number of cycles with winners
			query <- sprintf('where replicate = "%s" and sample="%s" and number_of_successful_bids != "0"', r, i)
			number_of_cycles_with_winners_df = dbGetQuery(db, paste('select count(*) from', "auction_statistics", query))

			# read highC and highP agents count at 100th round
			query <- sprintf('where replicate = "%s" and sample="%s" and cycle_number="100"', r, i)
			high_C_P_agents = dbGetQuery(db, paste('select HCLP_agents, HCHP_agents, LCHP_agents from', "auction_statistics", query))

			# read the last winning round's number
			query <- sprintf('where replicate = "%s" and sample="%s" and number_of_successful_bids != "0"', r, i)			
			winning_cycles_df = dbGetQuery(db, paste('select cycle_number from', "auction_statistics", query))
			winning_cycles_mat = data.matrix(winning_cycles_df)
			last_winning_cycle = winning_cycles_mat[which.max(winning_cycles_mat),1]

			# read highC and highP agents count at last winning round
			query <- sprintf('where replicate = "%s" and sample="%s" and cycle_number="%s"', r, i, last_winning_cycle)
			high_C_P_agents_at_last_winning_cycle = dbGetQuery(db, paste('select HCLP_agents, HCHP_agents, LCHP_agents from', "auction_statistics", query))

			# read cost of final round with winners
			query <- sprintf('where replicate = "%s" and sample="%s" and cycle_number="%s"', r, i, last_winning_cycle)
			cost_of_last_winning_cycle = dbGetQuery(db, paste('select normalized_cost from', "auction_statistics", query))

			# read average CE at 100th cycle
			query <- sprintf('where replicate = "%s" and sample="%s" and cycle_number="100"', r, i)
			average_CE_hundredth_round = dbGetQuery(db, paste('select average_CE from', "auction_statistics", query))

			# read average CE at last winning cycle
			query <- sprintf('where replicate = "%s" and sample="%s" and cycle_number="%s"', r, i, last_winning_cycle)
			average_CE_last_winning_round = dbGetQuery(db, paste('select average_CE from', "auction_statistics", query))

			# read initial average CE
			query <- sprintf('where replicate = "%s" and sample="%s" and cycle_number="1"', r, i)
			initial_average_CE = dbGetQuery(db, paste('select average_CE from', "auction_statistics", query))

			# read participants % in last winning cycle
			query <- sprintf('where replicate = "%s" and sample="%s" and cycle_number="%s"', r, i, last_winning_cycle)
			participants_winners_percentage = dbGetQuery(db, paste('select LCLP_participants_accumulated_percentage, HCLP_participants_accumulated_percentage, LCHP_participants_accumulated_percentage, HCHP_participants_accumulated_percentage, LCLP_winners_accumulated_percentage,HCLP_winners_accumulated_percentage, LCHP_winners_accumulated_percentage, HCHP_winners_accumulated_percentage from', "auction_statistics", query))

			# read how many agents have changed their group of CE during 100 auction cycles
			query <- sprintf('where replicate = "%s" and sample="%s" and ( cycle_number="0" or cycle_number="100")', r, i)
			agents_CE = dbGetQuery(db, paste('select * from', "agents_ce", query))
			CE_group_changes = 0
			for(k in 4:(ncol(agents_CE))){
				if(as.numeric(agents_CE[1,k]) > 50 ){
					if(as.numeric(agents_CE[2,k]) <= 50 ){
						CE_group_changes <- CE_group_changes+1
					}
				} else {
					if(as.numeric(agents_CE[2,k]) > 50 ){
						CE_group_changes <- CE_group_changes+1
					}
				}
			}

			query <- sprintf('where replicate = "%s" and sample="%s" and ( cycle_number="0" or cycle_number="100")', r, i)
			agents_PM = dbGetQuery(db, paste('select * from', "agents_pm", query))
			PM_group_changes = 0
			for(k in 4:(ncol(agents_PM))){
				if(as.numeric(agents_PM[1,k]) > 50 ){
					if(as.numeric(agents_PM[2,k]) <= 50 ){
						PM_group_changes <- PM_group_changes+1
					}
				} else {
					if(as.numeric(agents_PM[2,k]) > 50 ){
						PM_group_changes <- PM_group_changes+1
					}
				}
			}

			temp_output[i,] = c(number_of_cycles_with_winners_df[1,1], 
						as.numeric(high_C_P_agents[1,1]) + as.numeric(high_C_P_agents[1,2]), 
						as.numeric(high_C_P_agents[1,3]) + as.numeric(high_C_P_agents[1,2]),
						as.numeric(high_C_P_agents_at_last_winning_cycle[1,1]) + as.numeric(high_C_P_agents_at_last_winning_cycle[1,2]), 
						as.numeric(high_C_P_agents_at_last_winning_cycle[1,3]) + as.numeric(high_C_P_agents_at_last_winning_cycle[1,2]),
						as.numeric(cost_of_last_winning_cycle[1,1]),
						as.numeric(average_CE_hundredth_round[1,1]) - as.numeric(initial_average_CE[1,1]),
						as.numeric(average_CE_last_winning_round[1,1]) - as.numeric(initial_average_CE[1,1]),
						as.numeric(participants_winners_percentage[1,1]),
						as.numeric(participants_winners_percentage[1,2]),
						as.numeric(participants_winners_percentage[1,3]),
						as.numeric(participants_winners_percentage[1,4]),
						as.numeric(participants_winners_percentage[1,5]),
						as.numeric(participants_winners_percentage[1,6]),
						as.numeric(participants_winners_percentage[1,7]),
						as.numeric(participants_winners_percentage[1,8]),
						CE_group_changes,
						PM_group_changes
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
