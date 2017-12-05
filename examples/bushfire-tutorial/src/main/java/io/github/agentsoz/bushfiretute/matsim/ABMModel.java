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



import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.agentsoz.bdimatsim.EventsMonitorRegistry.MonitoredEventType;
import io.github.agentsoz.util.evac.ActionList;
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.util.evac.PerceptList;
import io.github.agentsoz.bdimatsim.Utils;
import io.github.agentsoz.bushfiretute.Config;
import io.github.agentsoz.bushfiretute.Util;
import io.github.agentsoz.bushfiretute.bdi.BDIModel;
import io.github.agentsoz.bushfiretute.datacollection.ScenarioTwoData;
import io.github.agentsoz.bushfiretute.shared.ActionID;
import io.github.agentsoz.nonmatsim.ActionHandler;
import io.github.agentsoz.nonmatsim.BDIPerceptHandler;
import io.github.agentsoz.nonmatsim.PAAgent;
import io.github.agentsoz.util.Global;
import scenarioTWO.agents.EvacResident;

public final class ABMModel  {
	static final Logger logger = LoggerFactory.getLogger("");

	private final MATSimModel matsimModel;
	private final BDIModel bdiModel;

	public ABMModel(BDIModel bdiModel, String[] args) {
		this.bdiModel = bdiModel;
		this.matsimModel = new MATSimModel(args);
	}
	public void run() {
		Scenario scenario = matsimModel.loadAndPrepareScenario();;
		
		List<String> bdiAgentIDs = Utils.getBDIAgentIDs( scenario );

		this.bdiModel.init(matsimModel.getAgentManager().getAgentDataContainer(),
				null, this.matsimModel,
				bdiAgentIDs.toArray( new String[bdiAgentIDs.size()] ));

		matsimModel.init( bdiAgentIDs);
		
		determineSafeCoordinatesFromMATSimPlans(bdiAgentIDs, bdiModel, matsimModel.getScenario() );

		assignDependentPersons(bdiAgentIDs, bdiModel);

		registerActionsWithAgents(bdiModel, matsimModel);

		registerPerceptsWithAgents(bdiModel, matsimModel);
		
		while ( true ) {
			this.bdiModel.takeControl( matsimModel.getAgentManager().getAgentDataContainer() );
//			this.matsimModel.takeControl(matsimModel.getAgentManager().getAgentDataContainer());
			if( this.matsimModel.isFinished() ) {
				break ;
			}
			this.matsimModel.runUntil(Global.getTime()+1, matsimModel.getAgentManager().getAgentDataContainer());
		}
		
		this.matsimModel.finish() ;
		this.bdiModel.finish();
	}

