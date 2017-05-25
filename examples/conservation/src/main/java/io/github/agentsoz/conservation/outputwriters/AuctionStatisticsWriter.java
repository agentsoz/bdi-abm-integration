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

import io.github.agentsoz.conservation.ConservationUtils;
import io.github.agentsoz.conservation.Log;
import io.github.agentsoz.conservation.Package;
import io.github.agentsoz.conservation.AuctionResultSet.AuctionResult;
import io.github.agentsoz.conservation.jill.agents.Landholder;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPOutputStream;

/**
 * Writes all statistics about each auction cycle to a output file named
 * "auction_statistics_<repeat>.csv"
 * 
 * The statistics that were written to the file are: cost_of_all_bids,
 * cost_of_successful_bids, normalized_cost, number_of_bids,
 * number_of_successful_bids, number_of_participans, average_CE, average_PM,
 * LCLP_agents, HCLP_agents, LCHP_agents, HCHP_agents, target_malleefowls,
 * target_phascogales, target_pythons, achievable_malleefowls,
 * achievable_phascogales, achievable_pythons, highCE_agents_percentage,
 * LCLP_participants, HCLP_participants, LCHP_participants, HCHP_participants,
 * LCLP_participants_percentage, HCLP_participants_percentage,
 * LCHP_participants_percentage, HCHP_participants_percentage,
 * LCLP_participants_accumulated_percentage,
 * HCLP_participants_accumulated_percentage, auctionStatisticsWriter,
 * LCHP_participants_accumulated_percentage, auctionStatisticsWriter,
 * HCHP_participants_accumulated_percentage, LCLP_winners, HCLP_winners,
 * LCHP_winners, HCHP_winners, LCLP_winners_percentage, HCLP_winners_percentage,
 * LCHP_winners_percentage, HCHP_winners_percentage, auctionStatisticsWriter,
 * LCLP_winners_accumulated_percentage, auctionStatisticsWriter,
 * HCLP_winners_accumulated_percentage, auctionStatisticsWriter,
 * LCHP_winners_accumulated_percentage, auctionStatisticsWriter,
 * HCHP_winners_accumulated_percentage, MovedUpCE, MovedDownCE, MovedUpPM,
 * MovedDownPM, high_profit_winning_bids, med_profit_winning_bids,
 * low_profit_winning_bids
 * 
 * @author Sewwandi Perera
 */
public class AuctionStatisticsWriter {
	/**
	 * {@link FileWriter} instance
	 */
	private BufferedWriter auctionStatisticsWriter;

	/**
	 * This is used allow some methods to be used by only by a single thread at
	 * a time
	 */
	private Lock lock;

	/**
	 * Singleton instance
	 */
	private static AuctionStatisticsWriter instance = new AuctionStatisticsWriter();

	/**
	 * Average CE and PM of agents
	 */
	private double averageCE, averagePM;

	/**
	 * Number of agents in each category
	 */
	private int LCLPAgents, HCLPAgents, LCHPAgents, HCHPAgents;;

	/**
	 * Percentage of agents with high CE
	 */
	private double highCEAgentsPercentage;

	/**
	 * Number of participants in each category
	 */
	public int participantsLCLP, participantsLCHP, participantsHCLP,
			participantsHCHP;

	/**
	 * Number of winners in each category
	 */
	public int winnersLCLP, winnersLCHP, winnersHCLP, winnersHCHP;

	/**
	 * Total number of participants in all auction cycles up to current auction
	 * cycle
	 */
	public double accumulatedParticipantsPercentageLCLP,
			accumulatedParticipantsPercentageLCHP,
			accumulatedParticipantsPercentageHCLP,
			accumulatedParticipantsPercentageHCHP;

	/**
	 * Total number of winners in all auction cycles up to current auction cycle
	 */
	public double accumulatedWinnersPercentageLCLP,
			accumulatedWinnersPercentageLCHP, accumulatedWinnersPercentageHCLP,
			accumulatedWinnersPercentageHCHP;

	/**
	 * @return singleton instance of the class
	 */
	public static AuctionStatisticsWriter getInstance() {
		return instance;
	}

