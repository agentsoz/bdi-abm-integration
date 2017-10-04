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

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdimatsim.AgentActivityEventHandler.MonitoredEventType;
import io.github.agentsoz.bdimatsim.*;
import io.github.agentsoz.bdimatsim.app.BDIActionHandler;
import io.github.agentsoz.bdimatsim.app.BDIPerceptHandler;
import io.github.agentsoz.bdimatsim.app.MATSimApplicationInterface;
import io.github.agentsoz.bushfiretute.*;
import io.github.agentsoz.bushfiretute.datacollection.ScenarioTwoData;
import io.github.agentsoz.bushfiretute.shared.ActionID;
import io.github.agentsoz.bushfiretute.shared.PerceptID;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.mobsim.qsim.ActivityEndRescheduler;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scenarioTWO.agents.EvacResident;

import java.util.List;
import java.util.Map;
import java.util.Random;

public final class ABMModel implements MATSimApplicationInterface {

	private final Logger logger = LoggerFactory.getLogger("");
	private final MATSimModel matsimModel;
	private final BDIModel bdiModel;
	private Replanner replanner = null;

	public ABMModel(BDIModel bdiModel) {
		this.bdiModel = bdiModel;
		this.matsimModel = new MATSimModel(bdiModel);
		matsimModel.registerPlugin(this);
	}

