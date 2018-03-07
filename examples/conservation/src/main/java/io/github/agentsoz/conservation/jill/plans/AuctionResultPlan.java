package io.github.agentsoz.conservation.jill.plans;

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
import io.github.agentsoz.conservation.LandholderHistory.BidResult;
import io.github.agentsoz.conservation.jill.agents.Landholder;
import io.github.agentsoz.conservation.jill.goals.AuctionResultGoal;
import io.github.agentsoz.conservation.jill.goals.SocialNormUpdateGoal;
import io.github.agentsoz.conservation.jill.goals.UpdateConservationEthicGoal;
import io.github.agentsoz.conservation.jill.goals.UpdateProfitMotivationGoal;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanInfo;
import io.github.agentsoz.jill.lang.PlanStep;

import java.util.ArrayList;
import java.util.Map;

/**
 * This plan is executed when the "AuctionResultGoal" event is triggered by the
 * agent. This is responsible for update part of the project.
 * 
 * @author Sewwandi Perera
 */
@PlanInfo(postsGoals = {
		"io.github.agentsoz.conservation.jill.goals.UpdateConservationEthicGoal",
		"io.github.agentsoz.conservation.jill.goals.UpdateProfitMotivationGoal",
		"io.github.agentsoz.conservation.jill.goals.SocialNormUpdateGoal" })
public class AuctionResultPlan extends Plan {

	Landholder landholder;
	AuctionResultGoal auctionResultGoal;
	boolean agentInitiallyHadHighCE;
	boolean agentInitiallyHadHighPM;

	public AuctionResultPlan(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		landholder = (Landholder) getAgent();
		auctionResultGoal = (AuctionResultGoal) getGoal();
		body = steps;
	}

	@Override
	public boolean context() {
		return true;
	}

	@Override
	public void setPlanVariables(Map<String, Object> vars) {

	}

	PlanStep[] steps = { new PlanStep() {
		public void step() {
			// In the first plan step, post the goal
			// UpdateConservationEthicGoal. Before posting the goal, land
			// holder's current category of PM and CE are recorded for future
			// references.
			agentInitiallyHadHighCE = landholder.isConservationEthicHigh();
			agentInitiallyHadHighPM = landholder.isProfitMotivationHigh();

			post(new UpdateConservationEthicGoal("update-ce",
					auctionResultGoal.getAuctionRound()));
		}
	}, new PlanStep() {
		public void step() {
			// In the second plan stap, post the goal
			// "UpdateProfitMotivationGoal".
			post(new UpdateProfitMotivationGoal("update-pm",
					auctionResultGoal.getAuctionRound()));
		}
	}, new PlanStep() {
		public void step() {
			// In the third plan step, post the goal "SocialNormUpdateGoal"
			post(new SocialNormUpdateGoal("social-norm",
					auctionResultGoal.getAverageConservationEthic()));
		}
	}, new PlanStep() {
		public void step() {

			// Record if agents moved to a different category. This information
			// is used only to record as outputs.
			if (agentInitiallyHadHighCE
					&& !landholder.isConservationEthicHigh()) {
				landholder
						.setMoveCEcategory(ConservationUtils.CategoryChanges.DOWN);
			} else if (!agentInitiallyHadHighCE
					&& landholder.isConservationEthicHigh()) {
				landholder
						.setMoveCEcategory(ConservationUtils.CategoryChanges.UP);
			} else {
				landholder
						.setMoveCEcategory(ConservationUtils.CategoryChanges.NONE);
			}

			if (agentInitiallyHadHighPM && !landholder.isProfitMotivationHigh()) {
				landholder
						.setMovePMcategory(ConservationUtils.CategoryChanges.DOWN);
			} else if (!agentInitiallyHadHighPM
					&& landholder.isProfitMotivationHigh()) {
				landholder
						.setMovePMcategory(ConservationUtils.CategoryChanges.UP);
			} else {
				landholder
						.setMovePMcategory(ConservationUtils.CategoryChanges.NONE);
			}

			// Record category of winning bids (High profit winning bid, medium
			// profit winning bids, low profit winning bid)
			landholder.setHighProfitWinningBids(0);
			landholder.setMedProfitWinningBids(0);
			landholder.setLowProfitWinningBids(0);

			ArrayList<BidResult> bids = landholder.getCurrentAuctionRound()
					.getMyBids();

			if (null != bids) {
				for (int i = 0; i < bids.size(); i++) {
					BidResult bid = (BidResult) bids.get(i);

					if (bid.isWon()) {
						// If the and holder has won
						double profitPercentage = ((bid.getBidPrice() - bid
								.getOpportunityCost()) / bid
								.getOpportunityCost()) * 100;
						if (profitPercentage <= ConservationUtils
								.getLowProfitPercentageRange()[1]) {
							landholder.increaseLowProfitWinningBids();
						} else if (profitPercentage <= ConservationUtils
								.getMediumProfitPercentageRange()[1]) {
							landholder.increaseMedProfitWinningBids();
						} else {
							landholder.increaseHighProfitWinningBids();
						}
					}
				}
			}

		}
	} };

}
