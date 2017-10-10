package io.github.agentsoz.bushfiretute.matsim;

import java.util.ArrayList;


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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import org.matsim.core.mobsim.qsim.ActivityEndRescheduler;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.withinday.utils.EditRoutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.bdimatsim.Replanner;

final class CustomReplanner extends Replanner{

	CustomReplanner(MATSimModel model, ActivityEndRescheduler activityEndRescheduler) {
		super(model, activityEndRescheduler);
	}

	final void addNewLegAndActvityToPlan(Id<Person> agentId, Id<Link> newActivityLinkId, int pickupTime )
	{
		logger.debug("starting addNewLegAndActvityToPlan method..");
		double now = model.getTime() ; 
		MobsimAgent agent = model.getMobsimAgentMap().get(agentId);

		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;

		List<PlanElement> planElements = plan.getPlanElements() ;

		int currentPlanIndex  = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);

		logger.trace("number of plan elements: " + planElements.size());
		logger.trace("current plan index : " + currentPlanIndex);

		//ending the current activity if its a Wait activity
		PlanElement currentPE = planElements.get(currentPlanIndex);
		if(currentPE instanceof Activity) { 
			Activity currentAct = (Activity) currentPE;
			if(currentAct.getType().equals("Wait")) {
				logger.debug("Wait/Current activity ending now:  {}", now);
				currentAct.setEndTime(now);
			}

			Leg newLeg = this.model.getScenario().getPopulation().getFactory().createLeg(TransportMode.car);
			//newLeg.setDepartureTime(10);	
			this.editRoutes.relocateFutureLegRoute(newLeg, currentAct.getLinkId(), newActivityLinkId,((HasPerson)agent).getPerson() );
			logger.debug(" added leg to plan..");
			planElements.add(currentPlanIndex+1,newLeg);

			//ADDING Pickup ACTIVITY
			Activity pickupAct = this.model.getScenario().getPopulation().getFactory().createActivityFromLinkId("Pickup", newActivityLinkId ) ;
			pickupAct.setMaximumDuration(pickupTime);
			logger.debug(" added {} type activity",pickupAct.getType());
			planElements.add(currentPlanIndex+2,pickupAct);

			//ADDING Wait ACTIVITY
			Activity waitAct = this.model.getScenario().getPopulation().getFactory().createActivityFromLinkId("Wait", newActivityLinkId ) ;
			waitAct.setEndTime( Double.POSITIVE_INFINITY ) ;
			//			waitAct.setEndTime( pickupTime ) ;
			//			waitAct.setMaximumDuration(10) ;
			logger.debug(" added {} type activity with INFINITY end time..",waitAct.getType());
			planElements.add(currentPlanIndex+3,waitAct);

			//			WithinDayAgentUtils.resetCaches(agent);
			//			this.internalInterface.rescheduleActivityEnd(agent);

			//rerouting the leg after wait activity
			logger.debug("reRouting the leg after wait activity");
			Leg nextLeg = (Leg)planElements.get(currentPlanIndex+4);
			Activity nextAct = (Activity)planElements.get(currentPlanIndex+5);
			logger.trace("all evac activity info : {} ", nextAct.toString());

			this.editRoutes.relocateFutureLegRoute(nextLeg,newActivityLinkId,nextAct.getLinkId(),((HasPerson)agent).getPerson() );

			logger.trace("number of plan elements after adding new leg : " + planElements.size());

			WithinDayAgentUtils.resetCaches(agent);
			this.internalInterface.rescheduleActivityEnd(agent);

		}


	}

	final void addNewActivityToPlan(Id<Person> agentId,int pickupTime)
	{
		logger.debug("started addNewActivityToPlan method..");
		double now = model.getTime() ; 

		MobsimAgent agent = model.getMobsimAgentMap().get(agentId);
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
		List<PlanElement> planElements = plan.getPlanElements() ;
		int currentPlanIndex  = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);

		logger.trace("number of plan elements : " + planElements.size());
		logger.trace("current plan index : " + currentPlanIndex);

		//1-ending the current activity
		PlanElement currentPE = planElements.get(currentPlanIndex);
		if( !(currentPE instanceof Activity) ) { 
			logger.error("currently exceuting plan element is not an activity");		
			return ;
		}
		Activity currentAct = (Activity) currentPE;
		currentAct.setEndTime(now);

		//2-ADDING PICKUP ACTIVITY
		Activity newAct = this.model.getScenario().getPopulation().getFactory().createActivityFromLinkId("PICKUP", currentAct.getLinkId() ) ;
		newAct.setMaximumDuration(pickupTime);
		planElements.add(currentPlanIndex+1,newAct);
		logger.debug(" added a new {} activity",newAct.getType());

		//3-ADDING Wait ACTIVITY
		Activity waitAct = this.model.getScenario().getPopulation().getFactory().createActivityFromLinkId("Wait", currentAct.getLinkId() ) ;
		waitAct.setEndTime( Double.POSITIVE_INFINITY ) ;
		logger.debug(" added {} type activity with INFINITY end time..",waitAct.getType());
		planElements.add(currentPlanIndex+2,waitAct);

		WithinDayAgentUtils.resetCaches(agent);
		this.internalInterface.rescheduleActivityEnd(agent);
	}

	final void addNewLegToPlan(Id<Person> agentId,Id<Link> newActivityLinkId, String dest)
	{
		logger.debug("agent {} | started addNewLegToPlan method..", agentId);
		double now = model.getTime() ; 

		MobsimAgent agent = model.getMobsimAgentMap().get(agentId);
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
		List<PlanElement> planElements = plan.getPlanElements() ;
		int currentPlanIndex  = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);

		logger.trace("number of plan elements : " + planElements.size());
		logger.trace("current plan index : " + currentPlanIndex);

		//1-ending the current activity
		PlanElement currentPE = planElements.get(currentPlanIndex);
		if( !(currentPE instanceof Activity) ) { 
			logger.error("currently exceuting plan element is not an activity");		
			return ;
		}
		Activity currentAct = (Activity) currentPE;
		currentAct.setEndTime(now);

		//2-Adding a new activity and a leg 
		Leg newLeg = this.model.getScenario().getPopulation().getFactory().createLeg(TransportMode.car);
		newLeg.setDepartureTime(now);	
		this.editRoutes.relocateFutureLegRoute(newLeg, currentAct.getLinkId(), newActivityLinkId,((HasPerson)agent).getPerson() );
		planElements.add(currentPlanIndex+1,newLeg);
		logger.debug(" added a new leg to current index");

		//3-ADDING wait ACTIVITY
		Activity newAct = this.model.getScenario().getPopulation().getFactory().createActivityFromLinkId(dest, newActivityLinkId ) ;
		newAct.setEndTime(Double.POSITIVE_INFINITY);
		//		newAct.setEndTime(20);
		planElements.add(currentPlanIndex+2,newAct);
		logger.debug(" added a new {} activity",newAct.getType());

		//4-Relocating the leg after wait activity
		if(currentPlanIndex+3 == planElements.size()) {  //  end of plan -there are no further leg to reroute 
			logger.debug("no leg found to relocate from the added destination..");

		}
		else{ 
			// there are elements - a leg exist to relocate
			logger.debug("reRouting the leg after the added activity..");
			Leg nextLeg = (Leg)planElements.get(currentPlanIndex+3);
			Activity nextAct = (Activity)planElements.get(currentPlanIndex+4);

			this.editRoutes.relocateFutureLegRoute(nextLeg,newActivityLinkId,nextAct.getLinkId(),((HasPerson)agent).getPerson() ) ; 

			logger.debug("addNewActivityToPlan - leg info after reroute : " + nextLeg.toString());
		}

		WithinDayAgentUtils.resetCaches(agent);
		this.internalInterface.rescheduleActivityEnd(agent);

	}
	final void modifyPlanForDest(Id<Person> agentId,Id<Link> newActivityLinkId, String dest)
	{
		logger.debug("started addNewLegToPlan method..");
		double now = model.getTime() ; 

		MobsimAgent agent = model.getMobsimAgentMap().get(agentId);
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
		List<PlanElement> planElements = plan.getPlanElements() ;
		int currentPlanIndex  = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);

		logger.trace("number of plan elements : " + planElements.size());
		logger.trace("current plan index : " + currentPlanIndex);

		//1-ending the current activity if its a Wait activity
		PlanElement currentPE = planElements.get(currentPlanIndex);
		if( !(currentPE instanceof Activity) ) { 
			logger.error("currently exceuting plan element is not an activity");		
			return ;
		}

		Activity currentAct = (Activity) currentPE;
		if(currentAct.getType().equals("Wait")) {
			logger.debug("set end time of the Wait activity to {}  type {} ", now);
			currentAct.setEndTime(now);
		}
		else {
			logger.debug("current activity is not the type of Wait");
			return ;
		}

		if(dest.equals("central point")) {
			//reRouting the initially added leg to central point from last pick up activity
			PlanElement nextPE = planElements.get(currentPlanIndex+1);
			if(nextPE instanceof Leg) {
				Leg legCP  = (Leg) nextPE ;

				//geting central point activity
				Activity activityCP = (Activity) planElements.get(currentPlanIndex+2);
				if ( !(activityCP.getType().equals("Evacuation")) )
				{
					logger.error("activity Evacuation is not the activity after the current activity, Aborting");
					return ;

				}

				this.editRoutes.relocateFutureLegRoute(legCP,currentAct.getLinkId(),activityCP.getLinkId(),((HasPerson)agent).getPerson() ) ;
				logger.debug("leg after pick up activity relocated to new dest");
				WithinDayAgentUtils.resetCaches(agent);
				this.internalInterface.rescheduleActivityEnd(agent);
				return ;
			}
			else {
				logger.error("plan element after the last pickup activiy is not a leg, Aborting");
			}
		}


		if(dest.equals("safe")) { 
			logger.debug("received the destination : safe");
			//removing all other plan elements and setting the last dest to Safe
			int planSize = planElements.size();
			logger.debug("plan size before modifying the plan : {}  current plan index : {} ",planSize , currentPlanIndex);

			//remove all plan elements between current activity and 
			//last two plan elements . eventually the leg between current and 
			//last activty will relocated to the safe dest 
			//			for (int i = currentPlanIndex+1 ; i <= planElements.size()-2; i ++) { 
			//				logger.debug("removed element : {} ", i);
			//				planElements.remove(i);
			//			}
			int count = 0 ;
			while (planElements.size() > planSize - 2) {

				planElements.remove(currentPlanIndex+1);
				count++;
			}
			logger.debug("total number of elements removed : {} ",count);	

			logger.debug("plan element size after removing : {}",  planElements.size() );
			PlanElement beforeLastElement = plan.getPlanElements().get(planElements.size()-2);
			if ( !(beforeLastElement instanceof Leg) ) {
				logger.error("selected plan element after removing plan elements is not a leg");
				return ;
			}
			//reLocating the the leg to safe dest
			Leg lastLeg = (Leg) beforeLastElement;
			Activity safeAct = (Activity) plan.getPlanElements().get(planElements.size()-1);

			this.editRoutes.relocateFutureLegRoute(lastLeg,currentAct.getLinkId(),safeAct.getLinkId(),((HasPerson)agent).getPerson() ) ;
			logger.debug("relocated the last leg to safe dest");
			WithinDayAgentUtils.resetCaches(agent);
			this.internalInterface.rescheduleActivityEnd(agent);
			return ;

		}
		if(dest.equals("home")) {
			logger.debug("destination home received");

			Activity homeAct = this.model.getScenario().getPopulation().getFactory().createActivityFromLinkId("Home", newActivityLinkId ) ;
			Leg homeLeg = this.model.getScenario().getPopulation().getFactory().createLeg(TransportMode.car);


			// new Route for current Leg.
			homeLeg.setDepartureTime(now);
			this.editRoutes.relocateFutureLegRoute(homeLeg, currentAct.getLinkId(), newActivityLinkId,((HasPerson)agent).getPerson() ) ;

			double homeTime = now + 7200.0 + 1000.0 ; // 2hours specnt at home, 40mins to get to home
			homeAct.setEndTime(homeTime) ;
			homeAct.setMaximumDuration(homeTime);
			planElements.add(currentPlanIndex+1,homeLeg);
			planElements.add(currentPlanIndex+2,homeAct);

			logger.debug(" finished adding home leg and activity");

			//reRouting the initially added leg  from home to central point
			PlanElement nextPE = planElements.get(currentPlanIndex+3);
			if(nextPE instanceof Leg) {
				Leg legCP  = (Leg) nextPE ;

				//geting central point activity
				Activity activityCP = (Activity) planElements.get(currentPlanIndex+4);
				if ( !(activityCP.getType().equals("Evacuation")) )
				{
					logger.error("activity Evacuation is not found when adding destination home, Aborting");
					return ;

				}

				this.editRoutes.relocateFutureLegRoute(legCP,homeAct.getLinkId(),activityCP.getLinkId(),((HasPerson)agent).getPerson() ) ;

				logger.debug(" finished reRouting leg from home to cenrtal point");
				WithinDayAgentUtils.resetCaches(agent);
				this.internalInterface.rescheduleActivityEnd(agent);

				return ;
			}

		}

	}

	final Id<Link> replanCurrentRoute(Id<Person> agentId, String actType)
	{
		double now = model.getTime();
		logger.debug(" starting replanCurrentRoute : activity type: {}", actType);
		Map<Id<Person>, MobsimAgent> mapping = model.getMobsimAgentMap();
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


		/*
		 * 			//getCurrentNode
			Id<Link> currentAct_linkId = currentAct.getLinkId();
			Link cur_link = this.model.getScenario().getNetwork().getLinks().get(currentAct_linkId);
			Node curNode = cur_link.getToNode();
			logger.trace("ToNode  and the link of the current activity: node-{} link {}", curNode.getId(),currentAct_linkId.toString());

			Person p = this.model.getScenario().getPopulation().getPersons().get(agentId);
			logger.trace("retreived the person for ID: {}", agentId.toString());


			FastAStarLandmarksFactory landmarkFac = new FastAStarLandmarksFactory(
					this.model.getScenario().getNetwork(),
					TravelDisutilityUtils.createFreespeedTravelTimeAndDisutility( model.getScenario().getConfig().planCalcScore()) 
);

			LeastCostPathCalculator pathCalculator = 
					landmarkFac.createPathCalculator(this.model.getScenario().getNetwork(),
					TravelDisutilityUtils.createFreespeedTravelTimeAndDisutility( model.getScenario().getConfig().planCalcScore()), 
					TravelTimeUtils.createFreeSpeedTravelTime()); 

				//Path path = pathCalculator.calcLeastCostPath(curNode, n, this.model.getTime(), p, vehicle);
				//logger.debug("travel cost from basenode to node {} : {}",n,path.travelCost);

		 */
		LinkedHashMap<String,Double> routeDistances =  new LinkedHashMap<String,Double>();


		for (Id<Link> lid : targetLinkIds) {

			Link link = this.model.getScenario().getNetwork().getLinks().get(lid);
			@SuppressWarnings("unused")
			Node n = link.getFromNode();

			Leg leg = this.model.getScenario().getPopulation().getFactory().createLeg(TransportMode.car);
			this.editRoutes.relocateFutureLegRoute(leg, currentAct.getLinkId(), link.getId(),((HasPerson)agent).getPerson() ) ;

			Route r = leg.getRoute();
			routeDistances.put(lid.toString(), r.getDistance());
			logger.trace("travel distance from link {}: {}",lid.toString(),r.getDistance());
		}

		//find the link for the shortest distance
		String connectLinkID=null;
		Double minDist = Collections.min(routeDistances.values());
		for(Map.Entry<String, Double> entry : routeDistances.entrySet()) {
			if(minDist == entry.getValue()) { 
				connectLinkID = (String) entry.getKey();
			}
		}

		logger.debug("minimum distance : {} is found to linkID : {}", minDist, connectLinkID);

		//create the connect leg with minimum distance to get its route..
		Leg connectLeg = this.model.getScenario().getPopulation().getFactory().createLeg(TransportMode.car);
		editRoutes.relocateFutureLegRoute(connectLeg, currentAct.getLinkId(), Id.createLinkId(connectLinkID),((HasPerson)agent).getPerson() ) ;

		Route rt = connectLeg.getRoute();
		NetworkRoute netRt = (NetworkRoute) rt;

		List<Id<Link>> connectLegLinks = netRt.getLinkIds();
		logger.trace("retreived links of the route of connectLeg : {}", connectLegLinks.toString());

		//new array list as container for the links of target leg
		ArrayList<Id<Link>> targetLegLinkSet = new ArrayList<Id<Link>>();

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
		Leg targetLeg = this.model.getScenario().getPopulation().getFactory().createLeg(TransportMode.car);
		this.editRoutes.relocateFutureLegRoute(targetLeg, currentAct.getLinkId(), targetAct.getLinkId(),((HasPerson)agent).getPerson() ) ;

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
		this.internalInterface.rescheduleActivityEnd(agent);

		return targetLink;
	}

	final void removeFutureActivities(Id<Person> agentId)
	{
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
		this.internalInterface.rescheduleActivityEnd(agent);

	}

	final boolean forceEndActivity(Id<Person> agentId, String actType, double newEndTime)
	{
		logger.debug("received to modify the endtime of activity {}", actType);
		Map<Id<Person>, MobsimAgent> mapping = model.getMobsimAgentMap();
		MobsimAgent agent = mapping.get(agentId);

		Activity endAct=null;
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
		int currentIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
		int EvacIndex= 100;
		List<PlanElement> planElements = plan.getPlanElements() ;
		PlanElement pe =  planElements.get(currentIndex);
		logger.debug("forceEndActivity: agent {} | current plan Index  {} | current time : {} ",agent.getId().toString(), currentIndex, model.getTime() );

		if (actType.equals("Current")) { 


			if( !(pe instanceof Activity) ) {
				logger.error("current plan element is not an activity, unable to forceEndActivity");
				return false;
			}
			endAct = (Activity) planElements.get(currentIndex);
		}

		if(actType.equals("Evacuation")) {

			for(int i=currentIndex;i<planElements.size();i++) {
				PlanElement element =  planElements.get(i);
				if( !(element instanceof Activity) ) {
					//logger.error("current plan element is not an activity, unable to forceEndActivity");
					continue;
				}
				Activity act = (Activity) element;
				if(act.getType().equals("Evacuation")) {
					endAct=act;
					EvacIndex = i;
				}				
			}
		}
		//delaying activity by newEndTime
		if(endAct == null) {
			logger.error("could not find the activity to End");
			return false;
		}
		double ENDTIME =  model.getTime() +  newEndTime;
		logger.debug("force end current actvity index {} with new end time  {} ", EvacIndex, ENDTIME );
		endAct.setEndTime(ENDTIME);
		//endAct.setMaximumDuration(ENDTIME);


		//reRouteCurrentLeg(agent, model.getTime()) ;
		//			if(currentIndex == 1) {
		//				
		//				reRouteCurrentLeg(agent, model.getTime()) ;
		//				
		//				Activity home =  (Activity) planElements.get(0);
		//				planElements.remove(1);
		//				Leg newHomeToEvacLeg = this.model.getScenario().getPopulation().getFactory().createLeg(TransportMode.car);
		//				EditRoutes.relocateFutureLegRoute(newHomeToEvacLeg, agent.getCurrentLinkId(), endAct.getLinkId(),((HasPerson)agent).getPerson(), 
		//						this.model.getScenario().getNetwork(), tripRouter );
		//				planElements.add(1,newHomeToEvacLeg);
		//			}

		WithinDayAgentUtils.resetCaches(agent);
		if ( currentIndex != 1)
			this.internalInterface.rescheduleActivityEnd(agent);
		return true;


	}


	public void addTempActivity(Id<Person> agentId) { 
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
		this.internalInterface.rescheduleActivityEnd(agent);

	}


}
