#!/usr/bin/Rscript
library(sqldf)
require("lattice")
library("lattice")


#
# first input parameter to the file should be 
# the path to experiment directory
#

# input & output variable names
args <- commandArgs(trailingOnly = TRUE)
experiment_dir=args[1]
input_names = c('conservationEthicModifier','profitMotivationModifier','socialNormUpdatePercentage','highCEAgentsPercentage')
output_names = c('number of cycles with winners','HC agents in cycle 100', 'HP agents in cycle 100', 'normalized cost of final round with winners', "average CE at 100th cycle - initial average CE", "accumulated LCLP participants at last winning cycle", "accumulated HCLP participants at last winning cycle", "accumulated LCHP participants at last winning cycle", "accumulated HCHP participants at last winning cycle", "accumulated LCLP winners at last winning cycle", "accumulated HCLP winners at last winning cycle", "accumulated LCHP winners at last winning cycle", "accumulated HCHP winners at last winning cycle", "number of agents changed groups (CE)", "number of agents changed groups (PM)")

# read config file
config_file_path <- sprintf('%sconfig', experiment_dir)
source(config_file_path, local = TRUE)

# read samples
samples_file_path <- sprintf('%ssamples.txt', experiment_dir)
samples = read.csv(samples_file_path, header = FALSE, sep='	')
colnames(samples) <- input_names
sampleCount <- nrow(samples)

# process database

plot_all <- function(all, input, output) {

	all <- all[sort.list(all[,3]), ]

	# Add extra space to right of plot area; change clipping to figure
	par(mar=c(4.1, 4.1, 16, 1), xpd=TRUE)
	#par(mar=c(4.1, 4.1, 13, 1), xpd=TRUE)

        plot(all[,c(input_names[3],output_names[1])],col="orange", xlab=input_names[4], type = "l", ylab="Outputs", ylim=range(c(-22,105)))
	lines(all[,c(input_names[3],output_names[2])],col="green")
	lines(all[,c(input_names[3],output_names[3])],col="red")
	lines(all[,c(input_names[3],output_names[4])],col="pink")
	lines(all[,c(input_names[3],output_names[5])],col="yellow")
	lines(all[,c(input_names[3],output_names[6])],col="black")
	lines(all[,c(input_names[3],output_names[7])],col="grey")
	lines(all[,c(input_names[3],output_names[8])],col="brown")
	lines(all[,c(input_names[3],output_names[9])],col="blue")
	lines(all[,c(input_names[3],output_names[10])],col="pink", lty=2, lwd=2)
	lines(all[,c(input_names[3],output_names[11])],col="green", lty=2, lwd=2)
	lines(all[,c(input_names[3],output_names[12])],col="red", lty=2, lwd=2)
	lines(all[,c(input_names[3],output_names[13])],col="black", lty=2, lwd=2)
	lines(all[,c(input_names[3],output_names[14])],col="brown", lty=2, lwd=2)
	lines(all[,c(input_names[3],output_names[15])],col="blue", lty=2, lwd=2)
       # title("linear scatter:percentage of paticipants in each category")
	legend(4, 267, c(output_names[1],output_names[2], output_names[3], output_names[4], output_names[5], output_names[6],output_names[7], output_names[8], output_names[9], output_names[10], output_names[11],output_names[12], output_names[13], output_names[14], output_names[15]), lty=c(1,1,1,1,1,1,1,1,1,2,2,2,2,2,2), lwd=c(1.5,1.5,1.5,1.5,1.5,1.5,1.5,1.5,1.5,1.5,2,2,2,2,2,2),col=c("orange","green", "red", "pink", "yellow", "black", "grey", "brown", "blue", "pink", "green", "red", "black", "brown", "blue")) 
	#legend(4, 209.5, c(output_names[1], output_names[3], output_names[4], output_names[6],output_names[7], output_names[8], output_names[10], output_names[11],output_names[12], output_names[13], output_names[14], output_names[15]), lty=c(1,1,1,1,1,1,2,2,2,2,2,2), lwd=c(1.5,1.5,1.5,1.5,1.5,1.5,2,2,2,2,2,2,2),col=c("orange", "red", "pink", "black", "grey", "brown", "pink", "green", "red", "black", "brown", "blue")) 
}


analysis <- function(db) {
	# Read inputs
	input <- matrix(nrow=sampleCount,ncol=length(input_names))
	colnames(input) <- input_names

	for (i in 1:sampleCount){
		query_last_part <- sprintf('where replicate = "1" and sample = "%s"', i)
		query_first_part <- sprintf('select %s, %s, %s, %s from', input_names[1], input_names[2], input_names[3], input_names[4])
		df = dbGetQuery(db, paste(query_first_part, "config_parameters", query_last_part))
		input[i,] = as.numeric(df[1,])
	}

	# Read outputs
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
						s <- sprintf('before %s HIGH | after %s LOW', agents_CE[1,k], agents_CE[2,k])
						CE_group_changes <- CE_group_changes+1
					}
				} else {
					if(as.numeric(agents_CE[2,k]) > 50 ){
						s <- sprintf('before %s LOW | after %s HIGH', agents_CE[1,k], agents_CE[2,k])
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
						s <- sprintf('before %s HIGH | after %s LOW', agents_PM[1,k], agents_PM[2,k])
						PM_group_changes <- PM_group_changes+1
					}
				} else {
					if(as.numeric(agents_PM[2,k]) > 50 ){
						s <- sprintf('before %s LOW | after %s HIGH', agents_PM[1,k], agents_PM[2,k])
						PM_group_changes <- PM_group_changes+1
					}
				}
			}

			temp_output[i,] = c(number_of_cycles_with_winners_df[1,1], 
						as.numeric(high_C_P_agents[1,1]) + as.numeric(high_C_P_agents[1,2]), 
						as.numeric(high_C_P_agents[1,3]) + as.numeric(high_C_P_agents[1,2]),
						as.numeric(cost_of_last_winning_cycle[1,1]),
						as.numeric(average_CE_hundredth_round[1,1]) - as.numeric(initial_average_CE[1,1]),
						as.numeric(participants_winners_percentage[1,1])/100,
						as.numeric(participants_winners_percentage[1,2])/100,
						as.numeric(participants_winners_percentage[1,3])/100,
						as.numeric(participants_winners_percentage[1,4])/100,
						as.numeric(participants_winners_percentage[1,5])/100,
						as.numeric(participants_winners_percentage[1,6])/100,
						as.numeric(participants_winners_percentage[1,7])/100,
						as.numeric(participants_winners_percentage[1,8])/100,
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

	# combine samples with results
	all = cbind(input, output/REPLICATES)

	plot_all(all, input, output/REPLICATES)
}

plot_file_path <- sprintf('%soutputs_vs_socialNormUpdatePercentage.pdf', experiment_dir)
pdf(plot_file_path)
db_path <- sprintf('%soutput.db', experiment_dir)
db <- dbConnect(SQLite(), dbname=db_path)
analysis(db)
dev.off()
