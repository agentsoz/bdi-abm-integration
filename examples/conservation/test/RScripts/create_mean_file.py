#!/usr/bin/python2.7
#
# output sucess.db
# in this script it is assumed that only one sample and 100 replicates are in the test results.
#

import sys, csv, numpy, sqlite3

class MyArgs:
	EXPERIMENT_DIR = sys.argv[1]
args = MyArgs()

execfile(args.EXPERIMENT_DIR + '/config')

## output variable names
replicates=100
samples = 1
input_table = "auction_statistics"
variables = ["LCLP_agents", "HCLP_agents", "LCHP_agents", "HCHP_agents", "LCLP_participants", "HCLP_participants", "LCHP_participants", "HCHP_participants", "LCLP_winners", "HCLP_winners", "LCHP_winners", "HCHP_winners", "normalized_cost", "average_CE", "average_PM"]

def extract(logpath):
	with open(logpath + 'output.txt') as f:
		y1 = [int(x) for x in f.readline().split()]
	return y1 

def mktables(curs, logpath):
	for index, item in enumerate(filenames):
        	f = open(logpath + item)
		row = f.readline()
		cmd="CREATE TABLE " + tablenames[index] + " (sample,replicate," + row + ",PRIMARY KEY (sample,replicate" + primarykeycolumns[index] + "));"
		curs.execute(cmd)

def insert(curs, logpath, sample, replicate):
	for index, item in enumerate(filenames):
        	f = open(logpath + item)
		##skip the header row
		f.readline();
		reader = csv.reader(f)
		for row in reader:
			## Import row data into database
			cmd="INSERT INTO " + tablenames[index] + " VALUES ('"  + str(sample) + "','" + str(replicate) + "','" + "','".join(row) + "')"
			curs.execute(cmd)	

def readCycles(curs):
	curs.execute('SELECT cast(cycle_number as integer) FROM %s WHERE sample="1" AND replicate="1" ORDER BY cast(cycle_number as integer)'%(input_table))
    	data = curs.fetchall()
	return data

def readMean(curs, variable, num_cycles):
	temp_mat = numpy.zeros((num_cycles, SAMPLES*REPLICATES))
	result_mat = numpy.zeros((num_cycles, 1))

	for sample in range(0, SAMPLES):
		for replicate in range(0, REPLICATES):
			col_num_to_store = sample * REPLICATES + replicate
			curs.execute('SELECT cast(%s as double) FROM %s WHERE sample="%s" AND replicate="%s" ORDER BY cast(cycle_number as integer)'%(variable,input_table, sample + 1, replicate + 1))
		    	data = curs.fetchall()
			for index, item in enumerate(data):
				temp_mat[index,col_num_to_store] = item[0]
	index = 0	
	for row in temp_mat:
		result_mat[index,0] = numpy.average(row)
		index += 1

	return result_mat[:,0]

### Main

# Connect database
conn = sqlite3.connect(args.EXPERIMENT_DIR + '/output.db')
curs = conn.cursor()

# Create a matrix to hold all results
cycles = readCycles(curs)
num_cycles = len(cycles)
mean_mat = numpy.zeros((num_cycles,len(variables) + 1))

rowCount = 0
for cycle in cycles:
	mean_mat[rowCount,0] = cycle[0]
	rowCount += 1

index = 1
for var in variables:
	mean_mat[:,index] = readMean(curs, var, num_cycles)
	index += 1

numpy.savetxt(args.EXPERIMENT_DIR + "/mean_over_time.csv", mean_mat, delimiter=",", header="cycle_number," + str(variables).strip('[]'))

conn.commit()
conn.close()

print "DONE"
