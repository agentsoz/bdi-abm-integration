package io.github.agentsoz.abmjill.genact;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2022 by its authors. See AUTHORS file.
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

import java.util.Map;

import io.github.agentsoz.bdiabm.EnvironmentActionInterface;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvironmentActionPlan extends Plan {

	private static final Logger logger = LoggerFactory.getLogger(EnvironmentActionPlan.class);

	public EnvironmentActionPlan(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		body = steps;
	}

	@Override
	public boolean context() {
		return true;
	}

	@Override
	public void setPlanVariables(Map<String, Object> vars) {
	}

	PlanStep[] steps = { new PlanStep() {
		public void step() {
			EnvironmentAction goal = (EnvironmentAction) getGoal();
			Agent agent = getAgent();
			if (agent instanceof io.github.agentsoz.bdiabm.Agent) {
				((io.github.agentsoz.bdiabm.Agent)agent)
						.getEnvironmentActionInterface()
						.packageAction(
								Integer.toString(agent.getId()),
							goal.getActionID(),
							goal.getActionParameters(),
							goal.getActionState());
			} else {
				logger.error(agent.getName()
						+ "does not implement EnvironmentActionInterface "
						+ "so cannot execute environment action");
			}
		}
	}, };

}
