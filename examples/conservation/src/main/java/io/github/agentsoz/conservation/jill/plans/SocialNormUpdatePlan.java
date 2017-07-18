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
 * All landholders executes this plan to adjust their CE if it is below the 
 * average CE of the population (the social norm).
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
				double oldX = ConservationUtils.sigmoid_normalised_100_inverse(myC/100);
				double deltaX = ConservationUtils.sigmoid_normalised_100_inverse(averageC/100) - oldX;
				double stepSize = ConservationUtils.getSocialNormUpdateMultiplier() * ConservationUtils.getSigmoidMaxStepX();
				if (deltaX > stepSize) {
					deltaX = stepSize;
				}
				double newX = (oldX + deltaX >= 100) ? 100.0 : oldX + deltaX;
				newC = 100*ConservationUtils.sigmoid_normalised_100(newX);
				logger.debug(landholder.logprefix()
						+ "CE increased as average social norm ("
						+ String.format("%.1f", averageC)
						+ ") is greater than agent's CE ("
						+ String.format("%.1f", myC)
						+ ")");
				newC = landholder.setConservationEthicBarometer(newC);
				String newStatus = (landholder.isConservationEthicHigh()) ? "high" : "low";
				logger.debug(String.format("%supdated CE %.1f=>%.1f, which is %s"
						,landholder.logprefix(), myC, newC, newStatus));
			}
		}
	} };

	public void updateConservationEthicBarometer(double newC, double currentC) {
	}

}
