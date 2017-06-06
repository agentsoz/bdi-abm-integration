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

import io.github.agentsoz.conservation.Bid;
import io.github.agentsoz.conservation.ConservationUtils;
import io.github.agentsoz.conservation.Global;
import io.github.agentsoz.conservation.Log;
import io.github.agentsoz.conservation.Package;
import io.github.agentsoz.conservation.jill.agents.Landholder;
import io.github.agentsoz.conservation.jill.goals.DecideBidsGoal;
import io.github.agentsoz.conservation.outputwriters.AgentsProgressWriter;
import io.github.agentsoz.conservation.outputwriters.BidsWriter;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanInfo;
import io.github.agentsoz.jill.lang.PlanStep;

import java.util.ArrayList;
import java.util.HashMap;

import io.github.agentsoz.abmjill.genact.EnvironmentAction;

/**
 * This plan is executed for the agents who have Low C and High P. The plan
 * selects bids which can make at least referenceBidProfit amount of profit when
 * using a profit percentage within the highProfitPercentageRange.
 * 
 * @author Sewwandi Perera
 */
@PlanInfo(postsGoals = { "io.github.agentsoz.abmjill.genact.EnvironmentAction" })
public class DecideBidsWhenLowCHighPPlan extends Plan {
	Landholder landholder;
	DecideBidsGoal decideBidsGoal;

	public DecideBidsWhenLowCHighPPlan(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		landholder = (Landholder) getAgent();
		decideBidsGoal = (DecideBidsGoal) getGoal();
		body = steps;
	}

	@Override
	public boolean context() {
		return !((Landholder) getAgent()).isConservationEthicHigh()
				&& ((Landholder) getAgent()).isProfitMotivationHigh();
	}

	@Override
	public void setPlanVariables(HashMap<String, Object> vars) {

	}

	PlanStep[] steps = { new PlanStep() {
		public void step() {
			Package[] data = decideBidsGoal.getPackages();
			ArrayList<Bid> selectedBids = new ArrayList<Bid>();

			double referenceProfit = ConservationUtils.getReferenceBidProfit();
			double[] highProfitPercentageRange = ConservationUtils
					.getHighProfitPercentageRange();

			for (int i = 0; i < data.length; i++) {
				/*
				 * Decide a profit percentage within highProfitPercentageRange
				 * to calculate the profit given to each package
				 */
				double profitPercentage = highProfitPercentageRange[0]
						+ (highProfitPercentageRange[1] - highProfitPercentageRange[0])
						* ConservationUtils.getGlobalRandom().nextDouble();

				Log.debug("Agent " + landholder.getName()
						+ " High profit percentage range:["
						+ highProfitPercentageRange[0] + ","
						+ highProfitPercentageRange[1]
						+ "], profit percentage:" + profitPercentage
						+ ", reference profit:" + referenceProfit);

				double profit = data[i].opportunityCost * profitPercentage
						/ 100;
				/*
				 * checks if the profit is greater than or equal to the
				 * referenceBidProfit
				 */
				if (profit >= referenceProfit) {
					double bidPrice = profit + data[i].opportunityCost;
					Bid bid = new Bid(data[i].id, bidPrice);
					Log.debug("Agent " + landholder.getName() + "(gams id:"
							+ landholder.gamsID() + ") made a bid " + bid
							+ ",opportunity cost:" + data[i].opportunityCost
							+ ",profit percentage" + profitPercentage);
					selectedBids.add(bid);
				}
			}

			Log.debug("Agent " + landholder.getName() + "(gams id:"
					+ landholder.gamsID() + ") made " + selectedBids.size()
					+ " bids.");

			// Update agent's progress info
			AgentsProgressWriter.getInstance().addAgentsInfo(
					landholder.getName(), "C");

			// write bids to the output file
			BidsWriter.getInstance().writeBids(landholder.gamsID(),
					selectedBids);

			// post the bids and wait for a response
			post(new EnvironmentAction(landholder.getName(),
					Global.actions.BID.toString(),
					(Object[]) selectedBids.toArray()));

			// Evaluate the response
			Log.debug("Agent " + landholder.getName() + " finished action BID.");
		}
	} };

}
