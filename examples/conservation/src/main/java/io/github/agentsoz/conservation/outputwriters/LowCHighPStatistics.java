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

import java.io.FileWriter;
import java.io.IOException;

/**
 * This output writer class is specially written to write information about the
 * behaviour of LowCHighP category
 * 
 * @author Sewwandi Perera
 */
public class LowCHighPStatistics {
	/**
	 * {@link FileWriter} instance
	 */
	private FileWriter writer;

	/**
	 * Current auction cycle number
	 */
	private int cycleNumber;

	/**
	 * Singleton instance of the class
	 */
	private static LowCHighPStatistics instance = new LowCHighPStatistics();

	/**
	 * @return singleton instance of the class
	 */
	public static LowCHighPStatistics getInstance() {
		return instance;
	}

	/**
	 * private constructor of the singleton class
	 */
	private LowCHighPStatistics() {

	}

	/**
	 * Open the output writer.
	 * 
	 * @param repeat
	 *            current repeat number
	 */
	public void open(int repeat) {
		try {
			writer = new FileWriter(
					ConstantFileNames.getLowCHighPStatFileName(repeat));
			writer.append("agentId,cycle#,probability,totalNumberOfWinnings,totalNumberOfWinningsWithGoodProfit,goodProfit,participated\n");
			writer.flush();

		} catch (IOException e) {
			Log.error(e.getMessage());
		}
	}

	/**
	 * Set current auction cycle number
	 * 
	 * @param cycle
	 */
	public void setCycleNumber(int cycle) {
		cycleNumber = cycle;
	}

	/**
	 * Print an entry to output file
	 * 
	 * @param agentName
	 *            name of the agent
	 * @param probability
	 *            probability of participating auction
	 * @param totalNumberOfBids
	 *            total number of bids made by the agent
	 * @param totalNumberOfWinnings
	 *            total number of successful bids by the agent
	 * @param goodProfit
	 *            total number of successful bids with good profit
	 * @param participated
	 *            true if participated in the auction cycle
	 */
	public void printEntry(String agentName, double probability,
			int totalNumberOfBids, int totalNumberOfWinnings,
			double goodProfit, String participated) {
		try {
			writer.append(agentName);
			writer.append(",");
			writer.append(Integer.toString(cycleNumber));
			writer.append(",");
			writer.append(Double.toString(probability));
			writer.append(",");
			writer.append(Integer.toString(totalNumberOfBids));
			writer.append(",");
			writer.append(Integer.toString(totalNumberOfWinnings));
			writer.append(",");
			writer.append(Double.toString(goodProfit));
			writer.append(",");
			writer.append(participated);
			writer.append("\n");
		} catch (IOException e) {
			Log.error(e.getMessage());
		}
	}

	/**
	 * close the writer
	 */
	public void close() {
		try {
			writer.close();
		} catch (IOException e) {
			Log.error(e.getMessage());
		}
	}
}
