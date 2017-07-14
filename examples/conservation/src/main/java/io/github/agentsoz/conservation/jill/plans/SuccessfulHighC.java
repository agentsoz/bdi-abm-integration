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
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * When successful, if Land holder's profit is low to medium, then CE decreases
 * proportional to the profit, else CE increases proportional to the profit.
 * 
 * @author Sewwandi Perera
 */
public class SuccessfulHighC extends Plan {

    final private Logger logger = LoggerFactory.getLogger(Main.LOGGER_NAME);

    Landholder landholder;
	UpdateConservationEthicGoal updateConservationEthicGoal;

	public SuccessfulHighC(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		landholder = (Landholder) getAgent();
		updateConservationEthicGoal = (UpdateConservationEthicGoal) getGoal();
	}

	@Override
	public boolean context() {
		return landholder.getCurrentAuctionRound().isWon()
				&& landholder.isConservationEthicHigh()
				&& landholder.getCurrentAuctionRound().isParticipated();
	}

	@Override
	public void setPlanVariables(HashMap<String, Object> vars) {

	}

	PlanStep[] steps = { new PlanStep() {
		public void step() {
			ArrayList<BidResult> bids = updateConservationEthicGoal
					.getMyResults().getMyBids();
			double highestProfit = landholder.getHighestProfitPercent(bids);
			if (Double.isNaN(highestProfit)) {
				logger.debug(landholder.logprefix() + "no winning bids");
				return;
			}
			double currentC = landholder.getConservationEthicBarometer();
			double newC;
			double deltaX = (highestProfit/100) * ConservationUtils.getSigmoidMaxStepX();
			double oldX = ConservationUtils.sigmoid_normalised_100_inverse(currentC/100);
			
			double[] medProfitPercentageRange = ConservationUtils
					.getMediumProfitPercentageRange();

			if (highestProfit > 0 && highestProfit <= medProfitPercentageRange[1]) {
				double newX = (oldX <= deltaX) ? 0.0 : oldX - deltaX;
				newC = 100*ConservationUtils.sigmoid_normalised_100(newX);

				updateConservationEthicBarometer(newC, currentC);
				logger.debug(landholder.logprefix()
						+ "CE decreased as highest profit% ("
						+ String.format("%.1f", highestProfit)
						+ ") is greater than 0 and less than/equal the upper margin of medium profit% range ("
						+ medProfitPercentageRange[1] + ")");
			} else if (highestProfit > medProfitPercentageRange[1]) {
				double newX = (oldX + deltaX >= 100) ? 100.0 : oldX + deltaX;
				newC = 100*ConservationUtils.sigmoid_normalised_100(newX);
				
				updateConservationEthicBarometer(newC, currentC);
				logger.debug(landholder.logprefix()
						+ "CE increased as highest profit% ("
						+ String.format("%.1f", highestProfit)
						+ ") is greater than the upper margin of medium profit% range ("
						+ medProfitPercentageRange[1] + ")");
			}
		}
	} };

	public void updateConservationEthicBarometer(double newC, double currentC) {
		newC = landholder.setConservationEthicBarometer(newC);
		String newStatus = (landholder.isConservationEthicHigh()) ? "high" : "low";
		logger.debug(String.format("%supdated CE %.1f=>%.1f, which is %s"
				,landholder.logprefix(), currentC, newC, newStatus));

	}
}
