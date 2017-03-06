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
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.qsim.ActivityEndRescheduler;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.agentsoz.bdimatsim.AgentActivityEventHandler.MonitoredEventType;
import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdimatsim.MATSimActionHandler;
import io.github.agentsoz.bdimatsim.MATSimActionList;
import io.github.agentsoz.bdimatsim.MATSimAgent;
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.bdimatsim.MatsimPerceptHandler;
import io.github.agentsoz.bdimatsim.MatsimPerceptList;
import io.github.agentsoz.bdimatsim.Replanner;
import io.github.agentsoz.bdimatsim.app.BDIActionHandler;
import io.github.agentsoz.bdimatsim.app.MATSimApplicationInterface;
import io.github.agentsoz.bdimatsim.app.BDIPerceptHandler;
import io.github.agentsoz.bushfiretute.BDIModel;
import io.github.agentsoz.bushfiretute.BushfireMain;
import io.github.agentsoz.bushfiretute.Config;
import io.github.agentsoz.bushfiretute.MATSimBDIParameterHandler;
import io.github.agentsoz.bushfiretute.Util;
import io.github.agentsoz.bushfiretute.datacollection.ScenarioTwoData;
import io.github.agentsoz.bushfiretute.shared.ActionID;
import io.github.agentsoz.bushfiretute.shared.PerceptID;
import scenarioTWO.agents.EvacResident;

public class ABMModel implements MATSimApplicationInterface {

    final Logger logger = LoggerFactory.getLogger("");      
    final MATSimModel model;
    final BDIModel bdiModel;
    private Replanner replanner = null;;
    
	public ABMModel(BDIModel bdiModel, MATSimBDIParameterHandler matSimBDIParameterHandler) {
		this.bdiModel = bdiModel;
		this.model = new MATSimModel(bdiModel, new MATSimBDIParameterHandler());
		model.registerPlugin(this);
	}
	
	/**
	 * Provides a custom Replanner (extended) to use with MATSim.
	 */
	@Override
	public Replanner getReplanner(ActivityEndRescheduler activityEndRescheduler) {
		if (replanner == null) {
			replanner = new CustomReplanner(model, activityEndRescheduler);
		}
		return replanner;
	}

	/**
	 * Use this to pre-process the BDI agents list if needed. For instance, 
	 * tasks like adding/removing specific agents, or renaming agents IDs, 
	 * should be done here. This function is called just prior to the
	 * BDI agent counterparts in MATSim being created.
	 * 
	 */
	@Override
	public void notifyBeforeCreatingBDICounterparts(List<Id<Person>> bdiAgentsIDs) {
	}

	/**
	 * Initialise the BDI agents with any application specific data. This 
	 * function is just immediately after the MATSim counterparts have been 
	 * created. 
	 */
	@Override
	public void notifyAfterCreatingBDICounterparts(List<Id<Person>> bdiAgentsIDs) {
		
		Map<Id<Link>,? extends Link> links = model.getScenario().getNetwork().getLinks();
		for (Id<Person> agentId : bdiAgentsIDs) {
			@SuppressWarnings("unused")
			MATSimAgent agent = model.getBDIAgent(agentId);
			EvacResident bdiAgent = bdiModel.getBDICounterpart(agentId.toString());
			if (bdiAgent == null) {
				logger.warn("No BDI counterpart for MATSim agent '" + agentId
					+ "'. Should not happen, but will keep going");
				continue;
			}
			Plan plan = WithinDayAgentUtils.getModifiablePlan(model.getMobsimAgentMap().get(agentId));
			List<PlanElement> planElements = plan.getPlanElements();

			// Assign start location
			double lat = links.get(model.getMobsimAgentMap().get(agentId).getCurrentLinkId()).getFromNode().getCoord().getX();
			double lon = links.get(model.getMobsimAgentMap().get(agentId).getCurrentLinkId()).getFromNode().getCoord().getY();
			bdiAgent.startLocation = new double[] { lat, lon };
			bdiAgent.currentLocation = "home"; // agents always start at home
			logger.trace("agent {} is at home at location {},{}", 
					agentId, lon, lat);

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
					logger.trace("agent {} end location is {},{}", 
						agentId, bdiAgent.endLocation[0], bdiAgent.endLocation[1]);
				}
			}
			
