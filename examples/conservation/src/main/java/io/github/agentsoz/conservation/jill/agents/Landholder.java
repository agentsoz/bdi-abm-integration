package io.github.agentsoz.conservation.jill.agents;

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

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.conservation.AuctionResultSet;
import io.github.agentsoz.conservation.ConservationUtils;
import io.github.agentsoz.conservation.ContractsList;
import io.github.agentsoz.conservation.Global;
import io.github.agentsoz.conservation.LandholderHistory;
import io.github.agentsoz.conservation.Package;
import io.github.agentsoz.conservation.LandholderHistory.AuctionRound;
import io.github.agentsoz.conservation.LandholderHistory.BidResult;
import io.github.agentsoz.conservation.Log;
import io.github.agentsoz.conservation.jill.goals.AuctionResultGoal;
import io.github.agentsoz.conservation.jill.goals.CallForBidsGoal;
import io.github.agentsoz.conservation.jill.goals.MeetExtensionOfficerGoal;
import io.github.agentsoz.conservation.outputwriters.AuctionStatisticsWriter;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.AgentInfo;

import java.io.PrintStream;

import com.google.gson.Gson;

/**
 * Land holder agent representation in BDI
 * 
 * @author Sewwandi Perera
 */
@AgentInfo(hasGoals = { "io.github.agentsoz.conservation.jill.goals.CallForBidsGoal",
		"io.github.agentsoz.conservation.jill.goals.DecideParticipationGoal",
		"io.github.agentsoz.conservation.jill.goals.DecideBidsGoal",
		"io.github.agentsoz.abmjill.genact.EnvironmentAction",
		"io.github.agentsoz.conservation.jill.goals.AuctionResultGoal",
		"io.github.agentsoz.conservation.jill.goals.UpdateConservationEthicGoal",
		"io.github.agentsoz.conservation.jill.goals.UpdateProfitMotivationGoal",
		"io.github.agentsoz.conservation.jill.goals.SocialNormUpdateGoal",
		"io.github.agentsoz.conservation.jill.goals.MeetExtensionOfficerGoal"
		})
public class Landholder extends Agent implements io.github.agentsoz.bdiabm.Agent {

	/**
	 * This agent's ID in the GAMS system
	 */
	private String gamsID;
	
	/**
	 * Profit motive barometer of the land holder
	 */
	private double profitMotiveBarometer;

	/**
	 * Conservation ethic barometer of the land holder
	 */
	private double conservationEthicBarometer;

	/**
	 * Information about land holder's previous three successful auction cycles
	 * stored as an instance of {@link LandholderHistory}
	 */
	private LandholderHistory history;

	/**
	 * Is land holder's conservation ethic barometer is considered as a high
	 * value
	 */
	private boolean isConservationEthicHigh;

	/**
	 * Is land holder's profit motivation barometer is considered as a high
	 * value
	 */
	private boolean isProfitMotivationHigh;

	/**
	 * Agent's decision on participating on the current auction cycle
	 */
	private boolean decisionOnParticipation;

	/**
	 * Jill output stream
	 */
	public PrintStream out = System.out;

	/**
	 * Results of current auction round. This is what stored in the history
	 */
	private AuctionRound currentRound;

	/**
	 * How the land holder changed his conservation ethic category during the
	 * auction.
	 * 
	 * UP: if moved from low CE to high CE.
	 * 
	 * DOWN: if moved from high CE to low CE.
	 * 
	 * NONE: if did not change the CE category.
	 */
	private ConservationUtils.CategoryChanges moveCEcategory;

	/**
	 * How the land holder changed his profit motivation category during the
	 * auction.
	 * 
	 * UP: if moved from low PM to high PM.
	 * 
	 * DOWN: if moved from high PM to low PM.
	 * 
	 * NONE: if did not change the PM category.
	 */
	private ConservationUtils.CategoryChanges movePMcategory;

	/**
	 * number of successful, high profit bids made by the land holder
	 */
	private int highProfitWinningBids;

	/**
	 * number of successful, medium profit bids made by the land holder
	 */
	private int medProfitWinningBids;

	/**
	 * number of successful, low profit bids made by the land holder
	 */
	private int lowProfitWinningBids;
	
	/**
	 * list of all won contracts that this agent has ever had
	 */
	private ContractsList contracts;

	/**
	 * public constructor
	 * 
	 * @param name
	 */
	public Landholder(String name) {
		super(name);
		gamsID = name;
		history = new LandholderHistory();
		contracts = new ContractsList();
		setMoveCEcategory(ConservationUtils.CategoryChanges.NONE);
		setMovePMcategory(ConservationUtils.CategoryChanges.NONE);
	}

	/**
	 * Initialise the land holder
	 * 
	 * @param profitMotiveBarometer
	 * @param conservationEthicBarometer
	 * @param highCE
	 */
	public void init(double profitMotiveBarometer,
			double conservationEthicBarometer, boolean highCE, 
			String bdiID, String gamsID) {
		this.setName(bdiID);
		this.gamsID = gamsID;
		this.conservationEthicBarometer = conservationEthicBarometer;
		this.profitMotiveBarometer = profitMotiveBarometer;
		this.isConservationEthicHigh = highCE;
		this.isProfitMotivationHigh = isProfitMotivationHigh(this.profitMotiveBarometer);
		decisionOnParticipation = false;
		Log.debug("Agent " + getName() + " initialised with C:"
				+ conservationEthicBarometer + " P:" + profitMotiveBarometer);
	}

