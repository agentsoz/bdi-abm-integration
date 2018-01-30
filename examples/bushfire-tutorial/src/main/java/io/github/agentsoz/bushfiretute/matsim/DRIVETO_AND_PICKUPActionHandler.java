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

import io.github.agentsoz.util.evac.ActionList;
import io.github.agentsoz.util.evac.PerceptList;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.network.SearchableNetwork;

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.bdimatsim.EventsMonitorRegistry.MonitoredEventType;
import io.github.agentsoz.bushfiretute.bdi.BDIModel;
import io.github.agentsoz.nonmatsim.BDIActionHandler;
import io.github.agentsoz.nonmatsim.BDIPerceptHandler;
import io.github.agentsoz.nonmatsim.PAAgent;
import io.github.agentsoz.bushfiretute.jack.agents.EvacResident;

public final class DRIVETO_AND_PICKUPActionHandler implements BDIActionHandler {
	private static final Logger logger = Logger.getLogger(DRIVETO_AND_PICKUPActionHandler.class) ;

	private final BDIModel bdiModel;

	private final MATSimModel model;

	public DRIVETO_AND_PICKUPActionHandler(BDIModel bdiModel, MATSimModel model) {
		logger.setLevel(Level.TRACE);
		
		this.bdiModel = bdiModel;
		this.model = model;
	}

	@Override
	public boolean handle(String agentID, String actionID, Object[] args) {
		logger.info("------------------------------------------------------------------------------------------") ;
		Gbl.assertIf( args.length >=4 ) ;
		Gbl.assertIf( args[1] instanceof double[] ) ;
		
		// Get nearest link ID and calls the CustomReplanner to map to MATSim.
		double[] coords = (double[]) args[1];
		Id<Link>	newLinkId = ((SearchableNetwork) model.getScenario().getNetwork())
					.getNearestLinkExactly(new Coord(coords[0], coords[1])).getId();
		
		DRIVETO_AND_PICKUPActionHandler.insertPickupAndWaitAtOtherLocation(Id.createPersonId(agentID), newLinkId, (int) args[3], model);

		// Now register a event handler for when the agent arrives at the destination
		PAAgent agent = model.getAgentManager().getAgent( agentID );
		EvacResident bdiAgent = bdiModel.getBDICounterpart(agentID.toString());
		bdiAgent.log("will drive to pickup from coords "+coords[0] + "," + coords[1] 
				+" i.e. link "+newLinkId.toString());
		
		// Now register a event handler for when the agent arrives and finished picking up the destination
		agent.getPerceptHandler().registerBDIPerceptHandler(
				agent.getAgentID(), 
				MonitoredEventType.EndedActivity, 
				newLinkId.toString(),
				new BDIPerceptHandler() {
					@Override
					public boolean handle(Id<Person> agentId, Id<Link> linkId, MonitoredEventType monitoredEvent) {
						PAAgent agent = model.getAgentManager().getAgent( agentId.toString() );
						Object[] params = { linkId.toString() };

						agent.getActionContainer().register(ActionList.DRIVETO_AND_PICKUP, params);
						// (yyyy probably does not make a difference in terms of current results, but: Shouldn't this be
						// called earlier, maybe around where the replanner is called?  kai, oct'17)

						agent.getActionContainer().get(ActionList.DRIVETO_AND_PICKUP).setState(ActionContent.State.PASSED);
						agent.getPerceptContainer().put(PerceptList.ARRIVED_AND_PICKED_UP, params);
						return true; //unregister this handler
					}
				});
		logger.info("------------------------------------------------------------------------------------------") ;
//		System.exit(-1) ;
		return true;
	}
	
	/**
	 * Look into {@link DRIVETOActionHandler} for syntax that does not need the indices. kai, jan'18
	 */
	private static final void insertPickupAndWaitAtOtherLocation(Id<Person> agentId, Id<Link> newActivityLinkId, int pickupTime, MATSimModel model) {
		// called at least once
		
		logger.debug("starting addNewLegAndActvityToPlan method..");
		double now = model.getTime();
		MobsimAgent agent = model.getMobsimDataProvider().getAgents().get(agentId);
		
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
		
		int currentPlanIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
		
		logger.trace("number of plan elements: " + plan.getPlanElements().size());
		logger.trace("current plan index : " + currentPlanIndex);
		
		PlanElement currentPE = plan.getPlanElements().get(currentPlanIndex);
		if (!(currentPE instanceof Activity)) {
			return;
		}
		Activity currentAct = (Activity) currentPE;
		if (currentAct.getType().equals("Wait")) {
			// yyyy why only for wait activity here, and for all activities in compagnon method?  kai, oct'17
			//ending the current activity if its a Wait activity:
			logger.debug("Wait/Current activity ending now:  {}" + now);
			currentAct.setEndTime(now);
		}
		
		final PopulationFactory pf = model.getScenario().getPopulation().getFactory();
		
		// leg/route from current activity/position to pickup activity:
		Leg newLeg = pf.createLeg(TransportMode.car);
		model.getReplanner().editRoutes().relocateFutureLegRoute(newLeg, currentAct.getLinkId(), newActivityLinkId, ((HasPerson) agent).getPerson());
		logger.debug(" inserting leg into plan..");
		plan.getPlanElements().add(currentPlanIndex + 1, newLeg);
		
		//ADDING Pickup ACTIVITY
		Activity pickupAct = pf.createActivityFromLinkId("Pickup", newActivityLinkId);
		pickupAct.setMaximumDuration(pickupTime);
		logger.debug(" added {} type activity" + pickupAct.getType());
		plan.getPlanElements().add(currentPlanIndex + 2, pickupAct);
		
		//ADDING Wait ACTIVITY
		Activity waitAct = pf.createActivityFromLinkId("Wait", newActivityLinkId);
//		waitAct.setEndTime( Double.POSITIVE_INFINITY ) ;
		waitAct.setEndTime(Double.MAX_VALUE);
		logger.debug(" added {} type activity with MAX_VALUE end time.." + waitAct.getType());
		plan.getPlanElements().add(currentPlanIndex + 3, waitAct);
		
		//rerouting the leg after wait activity
		logger.debug("reRouting the leg after wait activity");
		Leg nextLeg = (Leg) plan.getPlanElements().get(currentPlanIndex + 4);
		Activity nextAct = (Activity) plan.getPlanElements().get(currentPlanIndex + 5);
		logger.trace("all evac activity info : {} " + nextAct.toString());
		model.getReplanner().editRoutes().relocateFutureLegRoute(nextLeg, newActivityLinkId, nextAct.getLinkId(), ((HasPerson) agent).getPerson());
		
		logger.trace("number of plan elements after adding pickup, wait, new leg : " + plan.getPlanElements().size());
		
		logger.trace("plan=" + plan);
		for (PlanElement pe : plan.getPlanElements()) {
			logger.trace(pe.toString());
		}
	
		WithinDayAgentUtils.resetCaches(agent);
		model.getReplanner().editPlans().rescheduleActivityEnd(agent);
	
	}
}