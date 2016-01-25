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

import io.github.agentsoz.bdiabm.data.ActionContainer;
import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.bdiabm.data.AgentStateList;
import io.github.agentsoz.bdiabm.data.ActionContent.State;
import io.github.agentsoz.conservation.jill.agents.Landholder;
import io.github.agentsoz.conservation.outputwriters.ConstantFileNames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import io.github.agentsoz.bdigams.GAMSModel;

/**
 * Provide access to the {@link GAMSModel} and preforms auctioneer's actions.
 * 
 * @author Sewwandi Perera
 */
public class AuctioneerModel extends GAMSModel {

	/**
	 * {@link LandholderModel} instance
	 */
	private LandholderModel landholderModel;

	/**
	 * Stores all agents percept and action data.
	 */
	private AgentDataContainer adc;

	/**
	 * Agents' status
	 */
	private AgentStateList asl;

	/**
	 * State of the auction
	 */
	private AuctionState auctionState;

	/**
	 * Latest auction results
	 */
	private AuctionResultSet latestResultSet;

	/**
	 * Different states of the auction
	 */
	public enum AuctionState {
		NOT_STARTED, CALL_FOR_BIDS_SEND, RESULTS_SENT, ENDED,
	}

	/**
	 * Public Constructor
	 * 
	 * @param gamsDir
	 * @param gamsFile
	 */
	public AuctioneerModel(String gamsDir, String gamsFile) {
		super(gamsDir, gamsFile);
		auctionState = AuctionState.NOT_STARTED;
		latestResultSet = null;
	}

	/**
	 * Returns the latest auction results as a {@link AuctionResultSet}
	 * 
	 * @return
	 */
	public AuctionResultSet getLatestAuctionResultSet() {
		return latestResultSet;
	}

	/**
	 * Connect {@link LandholderModel} and agents' information to the
	 * {@link AuctioneerModel}
	 * 
	 * @param landholderModel
	 * @param adc
	 * @param asl
	 */
	public void connect(LandholderModel landholderModel,
			AgentDataContainer adc, AgentStateList asl) {
		this.landholderModel = landholderModel;
		this.adc = adc;
		this.setAsl(asl);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void takeControl(AgentDataContainer agentDataContainer) {
		switch (auctionState) {
		case NOT_STARTED:
			auctionState = AuctionState.CALL_FOR_BIDS_SEND;
			break;
		case CALL_FOR_BIDS_SEND:
			auctionState = AuctionState.RESULTS_SENT;
			break;
		case RESULTS_SENT:
			auctionState = AuctionState.ENDED;
			break;
		case ENDED:
			break;
		}
	}

	/**
	 * Start auction by sending the packages to land holders, when all agents
	 * finished bidding send the bids to the {@link GAMSModel}, send back the
	 * results to land holders.
	 */
	public void conductAuction() {
		// Send call for bids
		adc.getOrCreate("global").getPerceptContainer()
				.put(Global.percepts.CALL_FOR_BIDS.toString(), Main.packages);
		landholderModel.takeControl(adc);
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
		}

		// Process the bids (as well as set the BID BDI-actions to PASSED)
		ArrayList<String> bids = new ArrayList<String>();
		for (String agentId : adc.keySet()) {
			ActionContainer ac = adc.getOrCreate(agentId).getActionContainer();
			ActionContent acc = ac.get(Global.actions.BID.toString());
			if (acc != null) {
				for (Object object : acc.getParameters()) {
					Bid bid = (Bid) object;
					String agentName = ((Landholder) landholderModel
							.getAgent(Integer.parseInt(agentId))).getName();
					bids.add(String.format("%d,%s,%s,%011.9f", bid.id,
							agentName, Main.packages[bid.id - 1].description,
							bid.price));
				}
				acc.setState(State.PASSED);
			}
		}

		Object[] inputs = new Object[2];

		if (bids.isEmpty()) {
			Log.warn("No bids made for the auction round ");
			latestResultSet = new AuctionResultSet(null);
		} else {
			// Call GAMS with the input
			runGAMSModel(bids);

			latestResultSet = new AuctionResultSet(Main.GAMSOutputFile());
		}

		inputs[0] = latestResultSet;
		inputs[1] = this.getAverageConservationEthic();

		adc.getOrCreate("global").getPerceptContainer()
				.put(Global.percepts.AUCTION_RESULTS.toString(), inputs);
		landholderModel.takeControl(adc);

		try {
			Thread.sleep(10);
		} catch (InterruptedException e1) {
		}
	}

	/**
	 * Run {@link GAMSModel} when the list of bids made by all land holders are
	 * given
	 * 
	 * @param list
	 */
	public void runGAMSModel(ArrayList<String> list) {
		// Prepare GAMS input
		ArrayList<String> bids = new ArrayList<String>(list.subList(0,
				list.size()));

		Collections.sort(bids);
		String bidstr = "dummy,dummy,Malleefowl,Phascogale,Python,Bid";
		for (String bid : bids) {
			bidstr += "\n" + bid.toString();
		}
		Log.debug("Running GAMS with input:\n" + bidstr);
		Log.csvWrite(bidstr);

		// Run GAMS here
		ArrayList<String> output = new ArrayList<String>();
		HashMap<String, String> opts = new HashMap<String, String>();
		opts.put("target_table", ConstantFileNames.getTargetFileName());
		opts.put("csvInputFile", Main.GAMSInputFile());
		opts.put("csvOutputFile", Main.GAMSOutputFile());
		opts.put("numPackages", Integer.toString(Main.numPackages()));
		opts.put("numBidders", Integer.toString(Main.numLandholders()));

		super.run(opts, bids, output);

		// Read GAMS output

	}

	/**
	 * Returns the state of the auction
	 * 
	 * @return
	 */
	public AuctionState getAuctionState() {
		return auctionState;
	}

	/**
	 * Returns the average of conservation ethic barometers of all agents.
	 * 
	 * @return
	 */
	private double getAverageConservationEthic() {
		List<Landholder> agents = Main.landholders;
		double accumulatedConservationEthic = 0;
		for (Landholder agent : agents) {
			Landholder landholder = (Landholder) agent;
			accumulatedConservationEthic += landholder
					.getConservationEthicBarometer();
		}

		return accumulatedConservationEthic / agents.size();
	}

	/**
	 * Returns agents state list
	 * 
	 * @return
	 */
	public AgentStateList getAsl() {
		return asl;
	}

	/**
	 * Sets agents state list
	 * 
	 * @param asl
	 */
	public void setAsl(AgentStateList asl) {
		this.asl = asl;
	}
}
