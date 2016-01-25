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

# read config file
config_file_path <- sprintf('%sconfig', experiment_dir)
source(config_file_path, local = TRUE)

# read samples
samples_file_path <- sprintf('%ssamples.txt', experiment_dir)
samples = read.csv(samples_file_path, header = FALSE, sep='	')
sampleCount <- nrow(samples)
output_params <- c("cycle number", "Number of winners with high profit", "Number of winners with medium profitC", "Number of winners with low profit")

# process database
plot_pair <- function(all) {
	par(mar=c(4.1, 4.1, 4.1, 4.1), xpd=TRUE)
        plot(all[,c(output_params[1], output_params[2])], ylim=range(c(0,25)), col="red", type = "l", ylab="Number of Agents Changed Category", lwd=2)
        lines(all[,c(output_params[1], output_params[3])], col="green", lwd=2)
        lines(all[,c(output_params[1], output_params[4])], col="blue", lwd=2)
	title("Number of winners in the category")
	legend(30, 25, c(output_params[2], output_params[3], output_params[4]), lty=c(1,1,1) ,col=c("red","green", "blue"), lwd=c(2,2,2)) 
}


analysis <- function(db) {
	output <- matrix(nrow=cycles,ncol=4)
	colnames(output) <- output_params 

	plot_file_path <- sprintf('%swinners_according_to_profit.pdf', experiment_dir)
	pdf(plot_file_path)

	for (s in 1:sampleCount ) {	
		for (c in 1:cycles){	
			df = dbGetQuery(db, paste('select CAST(high_profit_winning_bids as REAL), CAST(med_profit_winning_bids as REAL), CAST(low_profit_winning_bids as REAL) from', "auction_statistics", sprintf('where sample="%s" and cycle_number="%s"', s, c)))
			result <- data.matrix(df)
			output[c,] = c(c,mean(result[,1]), mean(result[,2]), mean(result[,3]))
		}
		
		plot_pair(output)
	}

	dev.off()
}

db_path <- sprintf('%soutput.db', experiment_dir)
db <- dbConnect(SQLite(), dbname=db_path)
analysis(db)
