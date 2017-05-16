/**
 * 
 */
package io.github.agentsoz.conservation.jill.plans;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2017 by its authors. See AUTHORS file.
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

import java.util.HashMap;

import io.github.agentsoz.conservation.ConservationUtils;
import io.github.agentsoz.conservation.Log;
import io.github.agentsoz.conservation.jill.agents.Landholder;
import io.github.agentsoz.conservation.jill.goals.MeetExtensionOfficerGoal;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanStep;

/**
 * @author dsingh
 *
 */
public class MeetExtensionOfficerPlan extends Plan {

	Landholder landholder;
	MeetExtensionOfficerGoal meetExtensionOfficerGoal;
	/**
	 * @param agent
	 * @param goal
	 * @param name
	 */
	public MeetExtensionOfficerPlan(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		landholder = (Landholder) agent;
		meetExtensionOfficerGoal = (MeetExtensionOfficerGoal) goal;
		body = steps;
	}

	@Override
	public boolean context() {
		return true;
	}

	@Override
	public void setPlanVariables(HashMap<String, Object> arg0) {
	}

	PlanStep[] steps = { 
		new PlanStep() {
			public void step() {
				
				double currentC = landholder.getConservationEthicBarometer();
				double newC = currentC + ConservationUtils.getVisitConservationEthicBoostValue();
					//+ ConservationUtils.getStaticConservationEthicModifier();

				// Finally, update land holder's C and recalculate whether his C is
				// high or low.
				newC = landholder.setConservationEthicBarometer(newC);
				landholder.setConservationEthicHigh(landholder
					.isConservationEthicHigh(newC));
				String newStatus = (landholder.isConservationEthicHigh()) ? "high"
					: "low";
				Log.warn("Agent " + landholder.getName() + " updated his C from "
					+ currentC + " to :" + newC + ", which is " + newStatus);
			}
		} 
	};
	
}
