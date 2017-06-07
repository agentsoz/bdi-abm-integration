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

import io.github.agentsoz.conservation.Bid;
import io.github.agentsoz.conservation.Main;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Write information about all bids to an output csv file named
 * bids_<repeat>.csv
 * 
 * @author Sewwandi Perera
 */
public class BidsWriter {

	final private Logger logger = LoggerFactory.getLogger(Main.LOGGER_NAME);

	/**
	 * {@link FileWriter} instance
	 */
	private BufferedWriter writer;

	/**
	 * Current auction cycle number
	 */
	private static int cycleNumber;

	/**
	 * Singleton instance of the class
	 */
	private static BidsWriter instance = new BidsWriter();

	/**
	 * This is used to lock methods when multiple threads are trying to access
	 * them at the same time.
	 */
	private Lock lock = new ReentrantLock();

	/**
	 * @return singleton instance of the class
	 */
	public static BidsWriter getInstance() {
		return instance;
	}

	/**
	 * Private constructor of the singleton class
	 */
	private BidsWriter() {
	}

	/**
	 * Initialise the {@link BidsWriter}
	 * 
	 * @param repeat
	 *            current repeat number
	 */
	public void init(int repeat) {
		try {
			cycleNumber = 0;
			GZIPOutputStream zip = new GZIPOutputStream(new FileOutputStream(ConstantFileNames.getBidsFileName(repeat)));
			writer = new BufferedWriter(new OutputStreamWriter(zip, "UTF-8"));			
			writer.append("agentId,cycle_number,bidNumber,packageId,bidPrice\n");
			writer.flush();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}

	/**
	 * Write information about a bid to output file
	 * 
	 * @param agentName
	 *            name of the agent who made the bid
	 * @param bidNumber
	 *            this number is used to differentiate multiple bids made by the
	 *            same agent
	 * @param bid
	 *            information about the bid as an instance of {@link Bid}
	 */
	public void writeBid(String agentName, int bidNumber, Bid bid) {
		lock.lock();
		try {
			writer.append(agentName);
			writer.append(",");
			writer.append(Integer.toString(cycleNumber));
			writer.append(",");
			writer.append(Integer.toString(bidNumber));
			writer.append(",");
			writer.append(Integer.toString(bid.id));
			writer.append(",");
			writer.append(Double.toString(bid.price));
			writer.append("\n");
		} catch (IOException e) {
			logger.error(e.getMessage());
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Set current auction cycle number
	 * 
	 * @param number
	 *            current cycle number
	 */
	public void setCycleNumber(int number) {
		cycleNumber = number;
	}

	/**
	 * Close the writer
	 */
	public void close() {
		try {
			writer.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}

	/**
	 * Write information about a list of bids made by an agent
	 * 
	 * @param name
	 *            name of the agent
	 * @param selectedBids
	 *            bids made by the agent
	 */
	public void writeBids(String name, ArrayList<Bid> selectedBids) {
		int bidNumber = 1;

		for (Bid bid : selectedBids) {
			writeBid(name, bidNumber, bid);
			bidNumber++;
		}
	}
}
