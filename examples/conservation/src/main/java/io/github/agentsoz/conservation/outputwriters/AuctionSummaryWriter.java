package io.github.agentsoz.conservation.outputwriters;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2015 by its authors. See AUTHORS file.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import io.github.agentsoz.conservation.Log;
import io.github.agentsoz.conservation.AuctionResultSet.AuctionResult;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.zip.GZIPOutputStream;

/**
 * Write a summary about;
 * 
 * how many bids were made to each package in each auction cycle.
 * 
 * how many successful bids were in each package in each auction cycle.
 * 
 * all outputs are written to a file named auction.summary.log.
 * 
 * @author Sewwandi Perera
 */
public class AuctionSummaryWriter {
	/**
	 * {@link FileWriter} instance
	 */
	private BufferedWriter auctionSummary;

	/**
	 * Output file name
	 */
	private final String fileName = "auction.summary.log.gz";

	/**
	 * Singleton instance of the class
	 */
	private static AuctionSummaryWriter instance = new AuctionSummaryWriter();

	/**
	 * @return singleton instance of the class
	 */
	public static AuctionSummaryWriter getInstance() {
		return instance;
	}

	/**
	 * Private constructor of the singleton class
	 */
	private AuctionSummaryWriter() {
		try {
			GZIPOutputStream zip = new GZIPOutputStream(
		            new FileOutputStream(fileName));
			auctionSummary = new BufferedWriter(
					new OutputStreamWriter(zip, "UTF-8"));
		} catch (IOException e) {
			Log.error(e.getMessage());
		}
	}

	/**
	 * Write summary of an auction cycle
	 * 
	 * @param repeat
	 *            repeat number
	 * @param cycle
	 *            cycle number
	 * @param resultSet
	 *            results of the auction cycle
	 * @throws IOException
	 */
	public void writeSummary(int repeat, int cycle,
			HashMap<Integer, AuctionResult> resultSet) throws IOException {
		auctionSummary.append("Summary of cycle " + cycle + " of repeat "
				+ repeat + ":\n");

		Object[] packageIds = resultSet.keySet().toArray();

		for (int i = 0; i < packageIds.length; i++) {
			int packageId = (int) packageIds[i];
			auctionSummary.append("\tPakcage Number:");
			auctionSummary.append(Integer.toString(packageId));
			auctionSummary.append(", number of bids:");
			auctionSummary.append(Integer.toString(resultSet.get(packageId)
					.getBids().size()));
			auctionSummary.append(", number of winners:");
			auctionSummary.append(Integer.toString(resultSet.get(packageId)
					.getWinners().size()));
			auctionSummary.append("\n");
		}

		auctionSummary.flush();
	}

	/**
	 * Close the writer
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		auctionSummary.close();
	}
}
