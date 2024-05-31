package io.github.agentsoz.abmjill.testagent;

import io.github.agentsoz.abmjill.genact.EnvironmentAction;

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

import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanStep;
import io.github.agentsoz.jill.util.Log;

import java.util.Map;

public class PlanA extends Plan { 

	public PlanA(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		body = steps;
	}

	public boolean context() {
		return true;
	}
	
	PlanStep[] steps = {
			// Post the test action
			new PlanStep() {
				public void step() {
					subgoal(new EnvironmentAction(
							Integer.toString(((TestAgent)getAgent()).getId()),
							"TESTACTION", null));
					}
			},
			// Now wait till it is finished
			new PlanStep() {
				public void step() {
					// Must suspend the agent when waiting for external stimuli
					Log.info("Agent"+getAgent().getId()+": will wait for test action to finish");
					((TestAgent)getAgent()).suspend(true);
				}
			},
			// All done
			new PlanStep() {
				public void step() {
					Log.info("Agent"+getAgent().getId()+": test action has finished");
				}
			}
	};

	@Override
	public void setPlanVariables(Map<String, Object> vars) {
		// TODO Auto-generated method stub
		
	}
}
