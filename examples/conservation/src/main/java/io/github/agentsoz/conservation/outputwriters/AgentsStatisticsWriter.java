package io.github.agentsoz.conservation.outputwriters;

import io.github.agentsoz.conservation.Main;

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

import io.github.agentsoz.conservation.LandholderHistory.AuctionRound;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class writes statistics about agents to csv files. A separate file is
 * generated for each repeat. Rows of the output file represent auction cycles.
 * Columns represent agents.
 * 
 * The statistics written in to different output files:
 * 
 * agents_ce_<repeat>.csv : conservation ethic barometers of agents
 * 
 * agents_pm_<repeat>.csv : profit motive barometers of agents
 * 
 * number_of_bids_per_agent<repeat>.csv : number of bids made by each agent in
 * each auction cycle.
 * 
 * number_of_successful_bids_per_agent<repeat>.csv : number of successful bids
 * made by each agent in each auction cycle.
 * 
 * total_opportunity_cost_per_agent<repeat>.csv : total opportunity cost of all
 * bids made by each agent in each auction cycle
 * 
 * successful_opportunity_cost_per_agent<repeat>.csv : total opportunity cost of
 * all successful bids made by each agent in each auction cycle
 * 
 * total_bid_price_per_agent<repeat>.csv : total price of all bids made by each
 * agent in each auction cycle
 * 
 * successful_bid_price_per_agent<repeat>.csv : total price of all successful
 * bids made by each agent in each auction cycle
 * 
 * @author Sewwandi Perera
 */
public class AgentsStatisticsWriter {

    final private Logger logger = LoggerFactory.getLogger(Main.LOGGER_NAME);

	/**
	 * Current repeat
	 */
	private int repeat;

	/**
	 * Singleton instance
	 */
	private static AgentsStatisticsWriter instance = new AgentsStatisticsWriter();

	/**
	 * All agents conservation ethic barometers in each auction cycle. Key
	 * fields of the TreeMap contain the auction cycles. Value fields contain
	 * another TreeMap of agent names and their CE barometers.
	 */
	private TreeMap<Integer, TreeMap<String, Double>> agentsCE;

	/**
	 * All agents profit motive barometers in each auction cycle. Key fields of
	 * the TreeMap contain the auction cycles. Value fields contain a TreeMap of
	 * agent names and their PM barometers.
	 */
	private TreeMap<Integer, TreeMap<String, Double>> agentsPM;

	/**
	 * Number of bids made by each agent in each auction cycle. Key fields of
	 * the TreeMap contain the auction cycles. Value fields contain a TreeMap of
	 * agent names and number of bids they made.
	 */
	private TreeMap<Integer, TreeMap<String, Double>> numberOfBids;

	/**
	 * Number of successful bids made by each agent in each auction cycle. Key
	 * fields of the TreeMap contain the auction cycles. Value fields contain a
	 * TreeMap of agent names and the number of successful bids they made.
	 */
	private TreeMap<Integer, TreeMap<String, Double>> numberOfSuccessfulBids;

	/**
	 * Total opportunity cost of all bids made by each agent in each auction
	 * cycle. Key fields of the TreeMap contain the auction cycles. Value fields
	 * contain a TreeMap of agent names and total opportunity cost of all bids
	 * they made.
	 */
	private TreeMap<Integer, TreeMap<String, Double>> totalOpportunityCost;

	/**
	 * Total opportunity cost of successful bids made by each agent in each
	 * auction cycle. Key fields of the TreeMap contain the auction cycles.
	 * Value fields contain a TreeMap of agent names and total opportunity cost
	 * of successful bids they made.
	 */
	private TreeMap<Integer, TreeMap<String, Double>> totalOpportunityCostOfSuccessfulBids;

	/**
	 * Total price of all bids made by each agent in each auction cycle. Key
	 * fields of the TreeMap contain the auction cycles. Value fields contain a
	 * TreeMap of agent names and total price of all bids they made.
	 */
	private TreeMap<Integer, TreeMap<String, Double>> totalBidPrices;

