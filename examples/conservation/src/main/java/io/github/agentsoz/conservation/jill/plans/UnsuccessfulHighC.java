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
 * Landholder's CE is decreased proportional to the profit obtained by
 * the winner.
 * 
 * In some situations, land holder has made multiple bids and he is unsuccessful
 * in all of them. Some of they may have won by other land holders, and some of
 * them may have not won by any. It was assumed that, if no one has won, there
 * is no effect to the land holder's Conservation Ethic. So, the unsuccessful
 * bid, which has given the highest profit to the winner is used for the
 * calculation.
 * 
 * @author Sewwandi Perera
 * 
 */
public class UnsuccessfulHighC extends Plan {
	
    final private Logger logger = LoggerFactory.getLogger(Main.LOGGER_NAME);

	Landholder landholder;
	UpdateConservationEthicGoal updateConservationEthicGoal;

	public UnsuccessfulHighC(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		landholder = (Landholder) getAgent();
		updateConservationEthicGoal = (UpdateConservationEthicGoal) getGoal();
		body = steps;
	}

	@Override
	public boolean context() {
		return landholder.getCurrentAuctionRound().isParticipated()
				&& !(landholder.getCurrentAuctionRound().isWon())
				&& landholder.isConservationEthicHigh();
	}

	@Override
	public void setPlanVariables(HashMap<String, Object> vars) {

	}

	PlanStep[] steps = { new PlanStep() {
		public void step() {
			ArrayList<BidResult> winningBids = updateConservationEthicGoal.getMyResults().getWinnersInfo();
			double highestProfit = landholder.getHighestProfitPercent(winningBids);
			if (Double.isNaN(highestProfit)) {
				logger.debug(landholder.logprefix() + "no winning bids");
				return;
			}
			double currentC = landholder.getConservationEthicBarometer();
			double deltaX = (highestProfit/100) * ConservationUtils.getSigmoidMaxStepX();
			double oldX = ConservationUtils.sigmoid_normalised_100_inverse(currentC/100);
			double newX = (oldX <= deltaX) ? 0.0 : oldX - deltaX;
			double newC = 100*ConservationUtils.sigmoid_normalised_100(newX);
			newC = landholder.setConservationEthicBarometer(newC);
			String newStatus = (landholder.isConservationEthicHigh()) ? "high" : "low";
			logger.debug(String.format("%supdated CE %.1f=>%.1f, which is %s"
					,landholder.logprefix(), currentC, newC, newStatus));
		}
	} };
}
