package io.github.agentsoz.conservation;

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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * This class is used as the data structure to hold auction results, which were
 * read from conservation.csv.out file.
 * 
 * @author Sewwandi Perera
 */
public class AuctionResultSet {

    final private Logger logger = LoggerFactory.getLogger(Main.LOGGER_NAME);

	/**
	 * This identifier is used when reading out.CSV file to skip the lines which
	 * do not contain any auction results.
	 */
	public static final String OUT_CSV_SKIP_LINE_IDENTIFIER = "bidnum";

	/**
	 * Results are stored in a {@link HashMap} where the key is the relevant
	 * package ID. Results for each package are stored in a
	 * {@link AuctionResult}
	 */
	private HashMap<Integer, AuctionResult> resultSet;

	/**
	 * This is the only constructor for {@link AuctionResultSet}.
	 * 
	 * @param filepath
	 *            path to the CSV file, in which the auction results are stored.
	 */
	public AuctionResultSet(String filepath) {
		resultSet = new HashMap<Integer, AuctionResult>();
		parseCsvFile(filepath);
	}

	/**
	 * This private method can be used to get the results of the package when
	 * the package ID is given.
	 * 
	 * @param packageID
	 * @return auction results of the package as a {@link AuctionResult} object.
	 */
	private AuctionResult getResultsOfPackage(int packageID) {
		AuctionResult result = resultSet.get(packageID);
		if (result == null) {
			result = new AuctionResult(packageID);
			resultSet.put(packageID, result);
		}

		return result;
	}

	/**
	 * This private method is used in the Constructor to parse the input CSV
	 * file, which contains all auction results.
	 * 
	 * @param filepath
	 *            is the path to CSV file.
	 */
	private void parseCsvFile(String filepath) {
		if (filepath == null) {
			return;
		}

		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";

		try {
			br = new BufferedReader(new FileReader(filepath));
			while ((line = br.readLine()) != null) {
				// There exists few lines in CSV file which do not contain any
				// results.
				// Those lines were skipped by checking whether the line
				// contains "bidnum" string.
				if (line.contains(OUT_CSV_SKIP_LINE_IDENTIFIER)) {
					continue;
				}

				String[] parts = line.split(cvsSplitBy);
				int packageID = (int) Double.parseDouble(parts[6]);
				int agentName = (int) Double.parseDouble(parts[5]);
				String agentNameString = Integer.toString(agentName);
				double price = Double.parseDouble(parts[4]);
				double winner = Double.parseDouble(parts[7]);

				if (price != 0) {
					AuctionResult result = getResultsOfPackage(packageID);

					result.addBid(agentNameString, price);
					if (winner == 1) {
						result.addWinner(agentNameString);
					}
				}
			}

		} catch (FileNotFoundException e) {
			logger.error("ERROR:" + e.getMessage());
		} catch (IOException e) {
			logger.error("ERROR:" + e.getMessage());
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					logger.error("ERROR:" + e.getMessage());
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

	/**
	 * This method returns auction results.
	 * 
	 * @return results in a {@link HashMap}.
	 */
	public HashMap<Integer, AuctionResult> getResultSet() {
		return resultSet;
	}

	/**
	 * This data structure is used to store auction results of a package.
	 */
	public class AuctionResult {
		/**
		 * ID of the package.
		 */
		private int packageID;

		/**
		 * All bids made by land holders to the package. Name of the agent who
		 * made the bid is used as the key and the price of the bid is used as
		 * the value. key:Agent's Name, value:bid price
		 */
		private HashMap<String, Double> bids;

		/**
		 * Names of the agent who made the successful bid to the particular
		 * package.
		 */
		private ArrayList<String> winners;

		/**
		 * Constructor of {@link AuctionResult} class.
		 * 
		 * @param id
		 *            is the ID of the package.
		 */
		public AuctionResult(int id) {
			this.bids = new HashMap<String, Double>();
			this.winners = new ArrayList<String>();
			this.packageID = id;
		}

		public boolean isWinner(String agentName) {
			return winners.contains(agentName);
		}

		/**
		 * Gets the price of the bid made by an agent when the name of the agent
		 * is given.
		 * 
		 * @param agentName
		 * @return
		 */
		public Object getAgentsBid(String agentName) {
			return bids.get(agentName);
		}

		/**
		 * Add a bid, which is made to the particular package.
		 * 
		 * @param agentName
		 *            is the name of the agent.
		 * @param price
		 *            is price of the bid.
		 */
		public void addBid(String agentName, double price) {
			this.bids.put(agentName, price);
		}

		/**
		 * Set winner of the package.
		 * 
		 * @param agentName
		 *            is the name of the winner.
		 */
		public void addWinner(String agentName) {
			this.winners.add(agentName);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("\n==Package:" + packageID + "==");
			builder.append("\nWinners:" + this.winners);

			for (int i = 1; i <= bids.size(); i++) {
				builder.append("\n   Agent:" + i + " price:" + bids.get(i));
			}

			return builder.toString();
		}

		/**
		 * Get the winner of the package.
		 * 
		 * @return
		 */
		public ArrayList<String> getWinners() {
			return winners;
		}

		/**
		 * Get all bids made to the package.
		 * 
		 * @return
		 */
		public HashMap<String, Double> getBids() {
			return bids;
		}

		/**
		 * Get the ID of the package.
		 * 
		 * @return
		 */
		public int getPackageID() {
			return packageID;
		}
	}
}
