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
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.agentsoz.bdimatsim.EventsMonitorRegistry.MonitoredEventType;
import io.github.agentsoz.bdimatsim.MATSimActionHandler;
import io.github.agentsoz.bdimatsim.MATSimActionList;
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.bdimatsim.MATSimPerceptList;
import io.github.agentsoz.bdimatsim.PAAgent;
import io.github.agentsoz.bdimatsim.Utils;
import io.github.agentsoz.bdimatsim.app.BDIPerceptHandler;
import io.github.agentsoz.bdimatsim.app.MATSimApplicationInterface;
import io.github.agentsoz.bushfiretute.BDIModel;
import io.github.agentsoz.bushfiretute.Config;
import io.github.agentsoz.bushfiretute.Util;
import io.github.agentsoz.bushfiretute.datacollection.ScenarioTwoData;
import io.github.agentsoz.bushfiretute.shared.ActionID;
import io.github.agentsoz.util.Global;
import scenarioTWO.agents.EvacResident;

public final class ABMModel implements MATSimApplicationInterface {
	static final Logger logger = LoggerFactory.getLogger("");

	private final MATSimModel matsimModel;
	private final BDIModel bdiModel;

	public ABMModel(BDIModel bdiModel) {
		this.bdiModel = bdiModel;
		this.matsimModel = new MATSimModel(bdiModel);
		matsimModel.registerPlugin(this);
	}
	@Override
	public void run(String[] args) {
		org.matsim.core.config.Config config = ConfigUtils.loadConfig( args[0] ) ;
		config.network().setTimeVariantNetwork(true);
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		List<String> bdiAgentIDs = Utils.getBDIAgentIDs( scenario );

		this.bdiModel.init(matsimModel.getAgentManager().getAgentDataContainer(),
				matsimModel.getAgentManager().getAgentStateList(), this.matsimModel,
				bdiAgentIDs.toArray( new String[bdiAgentIDs.size()] ));

		matsimModel.run(args, bdiAgentIDs, scenario);
	}


//	/**
//	 * Use this to pre-process the BDI agents list if needed. For instance, 
//	 * tasks like adding/removing specific agents, or renaming agents IDs, 
//	 * should be done here. This function is called just prior to the
//	 * BDI agent counterparts in MATSim being created.
//	 * 
//	 */
//	@Override
//	public void notifyBeforeCreatingBDICounterparts(List<String> bdiAgentsIDs) {
//	}