	/**
	 * private constructor of singleton design pattern
	 */
	private AuctionStatisticsWriter() {
		lock = new ReentrantLock();
		lock.lock();
		participantsLCLP = 0;
		participantsLCHP = 0;
		participantsHCLP = 0;
		participantsHCHP = 0;
		winnersLCLP = 0;
		winnersLCHP = 0;
		winnersHCLP = 0;
		winnersHCHP = 0;
		accumulatedParticipantsPercentageLCLP = 0;
		accumulatedParticipantsPercentageLCHP = 0;
		accumulatedParticipantsPercentageHCLP = 0;
		accumulatedParticipantsPercentageHCHP = 0;
		accumulatedWinnersPercentageLCLP = 0;
		accumulatedWinnersPercentageLCHP = 0;
		accumulatedWinnersPercentageHCLP = 0;
		accumulatedWinnersPercentageHCHP = 0;
		lock.unlock();
	}

	/**
	 * Open the file writer.
	 * 
	 * @param repeat
	 *            current repeat
	 * @param agents
	 *            agents in the simulation
	 */
	public void open(int repeat, List<Landholder> agents) {
		try {
			GZIPOutputStream zip = new GZIPOutputStream(new FileOutputStream(ConstantFileNames.getAuctionStatsFileName(repeat)));
			auctionStatisticsWriter = new BufferedWriter(new OutputStreamWriter(zip, "UTF-8"));			
			appendHeader();
			addAuctionStatistics(0, null, agents, null, null, true);
		} catch (IOException e) {
			Log.error(e.getMessage());
		}
	}

	/**
	 * Append header row of the output csv to output file writer
	 */
	private void appendHeader() {
		try {
			auctionStatisticsWriter.append("cycle_number,");
			auctionStatisticsWriter.append("cost_of_all_bids,");
			auctionStatisticsWriter.append("cost_of_successful_bids,");
			auctionStatisticsWriter.append("normalized_cost,");
			auctionStatisticsWriter.append("number_of_bids,");
			auctionStatisticsWriter.append("number_of_successful_bids,");
			auctionStatisticsWriter.append("number_of_participans,");
			auctionStatisticsWriter.append("average_CE,");
			auctionStatisticsWriter.append("average_PM,");
			auctionStatisticsWriter.append("LCLP_agents,");
			auctionStatisticsWriter.append("HCLP_agents,");
			auctionStatisticsWriter.append("LCHP_agents,");
			auctionStatisticsWriter.append("HCHP_agents,");
			auctionStatisticsWriter.append("target_malleefowls,");
			auctionStatisticsWriter.append("target_phascogales,");
			auctionStatisticsWriter.append("target_pythons,");
			auctionStatisticsWriter.append("achievable_malleefowls,");
			auctionStatisticsWriter.append("achievable_phascogales,");
			auctionStatisticsWriter.append("achievable_pythons,");
			auctionStatisticsWriter.append("highCE_agents_percentage,");
			auctionStatisticsWriter.append("LCLP_participants,");
			auctionStatisticsWriter.append("HCLP_participants,");
			auctionStatisticsWriter.append("LCHP_participants,");
			auctionStatisticsWriter.append("HCHP_participants,");
			auctionStatisticsWriter.append("LCLP_participants_percentage,");
			auctionStatisticsWriter.append("HCLP_participants_percentage,");
			auctionStatisticsWriter.append("LCHP_participants_percentage,");
			auctionStatisticsWriter.append("HCHP_participants_percentage,");
			auctionStatisticsWriter
					.append("LCLP_participants_accumulated_percentage,");
			auctionStatisticsWriter
					.append("HCLP_participants_accumulated_percentage,");
			auctionStatisticsWriter
					.append("LCHP_participants_accumulated_percentage,");
			auctionStatisticsWriter
					.append("HCHP_participants_accumulated_percentage,");
			auctionStatisticsWriter.append("LCLP_winners,");
			auctionStatisticsWriter.append("HCLP_winners,");
			auctionStatisticsWriter.append("LCHP_winners,");
			auctionStatisticsWriter.append("HCHP_winners,");
			auctionStatisticsWriter.append("LCLP_winners_percentage,");
			auctionStatisticsWriter.append("HCLP_winners_percentage,");
			auctionStatisticsWriter.append("LCHP_winners_percentage,");
			auctionStatisticsWriter.append("HCHP_winners_percentage,");
			auctionStatisticsWriter
					.append("LCLP_winners_accumulated_percentage,");
			auctionStatisticsWriter
					.append("HCLP_winners_accumulated_percentage,");
			auctionStatisticsWriter
					.append("LCHP_winners_accumulated_percentage,");
			auctionStatisticsWriter
					.append("HCHP_winners_accumulated_percentage,");
			auctionStatisticsWriter.append("MovedUpCE,");
			auctionStatisticsWriter.append("MovedDownCE,");
			auctionStatisticsWriter.append("MovedUpPM,");
			auctionStatisticsWriter.append("MovedDownPM,");
			auctionStatisticsWriter.append("high_profit_winning_bids,");
			auctionStatisticsWriter.append("med_profit_winning_bids,");
			auctionStatisticsWriter.append("low_profit_winning_bids");
			auctionStatisticsWriter.append("\n");
		} catch (IOException e) {
			Log.error(e.getMessage());
		}
	}

