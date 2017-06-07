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

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.agentsoz.abmjill.genact.EnvironmentAction;
import io.github.agentsoz.conservation.Bid;
import io.github.agentsoz.conservation.Global;
import io.github.agentsoz.conservation.Main;
import io.github.agentsoz.conservation.jill.agents.Landholder;
import io.github.agentsoz.conservation.jill.goals.CallForBidsGoal;
import io.github.agentsoz.conservation.jill.goals.DecideBidsGoal;
import io.github.agentsoz.conservation.jill.goals.DecideParticipationGoal;
import io.github.agentsoz.conservation.outputwriters.AuctionStatisticsWriter;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanInfo;
import io.github.agentsoz.jill.lang.PlanStep;

/**
 * This plan handles CALL_FOR_BIDS action. Initially checks landholder's
 * decision on participating in auction. If he decides to participate, selects
 * bids and sends them back to auctioneer. If the landholder decides to not to
 * participate, sends the update back to auctioneer with no bids.
 * 
 * @author Sewwandi Perera
 */
@PlanInfo(postsGoals = { "io.github.agentsoz.conservation.jill.goals.DecideBidsGoal",
		"io.github.agentsoz.conservation.jill.goals.DecideParticipationGoal",
		"io.github.agentsoz.abmjill.genact.EnvironmentAction" })
public class CallForBidsPlan extends Plan {

    final private Logger logger = LoggerFactory.getLogger(Main.LOGGER_NAME);

	Landholder landholder;
	CallForBidsGoal callForBidsGoal;

	public CallForBidsPlan(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		landholder = (Landholder) agent;
		callForBidsGoal = (CallForBidsGoal) goal;
		body = steps;
	}

	@Override
	public boolean context() {
		return true;
	}

	@Override
	public void setPlanVariables(HashMap<String, Object> vars) {

	}

	PlanStep[] steps = { new PlanStep() {
		public void step() {
			// In the first plan step, decide whether to participate in the
			// auction by calling the "DecideParticipationGoal" goal
			DecideParticipationGoal decideParticipationGoal = new DecideParticipationGoal(
					"decide-participation");
			decideParticipationGoal.setPackages(callForBidsGoal.getPackages());
			post(decideParticipationGoal);
		}
	}, new PlanStep() {
		public void step() {
			// In the second plan step calculate the bids based on the decision
			// made at 1st plan step
			Landholder landholder = (Landholder) getAgent();
			CallForBidsGoal callForBidsGoal = (CallForBidsGoal) getGoal();

			if (landholder.getDecisionOnParticipation()) {
				// Reset the parameter
				landholder.setDecisionOnParticipation(false);

				AuctionStatisticsWriter.getInstance().addParticipant(
						landholder.isConservationEthicHigh(),
						landholder.isProfitMotivationHigh());

				DecideBidsGoal decideBidsGoal = new DecideBidsGoal(
						"decide-bids");
				decideBidsGoal.setPackages(callForBidsGoal.getPackages());

				post(decideBidsGoal);
			} else {
				// when decided not to participate in auction, reply the event
				// with 0 bids
				post(new EnvironmentAction(
						Integer.toString(landholder.getId()),
						Global.actions.BID.toString(), new Bid[0]));

				// Evaluate the response
				logger.debug(landholder.logprefix() + "finished action BID.");
			}
		}
	} };

}
