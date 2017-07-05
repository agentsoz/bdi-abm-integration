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
import io.github.agentsoz.conservation.jill.agents.Landholder;
import io.github.agentsoz.conservation.jill.goals.SocialNormUpdateGoal;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanStep;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * All landholders executes this plan to adjust their conservation ethic
 * barometer(C) according to social norm. Calculations are done as follow;
 * Firstly, the average C of the population is calculated and identified where
 * it lies in the sigmoid function. Here the sigmoid function is shifted to the
 * range of C by modifying it as below;
 * 
 * f(c) = = 1/(1+e^(-1*(c-maxC/2))
 * 
 * If all landholders average C is greater than
 * ConservationUtils.getSocialNormThreshold()
 * 
 * @author Sewwandi Perera
 */
public class SocialNormUpdatePlan extends Plan {
	
    final private Logger logger = LoggerFactory.getLogger(Main.LOGGER_NAME);

	Landholder landholder;
	SocialNormUpdateGoal socialNormUpdateGoal;

	public SocialNormUpdatePlan(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		landholder = (Landholder) getAgent();
		socialNormUpdateGoal = (SocialNormUpdateGoal) getGoal();
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
			double averageC = socialNormUpdateGoal
					.getAverageConservationEthic();
			double myC = landholder.getConservationEthicBarometer();
			double newC;

			if (averageC > myC) {
				//newC = myC + (averageC - myC)
				//		* ConservationUtils.getSocialNormUpdatePercentage()
				//		/ 100;
				double deltaX = (averageC/100) * ConservationUtils.getSigmoidMaxStepX();
				double oldX = ConservationUtils.sigmoid_normalised_100_inverse(myC/100);
				double newX = (oldX + deltaX >= 100) ? 100.0 : oldX + deltaX;
				newC = 100*ConservationUtils.sigmoid_normalised_100(newX);
				logger.debug(landholder.logprefix()
						+ "CE increased as average social norm ("
						+ String.format("%.1f", averageC)
						+ ") is greater than agent's CE ("
						+ String.format("%.1f", myC)
						+ ")");
				updateConsrvationEthicBarometer(newC, myC);
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