	/**
	 * Returns true is land holder's conservation ethic barometer is high
	 * 
	 * @return
	 */
	public boolean isConservationEthicHigh() {
		return isConservationEthicHigh;
	}

	/**
	 * Returns true is land holder's profit motivation barometer is high
	 * 
	 * @return
	 */
	public boolean isProfitMotivationHigh() {
		return isProfitMotivationHigh;
	}

	/**
	 * Set the category (high or low) of land holder's conservation ethic
	 * barometer
	 * 
	 * @param value
	 */
	public void setConservationEthicHigh(boolean value) {
		isConservationEthicHigh = value;
	}

	/**
	 * Set the category (high or low) of land holder's profit motive barometer
	 * 
	 * @param value
	 */
	public void setProfitMotivationHigh(boolean value) {
		isProfitMotivationHigh = value;
	}

	/**
	 * @return land holder's profit motive barometer
	 */
	public double getProfitMotiveBarometer() {
		return profitMotiveBarometer;
	}

	/**
	 * @return land holder's conservation ethic barometer
	 */
	public double getConservationEthicBarometer() {
		return conservationEthicBarometer;
	}

	/**
	 * Assign a given value as land holder's profit motivation barometer. If the
	 * given value is out of the range of profit motive barometer, the nearest
	 * margin is assigned.
	 * 
	 * @param value
	 *            the given value
	 * @return the assigned value
	 */
	public double setProfitMotiveBarometer(double value) {
		if (Double.isNaN(value)) {
			profitMotiveBarometer = 0;
		} else if (value > ConservationUtils.getMaxProfitMotivation()) {
			profitMotiveBarometer = ConservationUtils.getMaxProfitMotivation();
		} else if (value < 0) {
			profitMotiveBarometer = 0;
		} else {
			profitMotiveBarometer = value;
		}

		return profitMotiveBarometer;
	}

	/**
	 * Assign a given value as land holder's conservation ethic barometer. If
	 * the given value is out of the range of conservation ethic barometer, the
	 * nearest margin is assigned.
	 * 
	 * @param value
	 *            the given value
	 * @return the assigned value
	 */
	public double setConservationEthicBarometer(double value) {
		if (Double.isNaN(value)) {
			conservationEthicBarometer = 0;
		} else if (value > ConservationUtils.getMaxProfitMotivation()) {
			conservationEthicBarometer = ConservationUtils
					.getMaxConservationEthic();
		} else if (value < 0) {
			conservationEthicBarometer = 0;
		} else {
			conservationEthicBarometer = value;
		}

		return conservationEthicBarometer;
	}

	/**
	 * @return history of previous three successful auction rounds is returned
	 *         as an instance of {@link LandholderHistory}
	 */
	public LandholderHistory getHistory() {
		return history;
	}

	/**
	 * @return the results of the current auction round as an instance of
	 *         {@link AuctionRound}
	 */
	public AuctionRound getCurrentAuctionRound() {
		return this.currentRound;
	}

	/**
	 * Store the land holder's decision on participating the auction.
	 * 
	 * @param decision
	 *            true if the land holder decided to participate in the auction,
	 *            false if not
	 */
	public void setDecisionOnParticipation(boolean decision) {
		decisionOnParticipation = decision;
	}

	/**
	 * @return the land holder's decision on participating the auction round
	 */
	public boolean getDecisionOnParticipation() {
		return decisionOnParticipation;
	}

	/**
	 * Checks whether a given Conservation Ethic Barometer is high or low
	 * 
	 * @param c
	 * @return
	 */
	public boolean isConservationEthicHigh(double c) {

		double upperThresholdC = ConservationUtils.getUpperThresholdC();
		double lowerThresholdC = ConservationUtils.getLowerThresholdC();

		if (c > upperThresholdC) {
			return true;
		} else if (c < lowerThresholdC) {
			return false;
		} else {
			double probability = 1 - (((upperThresholdC - c) * 1.0) / (upperThresholdC - lowerThresholdC));
			if (ConservationUtils.getGlobalRandom().nextDouble() < probability) {
				return true;
			} else {
				return false;
			}
		}
	}