	/**
	 * Total price of successful bids made by each agent in each auction cycle.
	 * Key fields of the TreeMap contain the auction cycles. Value fields
	 * contain a TreeMap of agent names and total price of successful bids they
	 * made.
	 */
	private TreeMap<Integer, TreeMap<String, Double>> totalBidPricesOfSuccessfulBids;

	/**
	 * Number of agents in the simulation
	 */
	private int numOfAgents;

	/**
	 * @return singleton instance of the class
	 */
	public static AgentsStatisticsWriter getInstance() {
		return instance;
	}

	/**
	 * Open the {@link AgentsStatisticsWriter}.
	 * 
	 * @param repeat
	 *            current repeat number
	 * @param numOfAgents
	 *            number of agents in the simulation
	 */
	public void open(int repeat, int numOfAgents) {
		this.repeat = repeat;
		this.numOfAgents = numOfAgents;
	}

	/**
	 * Private constructor
	 */
	private AgentsStatisticsWriter() {
		numOfAgents = 0;

		// Initialise all TreeMaps that are used to store agents' statistics
		agentsCE = new TreeMap<Integer, TreeMap<String, Double>>();
		agentsPM = new TreeMap<Integer, TreeMap<String, Double>>();
		numberOfBids = new TreeMap<Integer, TreeMap<String, Double>>();
		numberOfSuccessfulBids = new TreeMap<Integer, TreeMap<String, Double>>();
		totalOpportunityCost = new TreeMap<Integer, TreeMap<String, Double>>();
		totalOpportunityCostOfSuccessfulBids = new TreeMap<Integer, TreeMap<String, Double>>();
		totalBidPrices = new TreeMap<Integer, TreeMap<String, Double>>();
		totalBidPricesOfSuccessfulBids = new TreeMap<Integer, TreeMap<String, Double>>();
	}

	/**
	 * Should be called at the end of the repeat to flush everything.
	 */
	public void flush() {
		try {
			GZIPOutputStream zip;
			
			zip = new GZIPOutputStream(new FileOutputStream(ConstantFileNames.getAgentsCEFileName(repeat)));
			BufferedWriter ceWriter = new BufferedWriter(new OutputStreamWriter(zip, "UTF-8"));			
			writeAndClose(ceWriter, agentsCE);

			zip = new GZIPOutputStream(new FileOutputStream(ConstantFileNames.getAgentsPmFile(repeat)));
			BufferedWriter pmWriter = new BufferedWriter(new OutputStreamWriter(zip, "UTF-8"));			
			writeAndClose(pmWriter, agentsPM);

			zip = new GZIPOutputStream(new FileOutputStream(ConstantFileNames.getNumberOfBidsPerAgent(repeat)));
			BufferedWriter numOfBidsWriter = new BufferedWriter(new OutputStreamWriter(zip, "UTF-8"));			
			writeAndClose(numOfBidsWriter, numberOfBids);

			zip = new GZIPOutputStream(new FileOutputStream(ConstantFileNames.getNumberOfSuccessfulBidsPerAgent(repeat)));
			BufferedWriter numOfSuccessBidsWriter = new BufferedWriter(new OutputStreamWriter(zip, "UTF-8"));			
			writeAndClose(numOfSuccessBidsWriter, numberOfSuccessfulBids);

			zip = new GZIPOutputStream(new FileOutputStream(ConstantFileNames.getTotalOpportunityCostPerAgent(repeat)));
			BufferedWriter totalOpportunityCostWriter = new BufferedWriter(new OutputStreamWriter(zip, "UTF-8"));			
			writeAndClose(totalOpportunityCostWriter, totalOpportunityCost);

			zip = new GZIPOutputStream(new FileOutputStream(ConstantFileNames.getSuccessfulOpportunityCostPerAgent(repeat)));
			BufferedWriter successOpportunityCostWriter = new BufferedWriter(new OutputStreamWriter(zip, "UTF-8"));			
			writeAndClose(successOpportunityCostWriter, totalOpportunityCostOfSuccessfulBids);

			zip = new GZIPOutputStream(new FileOutputStream(ConstantFileNames.getTotalBidPricePerAgent(repeat)));
			BufferedWriter totalBidPricesWriter = new BufferedWriter(new OutputStreamWriter(zip, "UTF-8"));			
			writeAndClose(totalBidPricesWriter, totalBidPrices);

			zip = new GZIPOutputStream(new FileOutputStream(ConstantFileNames.getSuccessfulBidPricePerAgent(repeat)));
			BufferedWriter successBidPricesWriter = new BufferedWriter(new OutputStreamWriter(zip, "UTF-8"));			
			writeAndClose(successBidPricesWriter, totalBidPricesOfSuccessfulBids);

		} catch (IOException e) {
			logger.error(e.getMessage());
		}

	}

