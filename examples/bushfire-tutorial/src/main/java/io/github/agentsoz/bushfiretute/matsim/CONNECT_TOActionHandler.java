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

import io.github.agentsoz.util.evac.ActionList;
import io.github.agentsoz.util.evac.PerceptList;
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
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.withinday.utils.EditPlans;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.bdimatsim.EventsMonitorRegistry.MonitoredEventType;
import io.github.agentsoz.bushfiretute.bdi.BDIModel;
import io.github.agentsoz.nonmatsim.BDIActionHandler;
import io.github.agentsoz.nonmatsim.BDIPerceptHandler;
import io.github.agentsoz.nonmatsim.PAAgent;
import io.github.agentsoz.bushfiretute.jack.agents.EvacResident;

public final class CONNECT_TOActionHandler implements BDIActionHandler {
	private static final Logger logger = LoggerFactory.getLogger("");
	
	private final BDIModel bdiModel;
	private final MATSimModel model;
	private static int cnt = 0 ;
	
	public CONNECT_TOActionHandler(BDIModel bdiModel, MATSimModel model) {
		this.bdiModel = bdiModel;
		this.model = model;
	}
	@Override
	public boolean handle(String agentID, String actionID, Object[] args) {
		logger.info("------------------------------------------------------------------------------------------") ;
		String destination = (String) args[1];
		// connect To route replanner method
		Id<Link> newLinkId = driveImmediatelyToActivityViaEvacRoute(Id.createPersonId(agentID), destination);
		if (newLinkId == null) {
			logger.warn("CONNECT_TO: returned a null link from the target activity");
			return true;
		}
		// Now register a event handler for when the agent arrives at the destination
		PAAgent agent = model.getAgentManager().getAgent( agentID );
		EvacResident bdiAgent = bdiModel.getBDICounterpart(agentID.toString());
		bdiAgent.log("replanned to drive to connecting link " + newLinkId.toString());
		agent.getPerceptHandler().registerBDIPerceptHandler(
				agent.getAgentID(),
				MonitoredEventType.ArrivedAtDestination,
				newLinkId.toString(),
				new BDIPerceptHandler() {
					@Override public boolean handle(Id<Person> agentId, Id<Link> linkId, MonitoredEventType monitoredEvent) {
						PAAgent agent = model.getAgentManager().getAgent( agentId.toString() );
						EvacResident bdiAgent = bdiModel.getBDICounterpart(agentId.toString());
						Object[] params = { linkId.toString() , Double.toString(bdiAgent.getCurrentTime())};
						
						agent.getActionContainer().register(ActionList.CONNECT_TO, params);
						// (yyyy probably does not make a difference in terms of current results, but: Shouldn't this be
						// called earlier, maybe around where the replanner is called?  kai, oct'17)
						
						agent.getActionContainer().get(ActionList.CONNECT_TO).setState(ActionContent.State.PASSED);
						agent.getPerceptContainer().put(PerceptList.ARRIVED_CONNECT_TO, params);
						return true; //unregister this handler
					}
				}
		);
		logger.info("------------------------------------------------------------------------------------------") ;
		return true;
	}
	