	/**
	 * Write statistics of current auction cycle to the output file.
	 * 
	 * @param cycleNumber
	 *            current auction cycle number
	 * @param resultSet
	 *            results of current auction cycle
	 * @param agents
	 *            agents of the simulation
	 * @param target
	 *            target of the auction cycle
	 * @param packages
	 *            packages given to agents in the current auction cycle
	 * @param init
	 *            true if this is the method is called before starting any
	 *            auction cycles to store systems initial statistics
	 */
	public void addAuctionStatistics(int cycleNumber,
			HashMap<Integer, AuctionResult> resultSet, List<Landholder> agents,
			String target, Package[] packages, boolean init) {

		if (!init) {
			Object[] results = resultSet == null ? new Object[0] : resultSet
					.keySet().toArray();
			int numberOfBids = 0;
			int numberOfSuccessfulBids = 0;
			double costOfAllBids = 0;
			double costOfSuccessfulBids = 0;
			double totalOpportunityCostOfSuccessfulBids = 0;

			// Calculate cost of bids and bid numbers
			for (Object o : results) {
				int packageId = (int) o;
				numberOfBids += resultSet.get(packageId).getBids().size();
				numberOfSuccessfulBids += resultSet.get(packageId).getWinners()
						.size();

				totalOpportunityCostOfSuccessfulBids += resultSet
						.get(packageId).getWinners().size()
						* Package.getPackage(packageId).opportunityCost;

				Object[] bids = resultSet.get(packageId).getBids().values()
						.toArray();
				for (Object bid : bids) {
					costOfAllBids += (double) bid;
				}

				for (String winnerId : resultSet.get(packageId).getWinners()) {
					costOfSuccessfulBids += resultSet.get(packageId).getBids()
							.get(winnerId);
				}
			}

			double normalizedCost = 0;

			if (numberOfSuccessfulBids > 0) {
				normalizedCost = (costOfSuccessfulBids - totalOpportunityCostOfSuccessfulBids)
						* 100 / totalOpportunityCostOfSuccessfulBids;
			}

			Log.debug("normalisedCost:" + normalizedCost
					+ " | totalOppCostSuccBids:"
					+ totalOpportunityCostOfSuccessfulBids
					+ " | costOfSuccBids:" + costOfSuccessfulBids);

			// Calculate number of participants
			Set<String> participatedAgents = new HashSet<String>();
			for (Object o : results) {
				int packageId = (int) o;
				HashMap<String, Double> bids = resultSet.get(packageId)
						.getBids();

				for (Object agentId : bids.keySet().toArray()) {
					participatedAgents.add((String) agentId);
				}
			}

			String[] targetSpecies = target.split(" ");

			int achievableMalleefowls = 0;
			int achievablePhascogales = 0;
			int achievablePythons = 0;

			int bidsForPackage;
			String[] speciesInPackage;

			for (Package p : packages) {

				if (resultSet != null && resultSet.get(p.id) != null) {
					bidsForPackage = resultSet.get(p.id).getBids().size();

					speciesInPackage = p.description.split(",");

					achievableMalleefowls += Integer
							.parseInt(speciesInPackage[0]) * bidsForPackage;
					achievablePhascogales += Integer
							.parseInt(speciesInPackage[1]) * bidsForPackage;
					achievablePythons += Integer.parseInt(speciesInPackage[2])
							* bidsForPackage;
				}
			}

			double LCLPwinnersPercentage;
			double HCLPwinnersPercentage;
			double LCHPwinnersPercentage;
			double HCHPwinnersPercentage;
			double LCLPparticipantsPercentage;
			double HCLPparticipantsPercentage;
			double LCHPparticipantsPercentage;
			double HCHPparticipantsPercentage;

			if (LCLPAgents == 0) {
				LCLPwinnersPercentage = 0;
				LCLPparticipantsPercentage = 0;
			} else {
				LCLPwinnersPercentage = 100.0 * winnersLCLP / LCLPAgents;
				LCLPparticipantsPercentage = 100.0 * participantsLCLP
						/ LCLPAgents;
			}

			if (LCHPAgents == 0) {
				LCHPwinnersPercentage = 0;
				LCHPparticipantsPercentage = 0;
			} else {
				LCHPwinnersPercentage = 100.0 * winnersLCHP / LCHPAgents;
				LCHPparticipantsPercentage = 100.0 * participantsLCHP
						/ LCHPAgents;
			}

			if (HCLPAgents == 0) {
				HCLPwinnersPercentage = 0;
				HCLPparticipantsPercentage = 0;
			} else {
				HCLPwinnersPercentage = 100.0 * winnersHCLP / HCLPAgents;
				HCLPparticipantsPercentage = 100.0 * participantsHCLP
						/ HCLPAgents;
			}

			if (HCHPAgents == 0) {
				HCHPwinnersPercentage = 0;
				HCHPparticipantsPercentage = 0;
			} else {
				HCHPwinnersPercentage = 100.0 * winnersHCHP / HCHPAgents;
				HCHPparticipantsPercentage = 100.0 * participantsHCHP
						/ HCHPAgents;
			}

			accumulatedParticipantsPercentageLCLP += LCLPparticipantsPercentage;
			accumulatedParticipantsPercentageLCHP += LCHPparticipantsPercentage;
			accumulatedParticipantsPercentageHCLP += HCLPparticipantsPercentage;
			accumulatedParticipantsPercentageHCHP += HCHPparticipantsPercentage;
			accumulatedWinnersPercentageLCLP += LCLPwinnersPercentage;
			accumulatedWinnersPercentageLCHP += LCHPwinnersPercentage;
			accumulatedWinnersPercentageHCLP += HCLPwinnersPercentage;
			accumulatedWinnersPercentageHCHP += HCHPwinnersPercentage;

			int movedUpCE = 0;
			int movedDownCE = 0;
			int movedUpPM = 0;
			int movedDownPM = 0;

			int highProfWinningBids = 0;
			int medProfWinningBids = 0;
			int lowProfWinningBids = 0;

			for (Landholder agent : agents) {
				if (agent instanceof Landholder) {
					Landholder landholder = (Landholder) agent;
					if (landholder.getMoveCEcategory() == ConservationUtils.CategoryChanges.UP) {
						movedUpCE++;
					} else if (landholder.getMoveCEcategory() == ConservationUtils.CategoryChanges.DOWN) {
						movedDownCE++;
					}

					if (landholder.getMovePMcategory() == ConservationUtils.CategoryChanges.UP) {
						movedUpPM++;
					} else if (landholder.getMovePMcategory() == ConservationUtils.CategoryChanges.DOWN) {
						movedDownPM++;
					}

					highProfWinningBids += landholder
							.getHighProfitWinningBids();
					medProfWinningBids += landholder.getMedProfitWinningBids();
					lowProfWinningBids += landholder.getLowProfitWinningBids();
				}
			}

			addAuctionStatistics(cycleNumber, costOfAllBids,
					costOfSuccessfulBids, normalizedCost, numberOfBids,
					numberOfSuccessfulBids, participatedAgents.size(),
					averageCE, averagePM, LCLPAgents, HCLPAgents, LCHPAgents,
					HCHPAgents, targetSpecies[0], targetSpecies[1],
					targetSpecies[2], achievableMalleefowls,
					achievablePhascogales, achievablePythons,
					highCEAgentsPercentage, participantsLCLP, participantsHCLP,
					participantsLCHP, participantsHCHP,
					LCLPparticipantsPercentage, HCLPparticipantsPercentage,
					LCHPparticipantsPercentage, HCHPparticipantsPercentage,
					winnersLCLP, winnersHCLP, winnersLCHP, winnersHCHP,
					LCLPwinnersPercentage, HCLPwinnersPercentage,
					LCHPwinnersPercentage, HCHPwinnersPercentage, movedUpCE,
					movedDownCE, movedUpPM, movedDownPM, highProfWinningBids,
					medProfWinningBids, lowProfWinningBids);
		}

		// Calculate average Conservation Ethic and Average Profit Motivation
		LCLPAgents = 0;
		HCLPAgents = 0;
		LCHPAgents = 0;
		HCHPAgents = 0;
		averageCE = 0;
		averagePM = 0;

		for (Landholder agent : agents) {

			if (agent instanceof Landholder) {
				Landholder landholder = (Landholder) agent;
				averageCE += landholder.getConservationEthicBarometer();
				averagePM += landholder.getProfitMotiveBarometer();

				// Count agent in each category
				if (landholder.isConservationEthicHigh()) {
					if (landholder.isProfitMotivationHigh()) {
						HCHPAgents++;
					} else {
						HCLPAgents++;
					}
				} else {
					if (landholder.isProfitMotivationHigh()) {
						LCHPAgents++;
					} else {
						LCLPAgents++;
					}
				}

			} else {
				Log.warn("The agent " + agent + " is not a LandHolder.");
			}
		}

		averageCE /= agents.size();
		averagePM /= agents.size();

		int divider = LCLPAgents + LCHPAgents + HCLPAgents + HCHPAgents;

		highCEAgentsPercentage = (HCLPAgents + HCHPAgents) * 100
				/ (divider == 0 ? 1 : divider);

		participantsLCLP = 0;
		participantsLCHP = 0;
		participantsHCLP = 0;
		participantsHCHP = 0;
		winnersLCLP = 0;
		winnersLCHP = 0;
		winnersHCLP = 0;
		winnersHCHP = 0;

	}