	private static void determineSafeCoordinatesFromMATSimPlans(List<String> bdiAgentsIDs, BDIModel bdiModel, Scenario scenario) {
//		Map<Id<Link>,? extends Link> links = matsimModel.getScenario().getNetwork().getLinks();
		for (String agentId : bdiAgentsIDs) {
			EvacResident bdiAgent = bdiModel.getBDICounterpart(agentId);
			if (bdiAgent == null) {
				logger.warn("No BDI counterpart for MATSim agent '" + agentId
						+ "'. Should not happen, but will keep going");
				continue;
			}
			//			Plan plan = WithinDayAgentUtils.getModifiablePlan(matsimModel.getMobsimAgentMap().get(agentId));

			Plan plan = scenario.getPopulation().getPersons().get( Id.createPersonId(agentId) ).getSelectedPlan() ;
			List<PlanElement> planElements = plan.getPlanElements();
			Activity startAct = (Activity) planElements.get(0) ;

			// Assign start location
			//			double lat = links.get(matsimModel.getMobsimAgentMap().get(agentId).getCurrentLinkId()).getFromNode().getCoord().getX();
			//			double lon = links.get(matsimModel.getMobsimAgentMap().get(agentId).getCurrentLinkId()).getFromNode().getCoord().getY();
			Coord startCoord = PopulationUtils.computeCoordFromActivity( startAct, scenario.getActivityFacilities(), scenario.getConfig() ) ;
			double lat = startCoord.getX() ;
			double lon = startCoord.getY();

			bdiAgent.startLocation = new double[] { lat, lon };
			// yy this is just used for finding school locations, so startLocation is a slight mis-nomer; it needs the home location. kai, nov'17

			bdiAgent.currentLocation = "home"; // agents always start at home
			bdiAgent.log("is at home at location "+lon+","+lat);

			for (int i = 0; i < planElements.size(); i++) {
				PlanElement element = planElements.get(i);
				if (!(element instanceof Activity)) {
					continue;
				}
				Activity act = (Activity) element;
				// Get departure time
				if (act.getType().equals("Evacuation")) {
					PlanElement pe = plan.getPlanElements().get(i + 1);
					if (!(pe instanceof Leg)) {
						logger.error("Utils : selected plan element to get deptime is not a leg");
						continue;
					}
					Leg depLeg = (Leg) pe;
					double depTime = depLeg.getDepartureTime();
					logger.trace("departure time of the depLeg : {}", depTime);
					bdiAgent.setDepTime(depTime);

				}
				// Assign coords of safe destination
				if (act.getType().equals("Safe")) {
					double safeX = act.getCoord().getX();
					double safeY = act.getCoord().getY();
					bdiAgent.endLocation = new double[] { safeX, safeY };
					bdiAgent.log("safe location is at "+safeX+","+safeY);
				}
			}
		}
	}
	private static void registerPerceptsWithAgents ( final BDIModel bdiModel, final MATSimModel matsimModel ) {
		// this is more complex than registerActions because for the percepts we need the linkIDs beforehand. kai, nov'17

		for (String agentID : matsimModel.getAgentManager().getBdiAgentIds() ) {
			PAAgent agent1 = matsimModel.getAgentManager().getAgent( agentID );
			EvacResident bdiAgent1 = bdiModel.getBDICounterpart(agentID.toString());
			Gbl.assertNotNull(bdiAgent1);
			final Coord endCoord = new Coord(bdiAgent1.endLocation[0], bdiAgent1.endLocation[1]);
			Id<Link> newLinkId = NetworkUtils.getNearestLinkExactly(matsimModel.getScenario().getNetwork(),
					endCoord).getId();

			agent1.getPerceptHandler().registerBDIPerceptHandler(agent1.getAgentID(),
					MonitoredEventType.ArrivedAtDestination, newLinkId.toString(), new BDIPerceptHandler() {
				@Override
				public boolean handle(Id<Person> agentId, Id<Link> linkId, MonitoredEventType monitoredEvent) {
					PAAgent agent = matsimModel.getAgentManager().getAgent( agentId.toString() );
					EvacResident bdiAgent = bdiModel.getBDICounterpart(agentId.toString());
					Object[] params = { "Safe" , Long.toString(bdiAgent.getCurrentTime())};
					agent.getPerceptContainer().put(PerceptList.ARRIVED, params);
					return true; // unregister this handler
				}
			});
		}
	}
	private static void registerActionsWithAgents(final BDIModel bdiModel, final MATSimModel matsimModel ) {
		for(String agentId1: matsimModel.getAgentManager().getBdiAgentIds() ) {
			
			ActionHandler withHandler = matsimModel.getAgentManager().getAgent( agentId1 ).getActionHandler();

			// overwrite default DRIVETO
			withHandler.registerBDIAction(ActionList.DRIVETO, new DRIVETOActionHandler(bdiModel, matsimModel));

			// register new action
			withHandler.registerBDIAction(ActionID.CONNECT_TO, new CONNECT_TOActionHandler(bdiModel, matsimModel));

			// register new action
			withHandler.registerBDIAction(ActionID.DRIVETO_AND_PICKUP, new DRIVETO_AND_PICKUPActionHandler(bdiModel, matsimModel));

			// register new action
			withHandler.registerBDIAction(ActionID.SET_DRIVE_TIME, new SET_DRIVE_TIMEActionHandler(bdiModel, matsimModel));
		}
	}

	/**
	 * Randomly assign dependent persons to be picked up. Uses 
	 * Pk ({@link Config#getProportionWithKids()}) and 
	 * Pr ({@link
	 * @param bdiModel TODO
	 * @param bdiAgent
	 */
	private static void assignDependentPersons(List<String> bdiAgentsIDs, BDIModel bdiModel) {
		for (String agentId : bdiAgentsIDs) {
			EvacResident bdiAgent = bdiModel.getBDICounterpart(agentId);
			if( ScenarioTwoData.totPickups <= Config.getMaxPickUps() ) {
				double[] pDependents = {Config.getProportionWithKids(), Config.getProportionWithRelatives()};
				pDependents = Util.normalise(pDependents);
				Random random = Global.getRandom();

				if (random.nextDouble() < pDependents[0]) {
					// Allocate dependent children
					ScenarioTwoData.agentsWithKids++;
					double[] sclCords = Config.getRandomSchoolCoords(bdiAgent.getId(),bdiAgent.startLocation);
					if(sclCords != null) { 
						bdiAgent.kidsNeedPickUp = true;  
						bdiAgent.schoolLocation = sclCords;
						bdiAgent.prepared_to_evac_flag = false;
						ScenarioTwoData.totPickups++;
						bdiAgent.log("has children at school coords " 
								+ sclCords[0] + "," +sclCords[1]);
					}
					else{
						bdiAgent.log("has children but there are no schools nearby");
						ScenarioTwoData.agentsWithKidsNoSchools++;
					}
				}
				if (random.nextDouble() < pDependents[1]) {
					// Allocate dependent adults
					ScenarioTwoData.agentsWithRels++;
					bdiAgent.relsNeedPickUp = true;
					bdiAgent.prepared_to_evac_flag = false;
					ScenarioTwoData.totPickups++;
					bdiAgent.log("has relatives");
				}
				if (!bdiAgent.relsNeedPickUp && !bdiAgent.kidsNeedPickUp) {
					bdiAgent.log("has neither children nor relatives");
				}
			}
		}
	}

}