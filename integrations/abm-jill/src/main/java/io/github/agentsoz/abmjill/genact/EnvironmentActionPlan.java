package io.github.agentsoz.abmjill.genact;

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

import java.util.Map;

import io.github.agentsoz.abmjill.JillModel;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanStep;

public class EnvironmentActionPlan extends Plan {

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
			JillModel.packageAgentActionV2(Integer.toString(getAgent().getId()),
					goal.getActionID(), goal.getActionParameters(), goal.getActionState());
		}
	}, };

}
