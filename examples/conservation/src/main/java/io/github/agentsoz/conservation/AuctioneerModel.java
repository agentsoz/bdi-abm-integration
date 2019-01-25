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

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.ActionContent.State;
import io.github.agentsoz.bdiabm.data.PerceptContent;
import io.github.agentsoz.bdiabm.v2.AgentDataContainer;
import io.github.agentsoz.bdigams.GAMSModel;
import io.github.agentsoz.conservation.jill.agents.Landholder;
import io.github.agentsoz.conservation.outputwriters.ConstantFileNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

/**
 * Provide access to the {@link GAMSModel} and preforms auctioneer's actions.
 * 
 * @author Sewwandi Perera
 */
public final class AuctioneerModel extends GAMSModel {

    final private Logger logger = LoggerFactory.getLogger(Main.LOGGER_NAME);

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
		NOT_STARTED, VISIT_BY_EXTENSION_OFFICER, CALL_FOR_BIDS_SEND, RESULTS_SENT, ENDED,
	}

	/**
	 * Input CSV file for GAMS, used to send bids to the GAMS system
	 */
	private String gamsInputFile = null;

	String[] agentIds;

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
	 * {@inheritDoc}
	 */
	@Override
	public AgentDataContainer takeControl(double time, AgentDataContainer agentDataContainer) {
		switch (auctionState) {
		case NOT_STARTED:
			auctionState = AuctionState.VISIT_BY_EXTENSION_OFFICER;
			break;
		case VISIT_BY_EXTENSION_OFFICER:
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
		return agentDataContainer;
	}

	/**
	 * Start auction by sending the packages to land holders, when all agents
	 * finished bidding send the bids to the {@link GAMSModel}, send back the
	 * results to land holders.
	 */
	public void conductAuction(LandholderModel landholderModel) {
		double time = 0;
		auctionState = AuctionState.NOT_STARTED;
		
		// Handle any extension officer visits (added externally) first
		// Step time
		landholderModel.takeControl(time, landholderModel.getAgentDataContainer());
		this.takeControl(time, this.getAgentDataContainer());
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			throw new RuntimeException("ERROR while waiting for GAMS to finish stepping", e);
		}
		
		// Send call for bids
		{
			for(String agentId : agentIds) {
				this.getAgentDataContainer().putPercept(agentId,
						Global.percepts.CALL_FOR_BIDS.toString(),
						new PerceptContent(Global.percepts.CALL_FOR_BIDS.toString(), Main.packages));
			}
		}
		// Step time
		time++;
		landholderModel.takeControl(time, landholderModel.getAgentDataContainer());
		this.takeControl(time, this.getAgentDataContainer());
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			throw new RuntimeException("ERROR while waiting for GAMS to finish stepping", e);
		}

		// Process the bids (as well as set the BID actions to PASSED)
		ArrayList<String> bids = new ArrayList<String>();
		{
			Iterator<String> it = this.getAgentDataContainer().getAgentIdIterator();
			while (it.hasNext()) {
				String agentId = it.next();
				Map<String, ActionContent> actions = this.getAgentDataContainer().getAllActionsCopy(agentId);
				for (String actionId : actions.keySet()) {
					ActionContent acc = actions.get(Global.actions.BID.toString());
					if (acc != null) {
						for (Object object : acc.getParameters()) {
							Bid bid = (Bid) object;
							String agentName = ((Landholder) landholderModel
									.getLandholder(Integer.parseInt(agentId))).gamsID();
							bids.add(String.format("%d,%s,%s,%011.9f", bid.id,
									agentName, Main.packages[bid.id - 1].description,
									bid.price));
						}
						acc.setState(State.PASSED);
					}
				}
			}
		}

		Object[] inputs = new Object[2];

		if (bids.isEmpty()) {
			logger.warn("No bids made for the auction round ");
			latestResultSet = new AuctionResultSet(null);
		} else {
			// Call GAMS with the input
			runGAMSModel(bids);

			latestResultSet = new AuctionResultSet(Main.GAMSOutputFile());
		}

		inputs[0] = latestResultSet;
		inputs[1] = this.getAverageConservationEthic();

		// Send auction results
		{
			Iterator<String> it = this.getAgentDataContainer().getAgentIdIterator();
			while (it.hasNext()) {
				String agentId = it.next();
				this.getAgentDataContainer().putPercept(agentId,
						Global.percepts.AUCTION_RESULTS.toString(),
						new PerceptContent(Global.percepts.AUCTION_RESULTS.toString(), inputs));
			}
		}
		// Step time
		time++;
		landholderModel.takeControl(time, landholderModel.getAgentDataContainer());
		this.takeControl(time, this.getAgentDataContainer());

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
		logger.debug("Running GAMS with input:\n" + bidstr);
		writeGAMSInputFile(bidstr);

		// Run GAMS here
		ArrayList<String> output = new ArrayList<String>();
		HashMap<String, String> opts = new HashMap<String, String>();
		opts.put("target_table", ConstantFileNames.getTargetFileName());
		opts.put("csvInputFile", Main.GAMSInputFile());
		opts.put("csvOutputFile", Main.GAMSOutputFile());
		opts.put("numPackages", Integer.toString(ConservationUtils.getNumberOfPackages()));
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

	public void writeGAMSInputFile(String s) {
		try {
			PrintWriter csv = new PrintWriter(new FileWriter(gamsInputFile, false), true);
			csv.println(s);
			csv.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setGAMSInputFile(String file) {
		gamsInputFile = file;
	}

	@Override
	public void init(Object[] args) {
		agentIds = (String[]) args[0];
	}
}
