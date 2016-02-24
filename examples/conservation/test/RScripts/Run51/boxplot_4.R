#!/usr/bin/Rscript
library(sqldf)


#
# first input configuration parameter to the file should be 
# the path to experiment directory
# second inputparameter : number of auction cycles per test
#

# input & output variable names
args <- commandArgs(trailingOnly = TRUE)
experiment_dir=args[1]
cycles=as.numeric(args[2])
numAgents = args[3]

# read config file
config_file_path <- sprintf('%sconfig', experiment_dir)
source(config_file_path, local = TRUE)

# read samples
samples_file_path <- sprintf('%ssamples.txt', experiment_dir)
samples = read.csv(samples_file_path, header = FALSE, sep='	')

analysis <- function(db) {
	s1_data <- readData(db,1)

	par(mar=c(6.5, 6.1, 4.1, 2), xpd=TRUE)
	boxplot(s1_data, ylab ="Percentage of agents increased/decreased their CE \n in 100 auction cycles",ylim=range(c(0,100)), las = 2)
	title('Initial high CE agents% = 25%')

	print(sprintf('Average percentage of high CE agents increased their CE: %s', mean(s1_data[,1])))
	print(sprintf('Average percentage of high CE agents decreased their CE: %s', mean(s1_data[,2])))
	print(sprintf('Average percentage of low CE agents increased their CE: %s', mean(s1_data[,3])))
	print(sprintf('Average percentage of low CE agents decreased their CE: %s', mean(s1_data[,4])))
}

readData <- function(db, sampleNumber){
	output <- matrix(nrow=REPLICATES,ncol=4)
	colnames(output) <- c("High CE:\nIncreased CE", "High CE:\nDecreased CE", "Low CE:\nIncreased CE", "Low CE:\nDecreased CE")
	threshold = 50

	for (r in 1:REPLICATES){
		# read agents initial Ce values
		df_1 = dbGetQuery(db, paste('select * from', "agents_ce", sprintf('where sample="%s" and cycle_number="0" and replicate="%s"', sampleNumber, r)))
		initial_values <- round(as.numeric(data.matrix(df_1)), digits = 4)

		initially_high = 0
		initially_low = 0
		# calculate how many agents initially had high CE.
		for(col in 4:length(initial_values)) {
			if(as.numeric(initial_values[col])<threshold){
				initially_low = initially_low + 1
			} else {
				initially_high = initially_high + 1
			}
		}

		df_2 = dbGetQuery(db, paste('select * from', "agents_ce", sprintf('where sample="%s" and cycle_number="100" and replicate="%s"', sampleNumber, r)))
		last_cycle <- round(as.numeric(data.matrix(df_2)), digits = 4)

		high_ce_increased = 0;
		high_ce_decreased = 0;
		low_ce_increased = 0;
		low_ce_decreased = 0;

		# iterate through all agents (agents are represented in each column from column number 4)
		for(col in 4:length(last_cycle)) {

			#if agent initially had high value
			if(initial_values[col] >= threshold){
				if(last_cycle[col] < initial_values[col]){
					high_ce_decreased = high_ce_decreased +1
				}
				if(last_cycle[col] > initial_values[col]){
					high_ce_increased = high_ce_increased +1
				}
			} 

			# if agent initially had low value
			if(initial_values[col] < threshold){
				if(last_cycle[col] > initial_values[col]){
					low_ce_increased = low_ce_increased +1
				}
				if(last_cycle[col] < initial_values[col]){
					low_ce_decreased = low_ce_decreased +1
				}
			}
		}

		output[r,] = c(high_ce_increased * 100/initially_high, high_ce_decreased* 100/initially_high, low_ce_increased * 100/initially_low, low_ce_decreased* 100/initially_low)
	}

	print(output)
	return(output)
}

plot_file_path <- sprintf('%sbox_4.pdf', experiment_dir)
pdf(plot_file_path)
db_path <- sprintf('%soutput.db', experiment_dir)
db <- dbConnect(SQLite(), dbname=db_path)
analysis(db)
dev.off()
