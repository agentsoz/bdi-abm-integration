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
import io.github.agentsoz.conservation.Main;
import io.github.agentsoz.conservation.LandholderHistory.BidResult;
import io.github.agentsoz.conservation.jill.agents.Landholder;
import io.github.agentsoz.conservation.jill.goals.UpdateProfitMotivationGoal;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanStep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * After receiving the results of the auction, every land holder updates his
 * profit motive barometer. This plan is used by all land holders who have high
 * profit motivation.
 * 
 * In this plan, it is not considered whether the land holder has participated
 * or successful in the auction. Instead, he updates his profit motive barometer
 * proportional to the highest profit obtained by any winner. The factor
 * ConservationUtils.increaseFactorForPWhenHighP is used to increase land
 * holder's profit motive barometer.
 * 
 * @author Sewwandi Perera
 */
public class HighP extends Plan {
	
    final private Logger logger = LoggerFactory.getLogger(Main.LOGGER_NAME);

	Landholder landholder;
	UpdateProfitMotivationGoal updateProfitMotivationGoal;

	public HighP(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		landholder = (Landholder) getAgent();
		updateProfitMotivationGoal = (UpdateProfitMotivationGoal) getGoal();
		body = steps;
	}

	@Override
	public boolean context() {
		return landholder.isProfitMotivationHigh();
	}

	@Override
	public void setPlanVariables(HashMap<String, Object> vars) {

	}

	PlanStep[] steps = { new PlanStep() {
		public void step() {
			ArrayList<BidResult> winningBids = updateProfitMotivationGoal
					.getMyResults().getWinnersInfo();

			// used only for logging
			ArrayList<Double> profits = new ArrayList<Double>();

			if (winningBids != null && !winningBids.isEmpty()) {
				double highestProfit = 0;
				double winningPrice = 0;

				for (int i = 0; i < winningBids.size(); i++) {
					BidResult bid = (BidResult) winningBids.get(i);
					winningPrice = bid.getBidPrice();

					if (winningPrice > 0) { // If somebody has won the package
						double tempProfit = (winningPrice - bid
								.getOpportunityCost())
								/ bid.getOpportunityCost();
						profits.add(tempProfit);

						if (highestProfit < tempProfit) {
							highestProfit = tempProfit;
						}
					}
				}

				Collections.sort(profits);
				logger.debug(landholder.logprefix()
						+ "all profits:" + profits + ", highest:" 
						+ highestProfit);

				// agent’s P = P * (1 + |profit| * agentAttributeModifier);
				double currentP = landholder.getProfitMotiveBarometer();
				//double newP = currentP
				//		* (1 + Math.abs(highestProfit)
				//				* ConservationUtils
				//						.getProfitMotivationModifier());
				double deltaX = ConservationUtils.getSigmoidMaxStepX();
				double oldX = ConservationUtils.sigmoid_normalised_100_inverse(currentP/100);
				double newX = (oldX + deltaX >= 100) ? 100.0 : oldX + deltaX;
				double newP = 100*ConservationUtils.sigmoid_normalised_100(newX);

				// Finally, update land holder's P and recalculate whether his P
				// is high or low.
				newP = landholder.setProfitMotiveBarometer(newP);
				landholder.setProfitMotivationHigh(landholder
						.isProfitMotivationHigh(newP));
				String newStatus = (landholder.isProfitMotivationHigh()) ? "high"
						: "low";
				logger.debug(String.format("%supdated PM %.1f=>%.1f, which is %s"
						,landholder.logprefix(), currentP, newP, newStatus));
			}
		}
	} };

}