	/**
	 * This private method is used by "flush" method to write statistics to
	 * relevant output csv file
	 * 
	 * @param writer
	 * @param values
	 */
	private void writeAndClose(BufferedWriter writer,
			TreeMap<Integer, TreeMap<String, Double>> values) {
		try {
			// append header
			writer.append("cycle_number,agentId,value");
			writer.append("\n");

			// append values
			for (int cycleNumber : values.keySet()) {
				TreeMap<String, Double> perAgentValues = values
						.get(cycleNumber);

				for (int i = 0; i < numOfAgents; i++) {
					String sAgent = Integer.toString(i);
					writer.append(String.format("%03d,%04d,%07.3f\n", cycleNumber, i, perAgentValues.get(sAgent)));
				}
			}

			writer.flush();
			writer.close();

		} catch (IOException e) {
			logger.error(e.getMessage());
		}

	}

	/**
	 * Stores conservation ethic barometers of agents
	 * 
	 * @param cycle
	 *            current auction cycle
	 * @param agentName
	 *            agents name
	 * @param value
	 *            conservation ethic barometer of the agent
	 */
	public void addAgentsCE(int cycle, String agentName, double value) {
		TreeMap<String, Double> storedInfo = agentsCE.get(cycle);

		if (storedInfo == null) {
			storedInfo = new TreeMap<String, Double>();
		}

		storedInfo.put(agentName, value);
		agentsCE.put(cycle, storedInfo);
	}

	/**
	 * Stores profit motive barometers of agents
	 * 
	 * @param cycle
	 *            current auction cycle
	 * @param agentName
	 *            agents name
	 * @param value
	 *            profit motive barometer of the agent
	 */
	public void addAgentsPM(int cycle, String agentName, double value) {
		TreeMap<String, Double> storedInfo = agentsPM.get(cycle);

		if (storedInfo == null) {
			storedInfo = new TreeMap<String, Double>();
		}

		storedInfo.put(agentName, value);
		agentsPM.put(cycle, storedInfo);
	}

	/**
	 * Stores number of bids made by agents
	 * 
	 * @param cycle
	 *            current auction cycle
	 * @param agentName
	 *            agents name
	 * @param value
	 *            number of bids made by the agent
	 */
	public void addNumberOfBids(int cycle, String agentName, double value) {
		TreeMap<String, Double> storedInfo = numberOfBids.get(cycle);

		if (storedInfo == null) {
			storedInfo = new TreeMap<String, Double>();
		}

		storedInfo.put(agentName, value);
		numberOfBids.put(cycle, storedInfo);
	}

	/**
	 * Stores number of successful bids made by agents
	 * 
	 * @param cycle
	 *            current auction cycle
	 * @param agentName
	 *            agents name
	 * @param value
	 *            number of successful bids made by the agent
	 */
	public void addNumberOfSuccessfulBids(int cycle, String agentName,
			double value) {
		TreeMap<String, Double> storedInfo = numberOfSuccessfulBids.get(cycle);

		if (storedInfo == null) {
			storedInfo = new TreeMap<String, Double>();
		}

		storedInfo.put(agentName, value);
		numberOfSuccessfulBids.put(cycle, storedInfo);
	}

	/**
	 * Stores total opportunity cost of all bids made by agents
	 * 
	 * @param cycle
	 *            current auction cycle
	 * @param agentName
	 *            agents name
	 * @param value
	 *            total opportunity cost of all bids made by the agent
	 */
	public void addTotalOpportunityCost(int cycle, String agentName,
			double value) {
		TreeMap<String, Double> storedInfo = totalOpportunityCost.get(cycle);

		if (storedInfo == null) {
			storedInfo = new TreeMap<String, Double>();
		}

		storedInfo.put(agentName, value);
		totalOpportunityCost.put(cycle, storedInfo);
	}