			// Assign dependent persons (to pick up before evacuating)
			assignDependentPersons(bdiAgent);
		}
	}
	
	/**
	 * This is where we register all application specific BDI actions, and/or
	 * overwrite default ones (like {@link MATSimActionList#DRIVETO}). 
	 * <p>
	 * This is also the place to register action-dependent percepts. 
	 * For instance, {@link MatsimPerceptList.ARRIVED} is conditional on the 
	 * agent arriving at the network link in action 
	 * {@link MATSimActionList.DRIVETO}, and so must be registered at the 
	 * same time.
	 * <p>
	 * Action-independent percepts should be registered using
	 * {@link this#registerNewBDIPercepts(MatsimPerceptHandler)}.
	 * <p>
	 * Note that actions/percepts are registered <strong>per agent</strong>,
	 * i.e. handlers passed in belong to specific agents.
	 */

	@Override
	public void registerNewBDIActions(MATSimActionHandler withHandler) {
		// overwrite default DRIVETO
		withHandler.registerBDIAction(MATSimActionList.DRIVETO, new BDIActionHandler() {
			@Override
			public boolean handle(String agentID, String actionID, Object[] args, MATSimModel model) {
				logger.debug("found DRIVETO action for agent : " + agentID);
				// Get nearest link ID and calls the CustomReplanner to map to MATSim.
				Id<Link> newLinkId;
				if (args[1] instanceof double[]) {
					double[] coords = (double[]) args[1];
					logger.debug("received coords for driveTo : " + coords[0] + " " + coords[1]);
					newLinkId = ((NetworkImpl) model.getScenario().getNetwork())
							.getNearestLinkExactly(new CoordImpl(coords[0], coords[1])).getId();
					logger.debug("generated link Id for driveTo : " + newLinkId.toString());
				} else {
					throw new RuntimeException("Destination coordinates are not given");
				}

				String destination = (String) args[2];
				// this.matSimModel.getReplanner().attachNewActivityAtEndOfPlan(
				// newLinkId, Id.createPersonId( agentID ));

				// this.matSimModel.getReplanner().addNewLegToPlan(
				// Id.createPersonId( agentID ),newLinkId, destination);
				
				((CustomReplanner)model.getReplanner()).addNewLegToPlan(Id.createPersonId(agentID), newLinkId, destination);

				// Now register a event handler for when the agent arrives at the destination
				MATSimAgent agent = model.getBDIAgent(agentID);
				agent.getPerceptHandler().registerBDIPerceptHandler(
						agent.getAgentID(), 
						MonitoredEventType.ArrivedAtDestination, 
						newLinkId,
						new BDIPerceptHandler() {
							@Override
							public boolean handle(Id<Person> agentId, Id<Link> linkId, MonitoredEventType monitoredEvent, MATSimModel model) {
								MATSimAgent agent = model.getBDIAgent(agentId);
								Object[] params = { linkId.toString() };
								agent.getActionContainer().register(MATSimActionList.DRIVETO, params);
								agent.getActionContainer().get(MATSimActionList.DRIVETO).setState(ActionContent.State.PASSED);
								agent.getPerceptContainer().put(MatsimPerceptList.ARRIVED, params);
								return true; //unregister this handler
							}
						});
				return true;
			}
		});
		
		// register new action
		withHandler.registerBDIAction(ActionID.CONNECT_TO, new BDIActionHandler() {
			@Override
			public boolean handle(String agentID, String actionID, Object[] args, MATSimModel model) {
				logger.debug("found CONNECT_TO action for agent : " + agentID);
				String destination = (String) args[1];
				// connect To route replanner method
				Id<Link> newLinkId = ((CustomReplanner)model.getReplanner()).replanCurrentRoute(Id.createPersonId(agentID), destination);
				if (newLinkId == null) {
					logger.debug("CONNECT_TO: returned a null link from the target activity");
					return true;
				}
				// Now register a event handler for when the agent arrives at the destination
				MATSimAgent agent = model.getBDIAgent(agentID);
				agent.getPerceptHandler().registerBDIPerceptHandler(
						agent.getAgentID(), 
						MonitoredEventType.ArrivedAtDestination, 
						newLinkId,
						new BDIPerceptHandler() {
							@Override
							public boolean handle(Id<Person> agentId, Id<Link> linkId, MonitoredEventType monitoredEvent, MATSimModel model) {
								MATSimAgent agent = model.getBDIAgent(agentId);
								Object[] params = { linkId.toString() };
								agent.getActionContainer().register(ActionID.CONNECT_TO, params);
								agent.getActionContainer().get(ActionID.CONNECT_TO).setState(ActionContent.State.PASSED);
								agent.getPerceptContainer().put(PerceptID.ARRIVED_CONNECT_TO, params);
								return true; //unregister this handler
							}
						});
				return true;
			}
		});

		// register new action
		withHandler.registerBDIAction(ActionID.DRIVETO_AND_PICKUP, new BDIActionHandler() {
			@Override
			public boolean handle(String agentID, String actionID, Object[] args, MATSimModel model) {
				// Get nearest link ID and calls the CustomReplanner to map to MATSim.
				Id<Link> newLinkId;
				if (args[1] instanceof double[]) {
					double[] coords = (double[]) args[1];
					logger.trace("received coords for driveToAndPickUp: " + coords[0] + " " + coords[1]);

					newLinkId = ((NetworkImpl) model.getScenario().getNetwork())
							.getNearestLinkExactly(new CoordImpl(coords[0], coords[1])).getId();
					logger.debug("driveToAndPickUp: agentID {} | received coords - {} {} | generated link Id {} ",
							agentID, coords[0], coords[1], newLinkId.toString());
				} else {
					throw new RuntimeException("Destination coordinates are not given");
				}

				int pickupTime = (int) args[3];
				((CustomReplanner)model.getReplanner()).addNewLegAndActvityToPlan(Id.createPersonId(agentID), newLinkId, pickupTime);
				logger.trace(" finished calling addLegAndActvityToNextIndex method in CustomReplanner");

				// Now register a event handler for when the agent arrives at the destination
				MATSimAgent agent = model.getBDIAgent(agentID);
				// Now register a event handler for when the agent arrives and finished picking up the destination
				agent.getPerceptHandler().registerBDIPerceptHandler(
						agent.getAgentID(), 
						MonitoredEventType.EndedActivity, 
						newLinkId,
						new BDIPerceptHandler() {
							@Override
							public boolean handle(Id<Person> agentId, Id<Link> linkId, MonitoredEventType monitoredEvent, MATSimModel model) {
								MATSimAgent agent = model.getBDIAgent(agentId);
								Object[] params = { linkId.toString() };
								agent.getActionContainer().register(ActionID.DRIVETO_AND_PICKUP, params);
								agent.getActionContainer().get(ActionID.DRIVETO_AND_PICKUP).setState(ActionContent.State.PASSED);
								agent.getPerceptContainer().put(PerceptID.ARRIVED_AND_PICKED_UP, params);
								return true; //unregister this handler
							}
						});
				return true;
			}
		});

		/*
		// register new action
		withHandler.registerBDIAction(ActionID.PICKUP, new BDIActionHandler() {
			@Override
			public boolean handle(String agentID, String actionID, Object[] args, MATSimModel model) {
				int pickupTime = (int) args[1];

				Id<Link> newLinkId; // just to distinguish PICKUP actions when
									// returning the action state
				if (args[2] instanceof double[]) {
					double[] coords = (double[]) args[2];
					logger.trace("received coords for driveToAndPickUp: " + coords[0] + " " + coords[1]);
					newLinkId = ((NetworkImpl) model.getScenario().getNetwork())
							.getNearestLinkExactly(new CoordImpl(coords[0], coords[1])).getId();
				} else {
					throw new RuntimeException("Destination coordinates are not given");
				}

				logger.debug("PICKUP: agentID {} | newLinkId: {} | pickupTime: {} ", agentID, newLinkId, pickupTime);
				((CustomReplanner)model.getReplanner()).addNewActivityToPlan(Id.createPersonId(agentID), pickupTime);
				logger.trace(" finished calling addNewActivityToPlan method in CustomReplanner");

				MATSimAgent agent = model.getBDIAgent(agentID);
				agent.newDriveTo(newLinkId);
				agent.newPickUp(newLinkId);

				return true;
			}
		});
		*/
		
		// register new action
		withHandler.registerBDIAction(ActionID.SET_DRIVE_TIME, new BDIActionHandler() {
			@Override
			public boolean handle(String agentID, String actionID, Object[] args, MATSimModel model) {
                double newEndTime = (double) args[1];
                String actType = (String) args[2];        
                logger.debug("setDriveTime: agentID {} | actType  {} | delayTime {} ",agentID,actType,newEndTime);
                ((CustomReplanner)model.getReplanner()).forceEndActivity(Id.createPersonId( agentID ),actType, newEndTime);

				// Now set the action to passed straight away
				MATSimAgent agent = model.getBDIAgent(agentID);
				Object[] params = {};
				agent.getActionContainer().register(ActionID.SET_DRIVE_TIME, params);
				agent.getActionContainer().get(ActionID.SET_DRIVE_TIME).setState(ActionContent.State.PASSED);
                return true;
			}
		});
	}

	/**
	 * Register any action-independent percepts here. Percepts that are
	 * conditional on actions (such as {@link MatsimPerceptList#ARRIVED} that
	 * is specific {@link ActionID#DRIVETO}
	 */
	@Override
	public void registerNewBDIPercepts(MatsimPerceptHandler withHandler) {
		// TODO Auto-generated method stub
		
	}

	
	public void run(String file, String[] args) {
		model.run(file, args);
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
	 * @param agent
	 */
	private void assignDependentPersons(EvacResident agent) {
		if( ScenarioTwoData.totPickups <= Config.getMaxPickUps() ) {
		    double[] pDependents = {Config.getProportionWithKids(), Config.getProportionWithRelatives()};
		    pDependents = Util.normalise(pDependents);
		    Random random = BushfireMain.getRandom();
		    
		    if (random.nextDouble() < pDependents[0]) {
		    	// Allocate dependent children
				ScenarioTwoData.agentsWithKids++;
				double[] sclCords = Config.getRandomSchoolCoords(agent.getId(),agent.startLocation);
				if(sclCords != null) { 
					agent.kidsNeedPickUp = true;  
					agent.schoolLocation = sclCords;
					agent.prepared_to_evac_flag = false;
					ScenarioTwoData.totPickups++;
					logger.debug("agent {} has kids |"
							+ " school location: {} {} |", agent.getId(), sclCords[0], sclCords[1]);
				}
				else{
					logger.debug("no school found for agent {}  assigned with kids ", agent.getId());
					ScenarioTwoData.agentsWithKidsNoSchools++;
				}
		    }
		    if (random.nextDouble() < pDependents[1]) {
		    	// Allocate dependent adults
				ScenarioTwoData.agentsWithRels++;
				agent.relsNeedPickUp = true;
				agent.prepared_to_evac_flag = false;
				ScenarioTwoData.totPickups++;
				logger.debug(" agent {} has rels", agent.getId());
		    }
		}
	}

}