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

import io.github.agentsoz.conservation.AuctionResultSet.AuctionResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * This data structure is used to store auction results obtained in previous
 * SUCCESSFUL auction rounds. Only information about up to three auction rounds
 * are stored.
 * 
 * @author Sewwandi Perera
 */
public class LandholderHistory {

    final private Logger logger = LoggerFactory.getLogger(Main.LOGGER_NAME);

	/**
	 * Maximum number of records that should be stored in the history. When
	 * storing the information about previous auction round, only information
	 * about successful auction rounds are stored.
	 */
	private final int MAX_RECORDS = 3;

	/**
	 * Results obtained in each round are stored in here.
	 */
	private ArrayList<AuctionRound> resultsHistory;

	/**
	 * Packages, which were sent with CALL_FOR_BIDS percept are stored in here.
	 * "packages" and "resultsHistory" are stored in the same order to use in
	 * calculations.
	 */
	private ArrayList<Package[]> packages;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

	/**
	 * Public constructor of the class.
	 */
	public LandholderHistory() {
		resultsHistory = new ArrayList<AuctionRound>();
		packages = new ArrayList<Package[]>();
	}

	/**
	 * Register new auction round in the history.
	 * 
	 * @param results
	 * @param agentName
	 * @return the new auction round as a {@link AuctionRound}.
	 */
	public AuctionRound registerAuctionRound(AuctionResultSet results,
			String agentName) {
		AuctionRound newRound = new AuctionRound(results.getResultSet(),
				agentName, packages.get(resultsHistory.size()));

		if (newRound.winnersInfo.size() != 0) {

			//slogger.debug("Agent " + agentName + " saved auction round: " + newRound);
			// The new auction round is stored in to the history only if it is a
			// successful auction round.
			resultsHistory.add(newRound);
			if (resultsHistory.size() > MAX_RECORDS) {
				resultsHistory.remove(0);
				packages.remove(0);
			}
		} else {
			logger.debug("Agent " + agentName + "did not save auction round as there were no winners");
		}
		return newRound;
	}

	/**
	 * Register the packages, which were sent with CALL_FOR_BIDS percept.
	 * 
	 * @param packages
	 * @return
	 */
	public boolean registerPackages(Package[] packages) {
		this.packages.add(packages);
		return true;
	}

	/**
	 * Get the results of the auction round when the index is given.
	 * 
	 * @param index
	 * @return
	 */
	public AuctionRound getAuctionRound(int index) {
		return resultsHistory.get(index);
	}

	/**
	 * Get the number of auction rounds stored in the history.
	 * 
	 * @return
	 */
	public int getNumberOfAuctionRounds() {
		return resultsHistory.size();
	}

	/**
	 * This class is used to store the results obtained by an agent in an
	 * auction round.
	 */
	public class AuctionRound {
		/**
		 * Whether the agent was participated in the auction round or not.
		 */
		private boolean participated;

		/**
		 * Whether the agent has won at least one package in the auction round.
		 */
		private boolean won;

		/**
		 * Number of bids made by all agents in the auction round.
		 */
		private int numberOfBidsByPopulation;

		/**
		 * All bids made by the agent in the auction round.
		 */
		private ArrayList<BidResult> myBids;

		/**
		 * Information about all winning bids are stored here.
		 */
		private ArrayList<BidResult> winnersInfo;

		/**
		 * private constructor of {@link AuctionRound}.
		 * 
		 * @param resultSet
		 *            contains all results obtained by all agents in the auction
		 *            round.
		 * @param agentName
		 *            ID of the agent.
		 * @param packages
		 *            all packages which were sent to select bids in the auction
		 *            round.
		 */
		private AuctionRound(HashMap<Integer, AuctionResult> resultSet,
				String agentName, Package[] packages) {
			myBids = new ArrayList<BidResult>();
			winnersInfo = new ArrayList<BidResult>();
			numberOfBidsByPopulation = 0;

			// Updating bids;
			Object bidPrice;
			ArrayList<String> winners;
			int packageID;
			double oppotunityCost;

			for (Integer key : resultSet.keySet()) {
				AuctionResult result = resultSet.get(key);
				numberOfBidsByPopulation += result.getBids().size();
				bidPrice = result.getAgentsBid(agentName);
				winners = result.getWinners();
				packageID = result.getPackageID();
				HashMap<String, Double> winningPrices = new HashMap<String, Double>();
				oppotunityCost = 0;

				for (Package p : packages) {
					if (p.id == packageID) {
						oppotunityCost = p.opportunityCost;
						break;
					}
				}

				for (String winner : winners) {
					double winnersBid = (double) result.getAgentsBid(winner);
					winningPrices.put(winner, winnersBid);
					winnersInfo.add(new BidResult(packageID, oppotunityCost,
							winnersBid, null, true));
				}

				if (winners.contains(agentName)) {
					won = true;
				}

				if (null != bidPrice && (double) bidPrice != 0) {
					addBidResult(packageID, oppotunityCost, (double) bidPrice,
							winningPrices, winners.contains(agentName));
				}
			}

			participated = myBids.size() > 0;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			return new Gson().toJson(this);
		}

		/**
		 * Returns the number of bids made by the agent during the auction round
		 * 
		 * @return
		 */
		public int getNumberOfBids() {
			return myBids.size();
		}

		/**
		 * Returnthe number of successful bids made by the agent during the
		 * auction cycle
		 * 
		 * @return
		 */
		public int getNumberOfSuccessfulBids() {
			int count = 0;

			for (BidResult bid : myBids) {
				if (bid.won) {
					count++;
				}
			}

			return count;
		}