	/**
	 * Initialise the BDI agents with any application specific data. This 
	 * function is just immediately after the MATSim counterparts have been 
	 * created. 
	 */
	@Override
	public void notifyAfterCreatingBDICounterparts(List<String> bdiAgentsIDs) {
		// note: it seems better to have the for loop over the agents inside the methods, since then one can create
		// joint infrastructure (e.g. lookup tables) before the agent loop. kai, nov'17

		determineSafeCoordinatesFromMATSimPlans(bdiAgentsIDs);
		// yyyy could now be before matsim start

		assignDependentPersons(bdiAgentsIDs);
		// yyyy could be before matsim start

		registerActions();
		// yyyy I think that this could be done before matsim start

		registerPercepts();
		// yyyy I think that this could be done before matsim start
		
		// yyyy which then means that this whole callback would not be necessary at all any more.
	}
	private void determineSafeCoordinatesFromMATSimPlans(List<String> bdiAgentsIDs) {
//		Map<Id<Link>,? extends Link> links = matsimModel.getScenario().getNetwork().getLinks();
		for (String agentId : bdiAgentsIDs) {
			EvacResident bdiAgent = bdiModel.getBDICounterpart(agentId);
			if (bdiAgent == null) {
				logger.warn("No BDI counterpart for MATSim agent '" + agentId
						+ "'. Should not happen, but will keep going");
				continue;
			}
			//			Plan plan = WithinDayAgentUtils.getModifiablePlan(matsimModel.getMobsimAgentMap().get(agentId));

			Plan plan = matsimModel.getScenario().getPopulation().getPersons().get( Id.createPersonId(agentId) ).getSelectedPlan() ;
			List<PlanElement> planElements = plan.getPlanElements();
			Activity startAct = (Activity) planElements.get(0) ;

			// Assign start location
			//			double lat = links.get(matsimModel.getMobsimAgentMap().get(agentId).getCurrentLinkId()).getFromNode().getCoord().getX();
			//			double lon = links.get(matsimModel.getMobsimAgentMap().get(agentId).getCurrentLinkId()).getFromNode().getCoord().getY();
			Coord startCoord = PopulationUtils.computeCoordFromActivity( startAct, matsimModel.getScenario().getActivityFacilities(), matsimModel.getScenario().getConfig() ) ;
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
	private void registerPercepts() {
		// this is more complex than registerActions because for the percepts we need the linkIDs beforehand. kai, nov'17

		for (String agentID : this.matsimModel.getAgentManager().getBdiAgentIds() ) {
			PAAgent agent1 = this.matsimModel.getAgentManager().getAgent( agentID );
			EvacResident bdiAgent1 = this.bdiModel.getBDICounterpart(agentID.toString());
			Gbl.assertNotNull(bdiAgent1);
			final Coord endCoord = new Coord(bdiAgent1.endLocation[0], bdiAgent1.endLocation[1]);
			Id<Link> newLinkId = NetworkUtils.getNearestLinkExactly(this.matsimModel.getScenario().getNetwork(),
					endCoord).getId();

			agent1.getPerceptHandler().registerBDIPerceptHandler(agent1.getAgentID(),
					MonitoredEventType.ArrivedAtDestination, newLinkId, new BDIPerceptHandler() {
				@Override
				public boolean handle(Id<Person> agentId, Id<Link> linkId, MonitoredEventType monitoredEvent) {
					PAAgent agent = matsimModel.getAgentManager().getAgent( agentId.toString() );
					EvacResident bdiAgent = bdiModel.getBDICounterpart(agentId.toString());
					Object[] params = { "Safe" , Long.toString(bdiAgent.getCurrentTime())};
					agent.getPerceptContainer().put(MATSimPerceptList.ARRIVED, params);
					return true; // unregister this handler
				}
			});
		}
	}
	private void registerActions() {
		for(String agentId1: this.matsimModel.getAgentManager().getBdiAgentIds() ) {
			MATSimActionHandler withHandler = this.matsimModel.getAgentManager().getAgent( agentId1 ).getActionHandler();

			// overwrite default DRIVETO
			withHandler.registerBDIAction(MATSimActionList.DRIVETO, new DRIVETOActionHandler(this.bdiModel, this.matsimModel));

			// register new action
			withHandler.registerBDIAction(ActionID.CONNECT_TO, new CONNECT_TOActionHandler(this.bdiModel, this.matsimModel));

			// register new action
			withHandler.registerBDIAction(ActionID.DRIVETO_AND_PICKUP, new DRIVETO_AND_PICKUPActionHandler(this.bdiModel, this.matsimModel));

			// register new action
			withHandler.registerBDIAction(ActionID.SET_DRIVE_TIME, new SET_DRIVE_TIMEActionHandler(this.bdiModel, this.matsimModel));
		}
	}

	/**
	 * Randomly assign dependent persons to be picked up. Uses 
	 * Pk ({@link Config#getProportionWithKids()}) and 
	 * Pr ({@link Config#getProportionWithRelatives()) probabilities to calculate
	 * normalised probabilities, and then allocate kids and/or relatives
	 * with those probabilities. If both input probabilities are non-zero,
	 * then all four allocations are possible (no kids or relatives, one or the
	 * other, both kids and relatives). 
	 * <p>
	 * Some examples:
	 * <ul> 
	 * <li> Pk=0.0, Pr=0.0: results in always no kids or relatives</li>
	 * <li> Pk=0.0, 0.0&lt;Pr&lt;1.0: results in always relatives</li>
	 * <li> 0.0&lt;Pk&lt;1.0, Pr=0.0: results in always kids</li>
	 * <li> 0.0&lt;Pk&lt;1.0, 0.0&lt;Pr&lt;1.0: results in all four combinations of kids and relatives</li>
	 * </ul>
	 * 
	 * @param bdiAgent
	 */
	private void assignDependentPersons(List<String> bdiAgentsIDs) {
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