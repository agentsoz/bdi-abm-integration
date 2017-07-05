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
input_names = c('numAgents','highCEAgentsPercentage', 'targetPercentage', 'sigmoidMaxStepX')
output_names = c('number of cycles with winners','HC agents in last cycle', 'HP agents in last cycle', 'HC agents in final winning cycle', 'HP agents in final winning cycle', 'normalized cost of final round with winners', "average CE at last cycle - initial average CE", "average CE at last winning cycle - initial average CE", "accumulated LCLP participants at last winning cycle", "accumulated HCLP participants at last winning cycle", "accumulated LCHP participants at last winning cycle", "accumulated HCHP participants at last winning cycle", "accumulated LCLP winners at last winning cycle", "accumulated HCLP winners at last winning cycle", "accumulated LCHP winners at last winning cycle", "accumulated HCHP winners at last winning cycle", "number of agents changed groups (CE)", "number of agents changed groups (PM)")

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
#exit
	Msplit=corr.test(input, output, method="spearman", adjust="none")
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

			# read the number of cycles with winners
			query <- sprintf('where replicate = "%s" and sample="%s" and number_of_successful_bids != "0"', r, i)
			number_of_cycles_with_winners_df = dbGetQuery(db, paste('select count(*) from', "auction_statistics", query))

			# read highC and highP agents count at 100th round
			query <- sprintf('where replicate = "%s" and sample="%s" and cycle_number="%s"', r, i, cycles)
			high_C_P_agents = dbGetQuery(db, paste('select HCLP_agents, HCHP_agents, LCHP_agents from', "auction_statistics", query))

			# read the last winning round's number
			query <- sprintf('where replicate = "%s" and sample="%s" and number_of_successful_bids != "0"', r, i)			
			winning_cycles_df = dbGetQuery(db, paste('select cycle_number from', "auction_statistics", query))
			winning_cycles_mat = data.matrix(winning_cycles_df)
			last_winning_cycle = winning_cycles_mat[which.max(winning_cycles_mat),1]
			if (length(last_winning_cycle) == 0) { # if no match then set last winning cycle to the first cycle
				last_winning_cycle = 1
			}

			# read highC and highP agents count at last winning round
			query <- sprintf('where replicate = "%s" and sample="%s" and cycle_number="%s"', r, i, last_winning_cycle)
			high_C_P_agents_at_last_winning_cycle = dbGetQuery(db, paste('select HCLP_agents, HCHP_agents, LCHP_agents from', "auction_statistics", query))
			if(nrow(high_C_P_agents_at_last_winning_cycle) == 0) {
				high_C_P_agents_at_last_winning_cycle[1,] = c(0,0,0)
			}


			# read cost of final round with winners
			query <- sprintf('where replicate = "%s" and sample="%s" and cycle_number="%s"', r, i, last_winning_cycle)
			cost_of_last_winning_cycle = dbGetQuery(db, paste('select normalized_cost from', "auction_statistics", query))

			# read average CE at 100th cycle
			query <- sprintf('where replicate = "%s" and sample="%s" and cycle_number="%s"', r, i, cycles)
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


			# get list of unique agent ids
			query <- sprintf('where replicate = "%s" and sample="%s" and ( cycle_number="%03d" or cycle_number="%03d")', r, i, 0, as.numeric(cycles))
			agentIds <- dbGetQuery(db, paste('select distinct agentId from agents_ce', query))

			# read how many agents have changed their group of CE between first and last auction cycles
			CE_group_changes = 0
			for(id in agentIds[,1]) {
				query <- sprintf('where replicate = "%s" and sample="%s" and agentId="%s" and ( cycle_number="%03d" or cycle_number="%03d")', r, i, id, 0, as.numeric(cycles))
				agents_CE = dbGetQuery(db, paste('select value from', 'agents_ce', query))
				if(as.numeric(agents_CE[1,1]) > 50 ){
					if(as.numeric(agents_CE[2,1]) <= 50 ){
						CE_group_changes <- CE_group_changes+1
					}
				} else {
					if(as.numeric(agents_CE[2,1]) > 50 ){
						CE_group_changes <- CE_group_changes+1
					}
				}
			}

			PM_group_changes = 0
			for(id in agentIds[,1]) {
				query <- sprintf('where replicate = "%s" and sample="%s" and agentId="%s" and ( cycle_number="%03d" or cycle_number="%03d")', r, i, id, 0, as.numeric(cycles))
				agents_PM = dbGetQuery(db, paste('select value from', 'agents_pm', query))
				if(as.numeric(agents_PM[1,1]) > 50 ){
					if(as.numeric(agents_PM[2,1]) <= 50 ){
						PM_group_changes <- PM_group_changes+1
					}
				} else {
					if(as.numeric(agents_PM[2,1]) > 50 ){
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