	/**
	 * Provides a custom Replanner (extended) to use with MATSim.
	 */
	@Override
	public Replanner getReplanner(ActivityEndRescheduler activityEndRescheduler) {
		if (replanner == null) {
			replanner = new CustomReplanner(matsimModel, activityEndRescheduler);
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

		Map<Id<Link>,? extends Link> links = matsimModel.getScenario().getNetwork().getLinks();
		for (Id<Person> agentId : bdiAgentsIDs) {
			@SuppressWarnings("unused")
			MATSimAgent agent = matsimModel.getBDIAgent(agentId);
			EvacResident bdiAgent = bdiModel.getBDICounterpart(agentId.toString());
			if (bdiAgent == null) {
				logger.warn("No BDI counterpart for MATSim agent '" + agentId
						+ "'. Should not happen, but will keep going");
				continue;
			}
			Plan plan = WithinDayAgentUtils.getModifiablePlan(matsimModel.getMobsimAgentMap().get(agentId));
			List<PlanElement> planElements = plan.getPlanElements();

			// Assign start location
			double lat = links.get(matsimModel.getMobsimAgentMap().get(agentId).getCurrentLinkId()).getFromNode().getCoord().getX();
			double lon = links.get(matsimModel.getMobsimAgentMap().get(agentId).getCurrentLinkId()).getFromNode().getCoord().getY();
			bdiAgent.startLocation = new double[] { lat, lon };
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

			// Assign dependent persons (to pick up before evacuating)
			assignDependentPersons(bdiAgent);
		}
	}

	/**
	 * This is where we register all application specific BDI actions, and/or
	 * overwrite default ones (like {@link MATSimActionList#DRIVETO}). 
	 * <p>
	 * This is also the place to register action-dependent percepts. 
	 * For instance, {@link MATSimPerceptList.ARRIVED} is conditional on the 
	 * agent arriving at the network link in action 
	 * {@link MATSimActionList.DRIVETO}, and so must be registered at the 
	 * same time.
	 * <p>
	 * Action-independent percepts should be registered using
	 * {@link this#registerNewBDIPercepts(MATSimPerceptHandler)}.
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
				// Get nearest link ID and calls the CustomReplanner to map to MATSim.
				Id<Link> newLinkId;
				double[] coords = (double[]) args[1];
				if (args[1] instanceof double[]) {
					newLinkId = ((NetworkImpl) model.getScenario().getNetwork())
							.getNearestLinkExactly(new CoordImpl(coords[0], coords[1])).getId();
				} else {
					throw new RuntimeException("Destination coordinates are not given");
				}

				((CustomReplanner)model.getReplanner()).addNewLegToPlan(Id.createPersonId(agentID), newLinkId, (String) args[2]);

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
								agent.getActionContainer().get(MATSimActionList.DRIVETO).setState(ActionContent.State.PASSED);
								agent.getPerceptContainer().put(MATSimPerceptList.ARRIVED, params);
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
				String destination = (String) args[1];
				// connect To route replanner method
				Id<Link> newLinkId = ((CustomReplanner)model.getReplanner()).replanCurrentRoute(Id.createPersonId(agentID), destination);
				if (newLinkId == null) {
					logger.warn("CONNECT_TO: returned a null link from the target activity");
					return true;
				}
				// Now register a event handler for when the agent arrives at the destination
				MATSimAgent agent = model.getBDIAgent(agentID);
				EvacResident bdiAgent = bdiModel.getBDICounterpart(agentID.toString());
				bdiAgent.log("replanned to drive to connecting link " + newLinkId.toString());
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
				double[] coords = (double[]) args[1];
				if (args[1] instanceof double[]) {
					newLinkId = ((NetworkImpl) model.getScenario().getNetwork())
							.getNearestLinkExactly(new CoordImpl(coords[0], coords[1])).getId();
				} else {
					throw new RuntimeException("Destination coordinates are not given");
				}

				((CustomReplanner)model.getReplanner()).addNewLegAndActvityToPlan(Id.createPersonId(agentID), newLinkId, (int) args[3]);

				// Now register a event handler for when the agent arrives at the destination
				MATSimAgent agent = model.getBDIAgent(agentID);
				EvacResident bdiAgent = bdiModel.getBDICounterpart(agentID.toString());
				bdiAgent.log("will drive to pickup from coords "+coords[0] + "," + coords[1] 
						+" i.e. link "+newLinkId.toString());

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

		// register new action
		withHandler.registerBDIAction(ActionID.SET_DRIVE_TIME, new BDIActionHandler() {
			@Override
			public boolean handle(String agentID, String actionID, Object[] args, MATSimModel model) {
				double newEndTime = (double) args[1];
				String actType = (String) args[2];        

				((CustomReplanner)model.getReplanner()).forceEndActivity(Id.createPersonId( agentID ),actType, newEndTime);

				// Now set the action to passed straight away
				MATSimAgent agent = model.getBDIAgent(agentID);
				EvacResident bdiAgent = bdiModel.getBDICounterpart(agentID.toString());
				bdiAgent.log("has set the drive time for activity " + actType + " to " + newEndTime);
				Object[] params = {};
				agent.getActionContainer().register(ActionID.SET_DRIVE_TIME, params);
				agent.getActionContainer().get(ActionID.SET_DRIVE_TIME).setState(ActionContent.State.PASSED);
				return true;
			}
		});
	}

	/**
	 * Register any action-independent percepts here. Percepts that are
	 * conditional on actions (such as {@link MATSimPerceptList#ARRIVED} that
	 * is specific {@link ActionID#DRIVETO}
	 */
	@Override
	public void registerNewBDIPercepts(MATSimPerceptHandler withHandler) {
		// For all agents, register a percept for when they arrive at the safe
		// destination. We do this here, irrespective of whether there is any
		// BDI reasoning involved, i.e., where the percept is not conditional
		// on a BDI (drive) action. 
		// Such as for MATSim agents that leave as
		// planned (according to their MATSim plan). 
		// FIXME: add Safe arrival percept for all agents (in a for loop)

		for (Id<Person> agentID : matsimModel.getBDIAgentIDs()) {
			MATSimAgent agent = matsimModel.getBDIAgent(agentID);
			EvacResident bdiAgent = bdiModel.getBDICounterpart(agentID.toString());
			Id<Link> newLinkId;
			newLinkId = ((NetworkImpl) matsimModel.getScenario().getNetwork())
					.getNearestLinkExactly(new CoordImpl(bdiAgent.endLocation[0], bdiAgent.endLocation[1])).getId();

			agent.getPerceptHandler().registerBDIPerceptHandler(agent.getAgentID(),
					MonitoredEventType.ArrivedAtDestination, newLinkId, new BDIPerceptHandler() {
				@Override
				public boolean handle(Id<Person> agentId, Id<Link> linkId, MonitoredEventType monitoredEvent,
						MATSimModel model) {
					MATSimAgent agent = model.getBDIAgent(agentId);
					EvacResident bdiAgent = bdiModel.getBDICounterpart(agentId.toString());
					Object[] params = { "Safe" , Long.toString(bdiAgent.getCurrentTime())};
					agent.getPerceptContainer().put(MATSimPerceptList.ARRIVED, params);
					return true; // unregister this handler
				}
			});
		}
	}

	@Override
	public void run(String[] args) {
		matsimModel.run(args);
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
	private void assignDependentPersons(EvacResident bdiAgent) {
		if( ScenarioTwoData.totPickups <= Config.getMaxPickUps() ) {
			double[] pDependents = {Config.getProportionWithKids(), Config.getProportionWithRelatives()};
			pDependents = Util.normalise(pDependents);
			Random random = BushfireMain.getRandom();

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