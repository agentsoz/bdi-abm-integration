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
import io.github.agentsoz.conservation.jill.goals.UpdateConservationEthicGoal;
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
 * Land holder updates his C according to the profit he has gained.
 * 
 * In some situations, land holder selects multiple bids and he wins more than
 * one of them. In this kind of scenarios, only the mostly profitable successful
 * bid is used for calculations.
 * 
 * If the profit is below the threshold defined in
 * ConservationUtils.profitThresholdForSuccessfulLowC, the land holder's C is
 * decreased slightly using the factor
 * ConservationUtils.decreaseFactorForCWhenSuccessAndLowC. This decrease is not
 * proportional to the profit he gained. Else if land holder's C is greater than
 * the threshold, his C is increased proportional to the profit he gained using
 * the factor ConservationUtils.increaseFactorForCWhenSuccessAndLowC.
 * 
 * @author Sewwandi Perera
 */
public class SuccessfulLowC extends Plan {
	
    final private Logger logger = LoggerFactory.getLogger(Main.LOGGER_NAME);

	Landholder landholder;
	UpdateConservationEthicGoal updateConservationEthicGoal;

	public SuccessfulLowC(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		landholder = (Landholder) getAgent();
		updateConservationEthicGoal = (UpdateConservationEthicGoal) getGoal();
		body = steps;
	}

	@Override
	public boolean context() {
		return landholder.getCurrentAuctionRound().isWon()
				&& !landholder.isConservationEthicHigh()
				&& landholder.getCurrentAuctionRound().isParticipated();
	}

	@Override
	public void setPlanVariables(HashMap<String, Object> vars) {

	}

	PlanStep[] steps = { new PlanStep() {
		public void step() {
			ArrayList<BidResult> bids = updateConservationEthicGoal
					.getMyResults().getMyBids();

			// This is used only for logging purposes
			ArrayList<Double> profits = new ArrayList<Double>();

			if (null != bids) {
				double highestProfit = 0;

				for (int i = 0; i < bids.size(); i++) {
					BidResult bid = (BidResult) bids.get(i);

					if (bid.isWon()) {
						// If the and holder has won
						double tempProfit = ((bid.getBidPrice() - bid
								.getOpportunityCost()) / bid
								.getOpportunityCost()) * 100;
						profits.add(tempProfit);
						if (highestProfit < tempProfit) {
							highestProfit = tempProfit;
						}
					}
				}

				Collections.sort(profits);
				logger.debug(landholder.logprefix()
						+ "- highest profit which changes agents C is "
						+ highestProfit + ", all profits:" + profits);

				double currentC = landholder.getConservationEthicBarometer();
				double newC;
				double[] medProfitPercentageRange = ConservationUtils
						.getMediumProfitPercentageRange();

				if (highestProfit > 0
						&& highestProfit <= medProfitPercentageRange[1]) {
					logger.debug(landholder.logprefix()
							+ " Decrease agent's C since his highest profit ("
							+ highestProfit
							+ "%) is greater than 0 and less than/equal the upper margin of medium profit percentage range ("
							+ medProfitPercentageRange[1] + ")");

					newC = currentC
							* (1 - Math.abs(highestProfit / 100)
									* ConservationUtils
											.getConservationEthicModifier());
					updateConsrvationEthicBarometer(newC, currentC);
				} else if (highestProfit > medProfitPercentageRange[1]) {
					logger.debug(landholder.logprefix()
							+ " Increase agent's C since his highest profit ("
							+ highestProfit
							+ "%) is greater than the upper margin of medium profit percentage range ("
							+ medProfitPercentageRange[1] + ")");

					newC = currentC
							* (1 + Math.abs(highestProfit / 100)
									* ConservationUtils
											.getConservationEthicModifier());
					updateConsrvationEthicBarometer(newC, currentC);
				}
			}
		}
	} };

	public void updateConsrvationEthicBarometer(double newC, double currentC) {
		newC = landholder.setConservationEthicBarometer(newC);
		landholder.setConservationEthicHigh(landholder
				.isConservationEthicHigh(newC));
		String newStatus = (landholder.isConservationEthicHigh()) ? "high"
				: "low";
		logger.debug(String.format("%supdated CE %.1f=>%.1f, which is %s"
				,landholder.logprefix(), currentC, newC, newStatus));
	}
}
