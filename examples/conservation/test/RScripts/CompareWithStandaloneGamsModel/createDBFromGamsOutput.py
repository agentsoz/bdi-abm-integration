#!/usr/bin/python2.7

import openpyxl as px
import numpy as np
import sqlite3
import sys

class MyArgs:
	EXPERIMENT_DIR = sys.argv[1]
args = MyArgs()

# function definitions


# main
W = px.load_workbook('payment_100agent_8package.xlsx', use_iterators = True)
p = W.get_sheet_by_name(name = 'DEA_ALL_BN10_CM20')

conn = sqlite3.connect(args.EXPERIMENT_DIR + '/gams_output.db')
curs = conn.cursor()
cmd="CREATE TABLE gams_output (replication,round,payment,PRIMARY KEY (replication,round));"
curs.execute(cmd)

headerRowFound = "false"
for row in p.iter_rows(row_offset=1):
	if headerRowFound == "true":
		## Import row data into database
		cmd="INSERT INTO gams_output VALUES ('"  + str(row[0].internal_value) + "','" + str(row[1].internal_value) + "','" + str(row[2].internal_value) + "');"
		curs.execute(cmd)
	else :
		headerRowFound = "true"

conn.commit()
conn.close()
