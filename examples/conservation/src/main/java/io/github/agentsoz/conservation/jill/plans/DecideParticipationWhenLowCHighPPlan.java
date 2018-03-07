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
import io.github.agentsoz.conservation.LandholderHistory;
import io.github.agentsoz.conservation.Main;
import io.github.agentsoz.conservation.LandholderHistory.BidResult;
import io.github.agentsoz.conservation.jill.agents.Landholder;
import io.github.agentsoz.conservation.outputwriters.AgentsProgressWriter;
import io.github.agentsoz.conservation.outputwriters.LowCHighPStatistics;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanStep;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This plan is executed for land owners who have Low C and High P. Probability
 * of participation of any land owner who comes to this plan is calculated using
 * below equation. probability = size of profit opportunity * chance to win size
 * of opportunity cost represents the maximum bid a land holder can make size of
 * profit opportunity: referencePackage.opportunityCost * (1 +
 * highProfitPercentage) chance to win: is the average number of successes in
 * last three rounds.
 * 
 * @author Sewwandi Perera
 */
public class DecideParticipationWhenLowCHighPPlan extends Plan {

    final private Logger logger = LoggerFactory.getLogger(Main.LOGGER_NAME);

	Landholder landholder;

	public DecideParticipationWhenLowCHighPPlan(Agent agent, Goal goal,
			String name) {
		super(agent, goal, name);
		landholder = (Landholder) getAgent();
		body = steps;
	}

	@Override
	public boolean context() {
		return !((Landholder) getAgent()).isConservationEthicHigh()
				&& ((Landholder) getAgent()).isProfitMotivationHigh();
	}

	@Override
	public void setPlanVariables(Map<String, Object> vars) {

	}

	PlanStep[] steps = { new PlanStep() {
		public void step() {
			int totalNumberOfWinnings = 0;
			int totalNumberOfGoodWinnings = 0;

			double goodProfit = ConservationUtils.getReferencePackage().opportunityCost
					* (ConservationUtils.getHighProfitPercentageRange()[0] / 100);

			LandholderHistory history = landholder.getHistory();
			for (int i = history.getNumberOfAuctionRounds() - 1; i > history
					.getNumberOfAuctionRounds() - 4; i--) {
				if (i == -1) {
					break;
				}
				totalNumberOfWinnings += history.getAuctionRound(i)
						.getNumberOfWinningsByPopulation();

				for (BidResult winResult : history.getAuctionRound(i)
						.getWinnersInfo()) {
					if ((winResult.getBidPrice() - winResult
							.getOpportunityCost()) >= goodProfit) {
						totalNumberOfGoodWinnings++;
					}
				}
			}

			double probability;
			if (totalNumberOfWinnings != 0) {
				probability = 1.0 * (totalNumberOfGoodWinnings)
						/ (totalNumberOfWinnings);
			} else if (history.getNumberOfAuctionRounds() == 0) {
				probability = ConservationUtils
						.getLowParticipationProbability();
			} else {
				probability = 0;
			}

			if (ConservationUtils.getGlobalRandom().nextDouble() < probability) {
				landholder.setDecisionOnParticipation(true);
				LowCHighPStatistics.getInstance().printEntry(
						landholder.getName(), probability,
						totalNumberOfWinnings, totalNumberOfGoodWinnings,
						goodProfit, "yes");
			} else {
				landholder.setDecisionOnParticipation(false);
				AgentsProgressWriter.getInstance().addAgentsInfo(
						landholder.getName(), "C-np");
				LowCHighPStatistics.getInstance().printEntry(
						landholder.getName(), probability,
						totalNumberOfWinnings, totalNumberOfGoodWinnings,
						goodProfit, "no");
			}

			logger.debug(landholder.logprefix()
					+ "reference package:" + ConservationUtils.getReferencePackage()
					+ " totalSuccessBids:" + totalNumberOfWinnings
					+ " withGoodProfit:" + totalNumberOfGoodWinnings
					+ " goodProfit:" + goodProfit 
					+ " participation probability:" + probability
					+ " will participate?:"
					+ landholder.getDecisionOnParticipation()
					);
		}
	} };
}