	/**
	 * Stores total opportunity cost of successful bids made by agents
	 * 
	 * @param cycle
	 *            current auction cycle
	 * @param agentName
	 *            agents name
	 * @param value
	 *            total opportunity cost of successful bids made by the agent
	 */
	public void addTotalOpportunityCostOfSuccessfulBids(int cycle,
			String agentName, double value) {
		TreeMap<String, Double> storedInfo = totalOpportunityCostOfSuccessfulBids
				.get(cycle);

		if (storedInfo == null) {
			storedInfo = new TreeMap<String, Double>();
		}

		storedInfo.put(agentName, value);
		totalOpportunityCostOfSuccessfulBids.put(cycle, storedInfo);
	}

	/**
	 * Stores total price of all bids made by agents
	 * 
	 * @param cycle
	 *            current auction cycle
	 * @param agentName
	 *            agents name
	 * @param value
	 *            total price of all bids made by the agent
	 */
	public void addTotalBidPrices(int cycle, String agentName, double value) {
		TreeMap<String, Double> storedInfo = totalBidPrices.get(cycle);

		if (storedInfo == null) {
			storedInfo = new TreeMap<String, Double>();
		}

		storedInfo.put(agentName, value);
		totalBidPrices.put(cycle, storedInfo);
	}

	/**
	 * Stores total price of successful bids made by agents
	 * 
	 * @param cycle
	 *            current auction cycle
	 * @param agentName
	 *            agents name
	 * @param value
	 *            total price of successful bids made by the agent
	 */
	public void addTotalBidPricesOfSuccessfulBids(int cycle, String agentName,
			double value) {
		TreeMap<String, Double> storedInfo = totalBidPricesOfSuccessfulBids
				.get(cycle);

		if (storedInfo == null) {
			storedInfo = new TreeMap<String, Double>();
		}

		storedInfo.put(agentName, value);
		totalBidPricesOfSuccessfulBids.put(cycle, storedInfo);
	}

	/**
	 * Store all statistics of agent for current auction cycle
	 * 
	 * @param cycle
	 *            current cycle number
	 * @param agentName
	 *            name of the agent
	 * @param CE
	 *            agent's CE barometer
	 * @param PM
	 *            agent's PM barometer
	 * @param lastAuctionRound
	 *            agents results of current auction cycle
	 */
	public void addAllInfo(int cycle, String agentName, double CE, double PM,
			AuctionRound lastAuctionRound) {
		addAgentsCE(cycle, agentName, CE);
		addAgentsPM(cycle, agentName, PM);

		if (cycle == 0) {

			addNumberOfBids(cycle, agentName, 0);
			addNumberOfSuccessfulBids(cycle, agentName, 0);
			addTotalOpportunityCost(cycle, agentName, 0);
			addTotalOpportunityCostOfSuccessfulBids(cycle, agentName, 0);
			addTotalBidPrices(cycle, agentName, 0);
			addTotalBidPricesOfSuccessfulBids(cycle, agentName, 0);

		} else {

			addNumberOfBids(cycle, agentName,
					lastAuctionRound.getNumberOfBids());
			addNumberOfSuccessfulBids(cycle, agentName,
					lastAuctionRound.getNumberOfSuccessfulBids());
			addTotalOpportunityCost(cycle, agentName,
					lastAuctionRound.getTotalOpportunityCostOfAllMyBids());
			addTotalOpportunityCostOfSuccessfulBids(cycle, agentName,
					lastAuctionRound
							.getTotalOpportunityCostOfMySuccessfulBids());
			addTotalBidPrices(cycle, agentName,
					lastAuctionRound.getTotalPriceOfAllMyBids());
			addTotalBidPricesOfSuccessfulBids(cycle, agentName,
					lastAuctionRound.getTotalPriceOfMySuccessfulBids());
		}

	}
}
