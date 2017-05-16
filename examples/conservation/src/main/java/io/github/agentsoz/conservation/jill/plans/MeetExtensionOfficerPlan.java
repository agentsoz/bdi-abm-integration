/**
 * 
 */
package io.github.agentsoz.conservation.jill.plans;

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