		/**
		 * Returns the total opportunity cost of all bids made by the agents
		 * during the auction cycle.
		 * 
		 * @return
		 */
		public double getTotalOpportunityCostOfAllMyBids() {
			double cost = 0;

			for (BidResult bid : myBids) {
				cost += bid.opportunityCostOfPackage;
			}

			return cost;
		}

		/**
		 * Returns the sum of prices of bids made by the agent during the
		 * auction cycle.
		 * 
		 * @return
		 */
		public double getTotalPriceOfAllMyBids() {
			double price = 0;

			for (BidResult bid : myBids) {
				price += bid.bidPrice;
			}

			return price;
		}

		/**
		 * Returns the total opportunity cost of agent's all successful bids
		 * during the auction round.
		 * 
		 * @return
		 */
		public double getTotalOpportunityCostOfMySuccessfulBids() {
			double cost = 0;

			for (BidResult bid : myBids) {
				if (bid.won) {
					cost += bid.opportunityCostOfPackage;
				}
			}

			return cost;
		}

		/**
		 * Returns the sum of prices of all successful bids made by the agent
		 * during the auction cycle.
		 * 
		 * @return
		 */
		public double getTotalPriceOfMySuccessfulBids() {
			double price = 0;

			for (BidResult bid : myBids) {
				if (bid.won) {
					price += bid.bidPrice;
				}
			}

			return price;
		}

		/**
		 * Add a new bid result to the auction round
		 * 
		 * @param packageID
		 * @param oppotunityCost
		 * @param price
		 * @param winningPrice
		 * @param won
		 */
		private void addBidResult(int packageID, double oppotunityCost,
				double price, HashMap<String, Double> winners, boolean won) {
			myBids.add(new BidResult(packageID, oppotunityCost, price, winners,
					won));
		}

		/**
		 * Get number of winnings made by the agent in the auction round.
		 * 
		 * @return
		 */
		public int getNumberOfWinningsByPopulation() {
			return winnersInfo.size();
		}

		/**
		 * Get number of bids made by all agents in the auction round.
		 * 
		 * @return
		 */
		public int getNumberOfBidsByPopulation() {
			return numberOfBidsByPopulation;
		}

		/**
		 * Checks whether the agent has participated in the auction round or
		 * not.
		 * 
		 * @return
		 */
		public boolean isParticipated() {
			return participated;
		}

		/**
		 * Gets all bid results obtained by the agent.
		 * 
		 * @return
		 */
		public ArrayList<BidResult> getMyBids() {
			return myBids;
		}

		/**
		 * Gets information about the successful winnings made to not
		 * participated packages.
		 * 
		 * @return
		 */
		public ArrayList<BidResult> getWinnersInfo() {
			return winnersInfo;
		}

		/**
		 * Returns true if the agent has at least one successful bid in the
		 * auction round.
		 * 
		 * @return
		 */
		public boolean isWon() {
			return won;
		}

		/**
		 * TRUE if agent has won at least one bid in the auction round.
		 * 
		 * @param won
		 */
		public void setWon(boolean won) {
			this.won = won;
		}
	}

	/**
	 * This data structure stores all information related to any bid.
	 */
	public class BidResult {
		/**
		 * ID of the package.
		 */
		private int packageID;

		/**
		 * Opportunity cost of the package.
		 */
		private double opportunityCostOfPackage;

		/**
		 * Price of the bid.
		 */
		private double bidPrice;

		/**
		 * Price of the successful bid made to the same package.
		 */
		private HashMap<String, Double> winners;

		/**
		 * Whether the agent has won his bid.
		 */
		private boolean won;

		/**
		 * Private constructor of {@link BidResult}
		 * 
		 * @param packageID
		 * @param oppotunityCost
		 * @param price
		 * @param winningPrice
		 * @param won
		 */
		private BidResult(int packageID, double oppotunityCost, double price,
				HashMap<String, Double> winners, boolean won) {
			this.setPackageID(packageID);
			this.opportunityCostOfPackage = oppotunityCost;
			this.bidPrice = price;
			this.winners = winners;
			this.won = won;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			return new Gson().toJson(this);
		}

		/**
		 * This bid result belongs to a successful bid.
		 * 
		 * @return
		 */
		public boolean isWon() {
			return won;
		}

		/**
		 * Return a hashmap of <winner ID, winning bid price> about all
		 * successful bids made to the package.
		 * 
		 * @return
		 */
		public HashMap<String, Double> getWinningPrices() {
			return winners;
		}

		/**
		 * Return the price of the successful bid which has the highest price
		 * out of all successful bids of the package.
		 * 
		 * @return
		 */
		public Double getHighestWinningPrice() {
			if (null != winners && !winners.isEmpty()) {
				return Collections.max(winners.values());
			}
			return Double.valueOf(0);
		}

		/**
		 * Returns the opportunity cost of the package to which the bid was made
		 * 
		 * @return
		 */
		public double getOpportunityCost() {
			return opportunityCostOfPackage;
		}

		/**
		 * Returns the price of the bid
		 * 
		 * @return
		 */
		public double getBidPrice() {
			return bidPrice;
		}

		/**
		 * Return the package ID, to which the bid was made
		 * 
		 * @return
		 */
		public int getPackageID() {
			return packageID;
		}

		/**
		 * Set the package ID, to which the bid was made.
		 * 
		 * @param packageID
		 */
		public void setPackageID(int packageID) {
			this.packageID = packageID;
		}
	}
}