	/**
	 * Checks whether a given Profit Motivation Barometer is high or low
	 * 
	 * @param p
	 * @return
	 */
	public boolean isProfitMotivationHigh(double p) {
		double upperThresholdP = ConservationUtils.getUpperThresholdP();
		double lowerThresholdP = ConservationUtils.getLowerThresholdP();

		if (p > upperThresholdP) {
			return true;
		} else if (p < lowerThresholdP) {
			return false;
		} else {
			double probability = 1 - (((upperThresholdP - p) * 1.0) / (upperThresholdP - lowerThresholdP));
			if (ConservationUtils.getGlobalRandom().nextDouble() < probability) {
				return true;
			} else {
				return false;
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void start(PrintStream writer, String[] params) {
		this.out = writer;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void handlePercept(String percept, Object params) {
		Log.debug("Agent " + getName() + " received percept " + percept
				+ ": " + new Gson().toJson(params));
		if (percept.equals(Global.percepts.AUCTION_RESULTS.toString())) {
			Object[] inputs = (Object[]) params;
			AuctionResultSet ars = (AuctionResultSet) inputs[0];
			this.currentRound = history.registerAuctionRound(ars, gamsID());

			// Update auction statistics
			if (currentRound.isWon()) {
				AuctionStatisticsWriter.getInstance().addWinner(
						isConservationEthicHigh(), isProfitMotivationHigh());
			}
			
			// Record a new contract period if the agent has won
			for(BidResult result : currentRound.getMyBids()) {
				if (result.isWon()) {
					contracts.addNew();
				}
			}


			Log.debug("Agent " + getName() + " updated history: "
					+ history.toString());
			post(new AuctionResultGoal(percept, currentRound,
					(double) inputs[1]));

		} else if (percept.equals(Global.percepts.CALL_FOR_BIDS.toString())) {
			Package[] packages = (Package[]) params;
			boolean registered = history.registerPackages(packages);
			if (registered) {
				CallForBidsGoal callForBids = new CallForBidsGoal(percept);
				callForBids.setPackages(packages);
				post(callForBids);
			}
		} else if (percept.equals(Global.percepts.EXTENSION_OFFICER_VISIT.toString())) {
			post(new MeetExtensionOfficerGoal(percept));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(String[] args) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void start() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void packageAction(String actionID, Object[] parameters) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateAction(String actionID, ActionContent content) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void kill() {
		super.finish();
	}

	/**
	 * @return how did the land holder change high or low category of
	 *         conservation ethic barometer during the auction round. Can be
	 *         either "NONE", "UP, or "DOWN"
	 */
	public ConservationUtils.CategoryChanges getMoveCEcategory() {
		return moveCEcategory;
	}

	/**
	 * Set how did the land holder change his conservation ethic category
	 * 
	 * @param moveCEcategory
	 *            can be either NONE, UP or DOWN
	 */
	public void setMoveCEcategory(
			ConservationUtils.CategoryChanges moveCEcategory) {
		this.moveCEcategory = moveCEcategory;
	}

	/**
	 * @return how did the land holder change high or low category of profit
	 *         motive barometer during the auction round. Can be either "NONE",
	 *         "UP, or "DOWN"
	 */
	public ConservationUtils.CategoryChanges getMovePMcategory() {
		return movePMcategory;
	}

	/**
	 * Set how did the land holder change his profit motive category
	 * 
	 * @param moveCEcategory
	 *            can be either NONE, UP or DOWN
	 */
	public void setMovePMcategory(
			ConservationUtils.CategoryChanges movePMcategory) {
		this.movePMcategory = movePMcategory;
	}

	/**
	 * @return the number of successful bids, which were made with high profit
	 */
	public int getHighProfitWinningBids() {
		return highProfitWinningBids;
	}

	/**
	 * Set the number of bids made by the land holder with high profit were
	 * successful during the auction cycle.
	 * 
	 * @param highProfitWinningBids
	 */
	public void setHighProfitWinningBids(int highProfitWinningBids) {
		this.highProfitWinningBids = highProfitWinningBids;
	}

	/**
	 * @return the number of successful bids, which were made with medium profit
	 */
	public int getMedProfitWinningBids() {
		return medProfitWinningBids;
	}

	/**
	 * Set the number of bids made by the land holder with medium profit were
	 * successful during the auction cycle.
	 * 
	 * @param medProfitWinningBids
	 */
	public void setMedProfitWinningBids(int medProfitWinningBids) {
		this.medProfitWinningBids = medProfitWinningBids;
	}

	/**
	 * @return the number of successful bids, which were made with low profit
	 */
	public int getLowProfitWinningBids() {
		return lowProfitWinningBids;
	}

	/**
	 * Set the number of bids made by the land holder with low profit were
	 * successful during the auction cycle.
	 * 
	 * @param lowProfitWinningBids
	 */
	public void setLowProfitWinningBids(int lowProfitWinningBids) {
		this.lowProfitWinningBids = lowProfitWinningBids;
	}

	/**
	 * Increase highProfitWinningBids by one
	 */
	public void increaseHighProfitWinningBids() {
		this.highProfitWinningBids++;
	}

	/**
	 * Increase medProfitWinningBids by one
	 */
	public void increaseMedProfitWinningBids() {
		this.medProfitWinningBids++;
	}

	/**
	 * Increase lowProfitWinningBids by one
	 */
	public void increaseLowProfitWinningBids() {
		this.lowProfitWinningBids++;
	}
	
	/**
	 * Returns this agent's list of contracts
	 * @return
	 */
	public ContractsList getContracts() {
		return contracts;
	}
	
	public String gamsID() {
		return gamsID;
	}
}
