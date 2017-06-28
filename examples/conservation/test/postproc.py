#!/usr/bin/python2.6
#
#
# usage: mine-success.py EXPERIMENT_DIR
#
# output sucess.db
#

import sys
import csv
import sqlite3
import gzip
from common import opengz_maybe, gz_maybe   # common.py routines

class MyArgs:
	EXPERIMENT_DIR = sys.argv[1]
args = MyArgs()

#import argparse
#parser = argparse.ArgumentParser()
#parser.add_argument("EXPERIMENT_DIR")
#args = parser.parse_args()

execfile(args.EXPERIMENT_DIR + '/config')


## output variable names
filenames = ["auction_statistics_001.csv.gz", "agents_ce_001.csv.gz", "agents_pm_001.csv.gz", "bids_001.csv.gz", "config_parameters.csv", "number_of_bids_per_agent001.csv.gz", "number_of_successful_bids_per_agent001.csv.gz", "successful_bid_price_per_agent001.csv.gz", "successful_opportunity_cost_per_agent001.csv.gz", "total_bid_price_per_agent001.csv.gz", "total_opportunity_cost_per_agent001.csv.gz"]
tablenames = ["auction_statistics", "agents_ce", "agents_pm", "bids", "config_parameters", "number_of_bids_per_agent", "number_of_successful_bids_per_agent", "successful_bid_price_per_agent", "successful_opportunity_cost_per_agent", "total_bid_price_per_agent", "total_opportunity_cost_per_agent"]
primarykeycolumns = [",cycle_number", ",cycle_number,agentId",",cycle_number,agentId", ",cycle_number,bidnumber,agentId", "", ",cycle_number,agentId",",cycle_number,agentId",",cycle_number,agentId",",cycle_number,agentId",",cycle_number,agentId",",cycle_number,agentId" ]
def extract(logpath):
	with open(logpath + 'output.txt') as f:
		y1 = [int(x) for x in f.readline().split()]
	return y1 

# main

def mktables(curs, logpath):
	for index, item in enumerate(filenames):
		name = logpath + item
		#print("\n" + "processing " + name + "\n")
		if (name.endswith('gz')):
			f = gzip.open(name)
		else:
			f = open(name)
		row = f.readline()
		cmd="CREATE TABLE " + tablenames[index] + " (sample,replicate," + row + ",PRIMARY KEY (sample,replicate" + primarykeycolumns[index] + "));"
		print(cmd)
		curs.execute(cmd)

def insert(curs, logpath, sample, replicate):
	for index, item in enumerate(filenames):
		name = logpath + item
		#print("\n" + "processing " + name + "\n")
		if (name.endswith('gz')):
			f = gzip.open(name)
		else:
			f = open(name)

		##skip the header row
		f.readline();
		reader = csv.reader(f)
		for row in reader:
			## Import row data into database
			cmd="INSERT INTO " + tablenames[index] + " VALUES ('"  + str(sample) + "','" + str(replicate) + "','" + "','".join(row) + "')"
			print(cmd)
			curs.execute(cmd)

conn = sqlite3.connect(args.EXPERIMENT_DIR + '/output.db')
curs = conn.cursor()

for sample in range(1, SAMPLES + 1):
	for replicate in range(1, REPLICATES + 1):
		logpath= args.EXPERIMENT_DIR + '/log/archive-%(sample)i-%(replicate)i/'%{'sample':sample, 'replicate':replicate}
		if (sample == 1) and (replicate == 1) :
			mktables(curs, logpath)
		insert(curs, logpath, sample, replicate)

conn.commit()
conn.close()

#print "DONE"