	/**
	 * Store information about a winner
	 * 
	 * @param highC
	 *            true if the winner has high CE
	 * @param highP
	 *            true if the winner has high PM
	 */
	public void addWinner(boolean highC, boolean highP) {
		lock.lock();
		if (highC) {
			if (highP) {
				this.winnersHCHP++;
			} else {
				this.winnersHCLP++;
			}

		} else {
			if (highP) {
				this.winnersLCHP++;
			} else {
				this.winnersLCLP++;
			}
		}
		lock.unlock();
	}

	/**
	 * Store information about a participants
	 * 
	 * @param highC
	 *            true if the participant has high CE
	 * @param highP
	 *            true if the participant has high PM
	 */
	public void addParticipant(boolean highC, boolean highP) {
		lock.lock();
		if (highC) {
			if (highP) {
				this.participantsHCHP++;
			} else {
				this.participantsHCLP++;
			}

		} else {
			if (highP) {
				this.participantsLCHP++;
			} else {
				this.participantsLCLP++;
			}
		}
		lock.unlock();
	}

	/**
	 * Private method to write all statics about an auction cycle to output
	 * file.
	 * 
	 * @param cycleNumber
	 * @param costOfAllBids
	 * @param costOfSuccessfulBids
	 * @param normalizedCost
	 * @param numberOfBids
	 * @param numberOfSuccessfulBids
	 * @param numberOfParticipants
	 * @param averageCE
	 * @param averagePM
	 * @param LCLPAgents
	 * @param HCLPAgents
	 * @param LCHPAgents
	 * @param HCHPAgents
	 * @param targetMalleefowls
	 * @param targetPhascogales
	 * @param targetPythons
	 * @param achievableMalleefowls
	 * @param achievablePhascogales
	 * @param achievablePythons
	 * @param highCEAgentsPercentage
	 * @param LCLP_participants
	 * @param HCLP_participants
	 * @param LCHP_participants
	 * @param HCHP_participants
	 * @param LCLP_participants_percentage
	 * @param HCLP_participants_percentage
	 * @param LCHP_participants_percentage
	 * @param HCHP_participants_percentage
	 * @param LCLP_winners
	 * @param HCLP_winners
	 * @param LCHP_winners
	 * @param HCHP_winners
	 * @param LCLP_winners_percentage
	 * @param HCLP_winners_percentage
	 * @param LCHP_winners_percentage
	 * @param HCHP_winners_percentage
	 * @param movedUpCE
	 * @param movedDownCE
	 * @param movedUpPM
	 * @param movedDownPM
	 * @param highProfWinningBids
	 * @param medProfWinningBids
	 * @param lowProfWinningBids
	 */
	private void addAuctionStatistics(int cycleNumber, double costOfAllBids,
			double costOfSuccessfulBids, double normalizedCost,
			int numberOfBids, int numberOfSuccessfulBids,
			int numberOfParticipants, double averageCE, double averagePM,
			int LCLPAgents, int HCLPAgents, int LCHPAgents, int HCHPAgents,
			String targetMalleefowls, String targetPhascogales,
			String targetPythons, int achievableMalleefowls,
			int achievablePhascogales, int achievablePythons,
			double highCEAgentsPercentage, int LCLP_participants,
			int HCLP_participants, int LCHP_participants,
			int HCHP_participants, double LCLP_participants_percentage,
			double HCLP_participants_percentage,
			double LCHP_participants_percentage,
			double HCHP_participants_percentage, int LCLP_winners,
			int HCLP_winners, int LCHP_winners, int HCHP_winners,
			double LCLP_winners_percentage, double HCLP_winners_percentage,
			double LCHP_winners_percentage, double HCHP_winners_percentage,
			int movedUpCE, int movedDownCE, int movedUpPM, int movedDownPM,
			int highProfWinningBids, int medProfWinningBids,
			int lowProfWinningBids) {
		try {
			auctionStatisticsWriter.append(Integer.toString(cycleNumber));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Double.toString(costOfAllBids));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Double
					.toString(costOfSuccessfulBids));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Double.toString(normalizedCost));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Integer.toString(numberOfBids));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Integer
					.toString(numberOfSuccessfulBids));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Integer
					.toString(numberOfParticipants));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Double.toString(averageCE));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Double.toString(averagePM));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Integer.toString(LCLPAgents));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Integer.toString(HCLPAgents));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Integer.toString(LCHPAgents));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Integer.toString(HCHPAgents));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(targetMalleefowls);
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(targetPhascogales);
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(targetPythons);
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Integer
					.toString(achievableMalleefowls));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Integer
					.toString(achievablePhascogales));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Integer.toString(achievablePythons));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Double
					.toString(highCEAgentsPercentage));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Integer.toString(LCLP_participants));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Integer.toString(HCLP_participants));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Integer.toString(LCHP_participants));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Integer.toString(HCHP_participants));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Double
					.toString(LCLP_participants_percentage));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Double
					.toString(HCLP_participants_percentage));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Double
					.toString(LCHP_participants_percentage));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Double
					.toString(HCHP_participants_percentage));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Double
					.toString(accumulatedParticipantsPercentageLCLP));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Double
					.toString(accumulatedParticipantsPercentageHCLP));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Double
					.toString(accumulatedParticipantsPercentageLCHP));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Double
					.toString(accumulatedParticipantsPercentageHCHP));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Integer.toString(LCLP_winners));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Integer.toString(HCLP_winners));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Integer.toString(LCHP_winners));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Integer.toString(HCHP_winners));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Double
					.toString(LCLP_winners_percentage));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Double
					.toString(HCLP_winners_percentage));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Double
					.toString(LCHP_winners_percentage));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Double
					.toString(HCHP_winners_percentage));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Double
					.toString(accumulatedWinnersPercentageLCLP));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Double
					.toString(accumulatedWinnersPercentageHCLP));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Double
					.toString(accumulatedWinnersPercentageLCHP));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Double
					.toString(accumulatedWinnersPercentageHCHP));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Integer.toString(movedUpCE));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Integer.toString(movedDownCE));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Integer.toString(movedUpPM));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Integer.toString(movedDownPM));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter.append(Integer
					.toString(highProfWinningBids));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter
					.append(Integer.toString(medProfWinningBids));
			auctionStatisticsWriter.append(",");
			auctionStatisticsWriter
					.append(Integer.toString(lowProfWinningBids));
			auctionStatisticsWriter.append("\n");
			auctionStatisticsWriter.flush();

		} catch (IOException e) {
			Log.error(e.getMessage());
		}

	}

	/**
	 * Closes the file wrter.
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		auctionStatisticsWriter.close();
	}
}
