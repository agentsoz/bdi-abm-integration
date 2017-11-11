package io.github.agentsoz.bushfiretute.matsim;

import java.util.List;

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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.network.SearchableNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdimatsim.MATSimActionList;
import io.github.agentsoz.bdimatsim.MATSimAgent;
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.bdimatsim.MATSimPerceptList;
import io.github.agentsoz.bdimatsim.AgentActivityEventHandler.MonitoredEventType;
import io.github.agentsoz.bdimatsim.app.BDIActionHandler;
import io.github.agentsoz.bdimatsim.app.BDIPerceptHandler;
import io.github.agentsoz.bushfiretute.BDIModel;
import scenarioTWO.agents.EvacResident;

final class DRIVETOActionHandler implements BDIActionHandler {
	private static final Logger logger = LoggerFactory.getLogger("");

	private final BDIModel bdiModel;

	private final MATSimModel model;

	public DRIVETOActionHandler(BDIModel bdiModel, MATSimModel model) {
		this.bdiModel = bdiModel;
		this.model = model;
	}

	@Override
	public boolean handle(String agentID, String actionID, Object[] args) {
		// Get nearest link ID and calls the CustomReplanner to map to MATSim.
		Id<Link> newLinkId;
		double[] coords = (double[]) args[1];
		if (args[1] instanceof double[]) {
			newLinkId = ((SearchableNetwork) model.getScenario().getNetwork())
					.getNearestLinkExactly(new Coord(coords[0], coords[1])).getId();
		} else {
			throw new RuntimeException("Destination coordinates are not given");
		}

		final String dest = (String) args[2];
		DRIVETOActionHandler.moveToWaitAtOtherLocation(Id.createPersonId(agentID), newLinkId, dest, model);

		// Now register a event handler for when the agent arrives at the destination
		MATSimAgent agent = model.getBDIAgent(agentID);
		EvacResident bdiAgent = bdiModel.getBDICounterpart(agentID.toString());
		bdiAgent.log("has started driving to coords "+coords[0] + "," + coords[1] 
				+" i.e. link "+newLinkId.toString());
		agent.getPerceptHandler().registerBDIPerceptHandler(
				agent.getAgentID(), 
				MonitoredEventType.ArrivedAtDestination, 
				newLinkId,
				new BDIPerceptHandler() {
					@Override
					public boolean handle(Id<Person> agentId, Id<Link> linkId, MonitoredEventType monitoredEvent, MATSimModel model) {
						MATSimAgent agent = model.getBDIAgent(agentId);
						EvacResident bdiAgent = bdiModel.getBDICounterpart(agentId.toString());
						Object[] params = { linkId.toString() , Long.toString(bdiAgent.getCurrentTime())};

						agent.getActionContainer().register(MATSimActionList.DRIVETO, params);
						// (yyyy probably does not make a difference in terms of current results, but: Shouldn't this be
						// called earlier, maybe around where the replanner is called?  kai, oct'17)
						
						agent.getActionContainer().get(MATSimActionList.DRIVETO).setState(ActionContent.State.PASSED);
						agent.getPerceptContainer().put(MATSimPerceptList.ARRIVED, params);
						return true; //unregister this handler
					}
				});
		return true;
	}

	/*
		final void insertPickupAndWaitAtCurrentLocation(Id<Person> agentId,int pickupTime) {
			// probably never called
			
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
			this.qsim.rescheduleActivityEnd(agent);
		}
		*/
		final static void moveToWaitAtOtherLocation(Id<Person> agentId,Id<Link> newActivityLinkId, String dest, MATSimModel model) {
			// called at least once
			
			logger.debug("agent {} | started addNewLegToPlan method..", agentId);
			double now = model.getTime() ; 
	
			MobsimAgent agent = model.getMobsimAgentMap().get(agentId);
			List<PlanElement> planElements = WithinDayAgentUtils.getModifiablePlan(agent).getPlanElements() ;
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
	//		currentAct.setEndTime(now);
			model.getReplanner().getEditPlans().rescheduleActivityEndtime(agent, currentPlanIndex, now);
	
			//2-insert a leg:
			Leg newLeg = model.getScenario().getPopulation().getFactory().createLeg(TransportMode.car);
			newLeg.setDepartureTime(now);	
			model.getReplanner().getEditRoutes().relocateFutureLegRoute(newLeg, currentAct.getLinkId(), newActivityLinkId,((HasPerson)agent).getPerson() );
			planElements.add(currentPlanIndex+1,newLeg);
			logger.debug(" added a new leg to current index");
	
			//3-ADDING wait ACTIVITY
			Activity newAct = model.getScenario().getPopulation().getFactory().createActivityFromLinkId(dest, newActivityLinkId ) ;
			newAct.setEndTime(Double.POSITIVE_INFINITY);
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
				model.getReplanner().getEditRoutes().relocateFutureLegRoute(nextLeg,newActivityLinkId,nextAct.getLinkId(),((HasPerson)agent).getPerson() ) ; 
				logger.debug("addNewActivityToPlan - leg info after reroute : " + nextLeg.toString());
			}
	
			WithinDayAgentUtils.resetCaches(agent);
	//		this.qsim.rescheduleActivityEnd(agent);
		}
		/*
		final void modifyPlanForDest(Id<Person> agentId,Id<Link> newActivityLinkId, String dest) {
			// probably no longer used.  Essentially changes location (= linkId) for an activity type ("dest")
			
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
					this.qsim.rescheduleActivityEnd(agent);
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
				this.qsim.rescheduleActivityEnd(agent);
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
					this.qsim.rescheduleActivityEnd(agent);
	
					return ;
				}
			}
		}
		*/
}