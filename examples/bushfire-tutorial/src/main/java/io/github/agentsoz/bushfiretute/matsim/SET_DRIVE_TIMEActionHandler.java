package io.github.agentsoz.bushfiretute.matsim;

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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.withinday.utils.EditPlans;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdimatsim.MATSimAgent;
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.bdimatsim.app.BDIActionHandler;
import io.github.agentsoz.bushfiretute.BDIModel;
import io.github.agentsoz.bushfiretute.shared.ActionID;
import scenarioTWO.agents.EvacResident;

final class SET_DRIVE_TIMEActionHandler implements BDIActionHandler {
	private static final Logger logger = LoggerFactory.getLogger("");

	private final BDIModel bdiModel;

	public SET_DRIVE_TIMEActionHandler(BDIModel bdiModel) {
		this.bdiModel = bdiModel;
	}

	@Override
	public boolean handle(String agentID, String actionID, Object[] args, MATSimModel model) {
		double newTime = (double) args[1];
		String actType = (String) args[2];        

		changeActivityEndTimeByActivityType(Id.createPersonId( agentID ),actType, newTime, model);

		// Now set the action to passed straight away
		MATSimAgent agent = model.getBDIAgent(agentID);
		EvacResident bdiAgent = bdiModel.getBDICounterpart(agentID.toString());
		bdiAgent.log("has set the end time for activity " + actType + " to " + newTime + " seconds past now." );
		Object[] params = {};
		agent.getActionContainer().register(ActionID.SET_DRIVE_TIME, params);
		agent.getActionContainer().get(ActionID.SET_DRIVE_TIME).setState(ActionContent.State.PASSED);
		return true;
	}

	/**
	 * The way I found this, "newTime" is added to "currentTime", and that is the new end time.  I don't find this particularly
	 * intuitive, especially for future activities.  kai, nov'17
	 */
	static final boolean changeActivityEndTimeByActivityType(Id<Person> agentId, String actType, double newTime, MATSimModel model) {
		
		logger.debug("received to modify the endtime of activity {}", actType);
		MobsimAgent agent = model.getMobsimAgentMap().get(agentId);
	
		int currentIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
		int indexToChange= -1 ;
		logger.debug("forceEndActivity: agent {} | current plan Index  {} | current time : {} ",agent.getId().toString(), currentIndex, model.getTime() );
	
		if (actType.equals("Current")) { 
			if( !(WithinDayAgentUtils.getCurrentPlanElement(agent) instanceof Activity) ) {
				logger.error("current plan element is not an activity, unable to forceEndActivity");
				return false;
			}
			indexToChange = WithinDayAgentUtils.getCurrentPlanElementIndex(agent) ;
		} else {
			indexToChange = EditPlans.indexOfNextActivityWithType(agent, actType) ;
		}
		
		if ( indexToChange==-1 ) {
			logger.error("could not find the activity to End");
			return false;
		}
		
		double newEndTime =  model.getTime() +  newTime;
		logger.debug("change end time of actvity with index {} to new end time  {} ", indexToChange, newEndTime );
		model.getReplanner().getEditPlans().rescheduleActivityEndtime(agent, indexToChange, newEndTime);
	
		return true;
	}
	/*
	void insertTempActivity(Id<Person> agentId) {
		// possibly never used
		
		Map<Id<Person>, MobsimAgent> mapping = model.getMobsimAgentMap();
		MobsimAgent agent = mapping.get(agentId);
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
	
		List<PlanElement> planElements = plan.getPlanElements() ;
	
		Activity evacAct = (Activity) planElements.get(2);
		//Temp Activivity 
		Activity tempAct = this.model.getScenario().getPopulation().getFactory().createActivityFromLinkId("Temp", evacAct.getLinkId() ) ;
		tempAct.setEndTime(5.0);
	
		planElements.add(2,tempAct);
	
		WithinDayAgentUtils.resetCaches(agent);
		this.qsim.rescheduleActivityEnd(agent);
	}
	*/
}