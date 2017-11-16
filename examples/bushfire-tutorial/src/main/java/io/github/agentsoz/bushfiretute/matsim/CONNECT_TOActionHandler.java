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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdimatsim.PAAgent;
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.bdimatsim.EventsMonitorRegistry.MonitoredEventType;
import io.github.agentsoz.bdimatsim.app.BDIActionHandler;
import io.github.agentsoz.bdimatsim.app.BDIPerceptHandler;
import io.github.agentsoz.bushfiretute.bdi.BDIModel;
import io.github.agentsoz.bushfiretute.shared.ActionID;
import io.github.agentsoz.bushfiretute.shared.PerceptID;
import scenarioTWO.agents.EvacResident;

final class CONNECT_TOActionHandler implements BDIActionHandler {
	private static final Logger logger = LoggerFactory.getLogger("");

	private final BDIModel bdiModel;
	private final MATSimModel model;

	CONNECT_TOActionHandler(BDIModel bdiModel, MATSimModel model) {
		this.bdiModel = bdiModel;
		this.model = model;
	}
	@Override
	public boolean handle(String agentID, String actionID, Object[] args) {
		String destination = (String) args[1];
		// connect To route replanner method
		Id<Link> newLinkId = CONNECT_TOActionHandler.driveDirectlyToActivity(Id.createPersonId(agentID), destination, model);
		if (newLinkId == null) {
			ABMModel.logger.warn("CONNECT_TO: returned a null link from the target activity");
			return true;
		}
		// Now register a event handler for when the agent arrives at the destination
		PAAgent agent = model.getAgentManager().getAgent( agentID );
		EvacResident bdiAgent = bdiModel.getBDICounterpart(agentID.toString());
		bdiAgent.log("replanned to drive to connecting link " + newLinkId.toString());
		agent.getPerceptHandler().registerBDIPerceptHandler(
				agent.getAgentID(), 
				MonitoredEventType.ArrivedAtDestination, 
				newLinkId,
				new BDIPerceptHandler() {
					@Override
					public boolean handle(Id<Person> agentId, Id<Link> linkId, MonitoredEventType monitoredEvent) {
						PAAgent agent = model.getAgentManager().getAgent( agentId.toString() );
						EvacResident bdiAgent = bdiModel.getBDICounterpart(agentId.toString());
						Object[] params = { linkId.toString() , Long.toString(bdiAgent.getCurrentTime())};
						
						agent.getActionContainer().register(ActionID.CONNECT_TO, params);
						// (yyyy probably does not make a difference in terms of current results, but: Shouldn't this be
						// called earlier, maybe around where the replanner is called?  kai, oct'17)

						agent.getActionContainer().get(ActionID.CONNECT_TO).setState(ActionContent.State.PASSED);
						agent.getPerceptContainer().put(PerceptID.ARRIVED_CONNECT_TO, params);
						return true; //unregister this handler
					}
				});
		return true;
	}
	final static Id<Link> driveDirectlyToActivity(Id<Person> agentId, String actType, MATSimModel model) {
			 // used at least once
			 
			// This method 
			// * finds the shortest path to the original evacuation route (from "Evacuation" to "Safe")
			// end then
			// * creates a new route from "here" via that "shortest path" and then continues on the 
			// original evacuation route.
	
			double now = model.getTime();
			logger.debug(" starting replanCurrentRoute : activity type: {}", actType);
			Map<Id<Person>, MobsimAgent> mapping = model.getMobsimDataProvider().getAgents();
			MobsimAgent agent = mapping.get(agentId);
	
			Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
			List<PlanElement> planElements = plan.getPlanElements() ;
			int currentIndex  = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
			logger.trace("current plan index {}", currentIndex);
			PlanElement pe =  planElements.get(currentIndex);
	
	
			//check current plan element is an activity  and ending the activity if its a Wait activity
			if( !(pe instanceof Activity) ) {
				logger.error("current plan element is not an activity, unable to replanCurrentRoute");
				return null;
			}
	
			// should this activity always be a wait activity??
			Activity currentAct = (Activity) pe;
			if(currentAct.getType().equals("Wait")) {
				logger.debug("set end time of the Wait activity to {}  type {} ", now);
				currentAct.setEndTime(now);
			}
			else {
				logger.debug("current activity is not the type of Wait");
				return null;
			}
	
	
			//find the target activity
			int targetActIndex=0;
			Activity targetAct=null;
	
			for(int i=currentIndex;i<planElements.size();i++) {
				PlanElement element =  planElements.get(i);
				if( !(element instanceof Activity) ) {
					continue;
				}
				Activity act = (Activity) element;
				if(act.getType().equals(actType)) {
					targetAct=act;
					targetActIndex = i;
					logger.trace("target activity plan index: {}", i);
				}				
	
			}
	
			if (targetAct == null) { 
				logger.error("replanCurrentRoute: target activity name not found in the plan");
				return null;
			}
	
			//
			PlanElement e = planElements.get(targetActIndex-1);
			if(!(e instanceof Leg)) { 
				logger.error("replanCurrentRoute: selected plan element is not a leg, cannot retreive the route");
				return null;
			}
	
			Leg legWithOptRoute =  (Leg) e;
			logger.trace("Optimised leg route info before replacement: {}", legWithOptRoute.toString());
	
			Route replanRoute = legWithOptRoute.getRoute();
			NetworkRoute netRoute = (NetworkRoute) replanRoute;
	
			//get the links
			List<Id<Link>> targetLinkIds = netRoute.getLinkIds();
			logger.trace("Size of the retreived links : {}", targetLinkIds.size());
	
	
			LinkedHashMap<String,Double> routeDistances =  new LinkedHashMap<>();
	
	
			for (Id<Link> lid : targetLinkIds) {
	
				Link link = model.getScenario().getNetwork().getLinks().get(lid);
				@SuppressWarnings("unused")
				Node n = link.getFromNode();
	
				Leg leg = model.getScenario().getPopulation().getFactory().createLeg(TransportMode.car);
				model.getReplanner().getEditRoutes().relocateFutureLegRoute(leg, currentAct.getLinkId(), link.getId(),((HasPerson)agent).getPerson() ) ;
	
				Route r = leg.getRoute();
				routeDistances.put(lid.toString(), r.getDistance());
				logger.trace("travel distance from link {}: {}",lid.toString(),r.getDistance());
			}
	
			//find the link for the shortest distance
			String connectLinkID=null;
			Double minDist = Collections.min(routeDistances.values());
			for(Map.Entry<String, Double> entry : routeDistances.entrySet()) {
				if(minDist == entry.getValue()) { 
					connectLinkID = entry.getKey();
				}
			}
	
			logger.debug("minimum distance : {} is found to linkID : {}", minDist, connectLinkID);
	
			//create the connect leg with minimum distance to get its route..
			Leg connectLeg = model.getScenario().getPopulation().getFactory().createLeg(TransportMode.car);
			model.getReplanner().getEditRoutes().relocateFutureLegRoute(connectLeg, currentAct.getLinkId(), Id.createLinkId(connectLinkID),((HasPerson)agent).getPerson() ) ;
	
			Route rt = connectLeg.getRoute();
			NetworkRoute netRt = (NetworkRoute) rt;
	
			List<Id<Link>> connectLegLinks = netRt.getLinkIds();
			logger.trace("retreived links of the route of connectLeg : {}", connectLegLinks.toString());
	
			//new array list as container for the links of target leg
			ArrayList<Id<Link>> targetLegLinkSet = new ArrayList<>();
	
			targetLegLinkSet.addAll(connectLegLinks);
			logger.trace(" added linkset of the connect leg to the targetLegLinkSet : {}", targetLegLinkSet.toString());
	
			boolean linkDeleted = false;
			for (Id<Link> lid : targetLinkIds) {
	
				if(lid.toString().equals(connectLinkID)) {
					linkDeleted = true;
				}
	
				if(linkDeleted) { 
					targetLegLinkSet.add(lid);
	
				}
			}
	
	
			logger.trace("agent {} | added the subset of the optimised route to the targetLegLinkSet: {}",agentId, targetLegLinkSet.toString());
	
			//delete all plan elements between the current activity and the target activity
			int noDelElements = targetActIndex - (currentIndex+1);
			int count =0;
			while(count<noDelElements) { 
				planElements.remove(currentIndex+1);
				count++;
			}
	
			logger.trace("agent {} | removed {} elements between currentAct and targetAct | new plan size after removal {}",agentId, noDelElements,planElements.size() );
	
			//create a new leg and replace its route with the merged route
			Leg targetLeg = model.getScenario().getPopulation().getFactory().createLeg(TransportMode.car);
			model.getReplanner().getEditRoutes().relocateFutureLegRoute(targetLeg, currentAct.getLinkId(), targetAct.getLinkId(),((HasPerson)agent).getPerson() ) ;
	
			Route rout = targetLeg.getRoute();
			NetworkRoute netRout = (NetworkRoute) rout;
	
			//set new route to the target Leg
			netRout.setLinkIds(currentAct.getLinkId(), targetLegLinkSet, targetAct.getLinkId());
			targetLeg.setRoute(netRout);
	
			//retreiving
			Id<Link> targetLink = targetAct.getLinkId();
	
			//add targetLeg to the plan
			planElements.add(currentIndex+1,targetLeg);
			logger.trace("agent {} | set the targetLegLinkSet to the route of the targetLeg and added it to the position of {}",agentId, currentIndex+1);	
	
	
			WithinDayAgentUtils.resetCaches(agent);
	//		this.qsim.rescheduleActivityEnd(agent);
	
			return targetLink;
		}
		 /*
		final void removeFutureActivities(Id<Person> agentId) {
			// possibly never used
			
			logger.debug("inside removeFutureActivities method");
			Map<Id<Person>, MobsimAgent> mapping = model.getMobsimAgentMap();
			MobsimAgent agent = mapping.get(agentId);
	
			Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
			List<PlanElement> planElements = plan.getPlanElements() ;
			int currentPlanIndex  = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
	
			logger.debug("plan size before removal : {}", planElements.size());
			while(planElements.size() > currentPlanIndex+1){
				planElements.remove(planElements.size()-1);
			}
	
			logger.debug("plan size after removal : {}", planElements.size());
			PlanElement currentPE = planElements.get(currentPlanIndex);
			if( !(currentPE instanceof Activity) ) { 
	
				logger.error("current plan element is not an activity - aborting");
				return ;
			}
			WithinDayAgentUtils.resetCaches(agent);
			this.qsim.rescheduleActivityEnd(agent);
		}
		*/
}