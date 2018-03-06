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
import io.github.agentsoz.conservation.Main;
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
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.agentsoz.abmjill.genact.EnvironmentAction;

/**
 * This plan is executed for the agents who have Low C and Low P. The plan
 * selects random bids with random profit.
 * 
 * @author Sewwandi Perera
 */
@PlanInfo(postsGoals = { "io.github.agentsoz.abmjill.genact.EnvironmentAction" })
public class DecideBidsWhenLowCLowPPlan extends Plan {
	
    final private Logger logger = LoggerFactory.getLogger(Main.LOGGER_NAME);

	Landholder landholder;
	DecideBidsGoal decideBidsGoal;

	public DecideBidsWhenLowCLowPPlan(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		landholder = (Landholder) getAgent();
		decideBidsGoal = (DecideBidsGoal) getGoal();
		body = steps;
	}

	@Override
	public boolean context() {
		return !((Landholder) getAgent()).isConservationEthicHigh()
				&& !((Landholder) getAgent()).isProfitMotivationHigh();
	}

	@Override
	public void setPlanVariables(Map<String, Object> vars) {
	}

	PlanStep[] steps = { new PlanStep() {
		public void step() {
			// Landholder selects bids randomly
			ArrayList<Bid> selectedBids = new ArrayList<Bid>();
			Package[] packages = decideBidsGoal.getPackages();
			
			while(selectedBids.size() == 0){
				double probability = ConservationUtils.getGlobalRandom()
						.nextDouble();

				for (int i = 0; i < packages.length; i++) {
					double random = ConservationUtils.getGlobalRandom()
							.nextDouble();
					if (random < probability) {
						// bid for the package with a random profit
						double profitPercentage = ConservationUtils
								.getGlobalRandom().nextDouble() * 100;
						double bidPrice = packages[i].opportunityCost
								* (1 + profitPercentage / 100);

						Bid bid = new Bid(packages[i].id, bidPrice);
						logger.debug(landholder.logprefix()
								+ " profit%:" + String.format("%.1f",profitPercentage)
								+ " prepared bid:" + bid
								+ " opportunity cost:" + packages[i].opportunityCost
								);
						selectedBids.add(bid);
					}
				}
			}
			
			// Update agent's progress info
			AgentsProgressWriter.getInstance().addAgentsInfo(
					landholder.getName(), "A");

			// write bids to the output file
			BidsWriter.getInstance().writeBids(landholder.gamsID(),
					selectedBids);

			logger.debug(landholder.logprefix() 
					+ "(gams id:" + landholder.gamsID() + ")"
					+ " submitted #bids:" + selectedBids.size()
					+ " and waiting");

			// post the bids and wait for a response
			post(new EnvironmentAction(landholder.getName(),
					Global.actions.BID.toString(),
					(Object[]) selectedBids.toArray()));

			// Evaluate the response
			logger.debug(landholder.logprefix() + "finished action BID.");
		}
	} };

}