	final Id<Link> driveImmediatelyToActivityViaEvacRoute(Id<Person> agentId, String actType) {
		// This method
		// * finds the shortest path to the original evacuation route (from "Evacuation" to "Safe")
		// end then
		// * creates a new route from "here" via that "shortest path" and then continues on the
		// original evacuation route.
		
		double now = model.getTime();
		logger.info(" starting replanCurrentRoute : activity type: {}", actType);
		MobsimAgent agent = model.getMobsimDataProvider().getAgents().get(agentId);
		
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
		List<PlanElement> planElements = plan.getPlanElements() ;
		int currentIndex  = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
		logger.debug("current plan index {}", currentIndex);
		PlanElement pe =  planElements.get(currentIndex);
		
		
		//check current plan element is an activity  and ending the activity if its a Wait activity
		if( !(pe instanceof Activity) ) {
			logger.error("current plan element is not an activity, unable to replanCurrentRoute");
			return null;
		}
		
		// yy should this activity really always be a wait activity??  kai, oct'17
		Activity currentAct = (Activity) pe;
		if(currentAct.getType().equals("Wait")) {
			logger.info("set end time of the Wait activity to {}  type {} ", now);
			model.getReplanner().editPlans().rescheduleCurrentActivityEndtime(agent,now);
		}
		else {
			logger.info("current activity is not the type of Wait");
			return null;
		}
		
		//find the target activity
		int targetActIndex = EditPlans.indexOfNextActivityWithType(agent, actType);
		Activity targetAct = (Activity) planElements.get(targetActIndex);
		
//		int targetActIndex=0;
//		Activity targetAct=null;
//
//		for(int i=currentIndex;i<planElements.size();i++) {
//			PlanElement element =  planElements.get(i);
//			if( !(element instanceof Activity) ) {
//				continue;
//			}
//			Activity act = (Activity) element;
//			if(act.getType().equals(actType)) {
//				targetAct=act;
//				targetActIndex = i;
//				logger.debug("target activity plan index: {}", i);
//			}
//
//		}
		
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
		logger.debug("Optimised leg route info before replacement: {}", legWithOptRoute.toString());
		
		Route replanRoute = legWithOptRoute.getRoute();
		NetworkRoute netRoute = (NetworkRoute) replanRoute;
		
		//get the links
		List<Id<Link>> targetLinkIds = netRoute.getLinkIds();
		logger.debug("Size of the retreived links : {}", targetLinkIds.size());
		
		
		LinkedHashMap<String,Double> routeDistances =  new LinkedHashMap<>();
		
		
		for (Id<Link> possibleTargetLinkId : targetLinkIds) {
			
			Leg leg = model.getScenario().getPopulation().getFactory().createLeg(TransportMode.car);
			model.getReplanner().editRoutes().relocateFutureLegRoute(leg, currentAct.getLinkId(), possibleTargetLinkId, ((HasPerson)agent).getPerson() ) ;
			
			Route r = leg.getRoute();
			routeDistances.put(possibleTargetLinkId.toString(), r.getDistance());
			logger.debug("travel distance from link {}: {}",possibleTargetLinkId.toString(),r.getDistance());
			
			// ---
			
			// I think that it would actually be cleaner to us the "computer science" router directly, which is done by the code
			// coming now (not switched on). kai, jan'18
			
			Link currentLink = model.getScenario().getNetwork().getLinks().get(currentAct.getLinkId());
			Node startNode = currentLink.getToNode() ;

			Node endNode = model.getScenario().getNetwork().getLinks().get( possibleTargetLinkId ).getToNode() ;
			// (using toNode rather than fromNode because this is more consistent with the original approach which would
			// route from link to link. Could be changed; yy would then make more sense (because otherwise the result
			// also depends on the length of the destination link); but would change results.  kai, jan'18)

			LeastCostPathCalculator.Path path = model.getReplanner().editRoutes().getPathCalculator().calcLeastCostPath(
					startNode, endNode, now, null, null);
			
			double sum = 0. ;
			for ( Link link : path.links ) {
				sum += link.getLength() ;
			}
//			sum += currentLink.getLength() ;
			
			// check if new results are equal to old results:
//			Gbl.assertIf( sum==r.getDistance() ) ;
			if ( cnt < 1 ) {
				if (sum != r.getDistance()) {
					cnt++;
					logger.warn("old and new routing method produce different results; old = {}; new = {}.", r.getDistance(), sum);
					logger.warn( Gbl.ONLYONCE ) ;
				}
			}
			
		}
		
		//find the link for the shortest distance
		String connectLinkID=null;
		Double minDist = Collections.min(routeDistances.values());
		for(Map.Entry<String, Double> entry : routeDistances.entrySet()) {
			if(minDist == entry.getValue()) {
				connectLinkID = entry.getKey();
			}
		}
		
		logger.info("minimum distance : {} is found to linkID : {}", minDist, connectLinkID);
		
		//create the connect leg with minimum distance to get its route..
		Leg connectLeg = model.getScenario().getPopulation().getFactory().createLeg(TransportMode.car);
		model.getReplanner().editRoutes().relocateFutureLegRoute(connectLeg, currentAct.getLinkId(), Id.createLinkId(connectLinkID),((HasPerson)agent).getPerson() ) ;
		
		Route rt = connectLeg.getRoute();
		NetworkRoute netRt = (NetworkRoute) rt;
		
		List<Id<Link>> connectLegLinks = netRt.getLinkIds();
		logger.debug("retreived links of the route of connectLeg : {}", connectLegLinks.toString());
		
		//new array list as container for the links of target leg
		ArrayList<Id<Link>> targetLegLinkSet = new ArrayList<>();
		
		targetLegLinkSet.addAll(connectLegLinks);
		logger.debug(" added linkset of the connect leg to the targetLegLinkSet : {}", targetLegLinkSet.toString());
		
		boolean linkDeleted = false;
		for (Id<Link> lid : targetLinkIds) {
			
			if(lid.toString().equals(connectLinkID)) {
				linkDeleted = true;
			}
			
			if(linkDeleted) {
				targetLegLinkSet.add(lid);
				
			}
		}
		
		
		logger.debug("agent {} | added the subset of the optimised route to the targetLegLinkSet: {}",agentId, targetLegLinkSet.toString());
		
		//delete all plan elements between the current activity and the target activity
		int noDelElements = targetActIndex - (currentIndex+1);
		int count =0;
		while(count<noDelElements) {
			planElements.remove(currentIndex+1);
			count++;
		}
		
		logger.debug("agent {} | removed {} elements between currentAct and targetAct | new plan size after removal {}",agentId, noDelElements,planElements.size() );
		
		//create a new leg and replace its route with the merged route
		Leg targetLeg = model.getScenario().getPopulation().getFactory().createLeg(TransportMode.car);
		model.getReplanner().editRoutes().relocateFutureLegRoute(targetLeg, currentAct.getLinkId(), targetAct.getLinkId(),((HasPerson)agent).getPerson() ) ;
		
		Route rout = targetLeg.getRoute();
		NetworkRoute netRout = (NetworkRoute) rout;
		
		//set new route to the target Leg
		netRout.setLinkIds(currentAct.getLinkId(), targetLegLinkSet, targetAct.getLinkId());
		targetLeg.setRoute(netRout);
		
		//retreiving
		Id<Link> targetLink = targetAct.getLinkId();
		
		//add targetLeg to the plan
		planElements.add(currentIndex+1,targetLeg);
		logger.debug("agent {} | set the targetLegLinkSet to the route of the targetLeg and added it to the position of {}",agentId, currentIndex+1);
		
		
		WithinDayAgentUtils.resetCaches(agent);
		//		this.qsim.rescheduleActivityEnd(agent);
		
		return targetLink;
	}
	
}