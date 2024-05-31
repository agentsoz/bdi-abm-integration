package io.github.agentsoz.bdimatsim;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2023 by its authors. See AUTHORS file.
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

import io.github.agentsoz.bdimatsim.TotalLinkLengthSingleton;
import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.PerceptContent;
import io.github.agentsoz.bdimatsim.EventsMonitorRegistry.MonitoredEventType;
import io.github.agentsoz.nonmatsim.BDIActionHandler;
import io.github.agentsoz.nonmatsim.BDIPerceptHandler;
import io.github.agentsoz.nonmatsim.EventData;
import io.github.agentsoz.nonmatsim.PAAgent;
import io.github.agentsoz.util.ActionList;
import io.github.agentsoz.util.PerceptList;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;

import java.util.Map;

public final class ActionHandlerForPerceive implements BDIActionHandler {
	private static final Logger log = Logger.getLogger( ActionHandlerForPerceive.class ) ;
	private final MATSimModel model;

	public ActionHandlerForPerceive(MATSimModel model ) {
		this.model = model;
	}
	@Override
	public ActionContent.State handle(String agentID, String actionID, Object[] args) {
		// assertions:
		Gbl.assertIf(args.length >= 1);

		for(int i=0; i<args.length; i++) {
			Gbl.assertIf(args[i] instanceof String);

			String eventToPerceive = (String) args[i];
			PAAgent paAgent = model.getAgentManager().getAgent(agentID);

			switch (eventToPerceive) {
				case PerceptList.TIME:
					// Time percept is managed by the PAAgentManager; Dhi 11/Jul/19
					if (i+1>=args.length || !(args[i+1] instanceof Double)) {
						throw new RuntimeException("Percept '" + eventToPerceive + "' must be followed by a time (double) value");
					} else {
						i++;
						Double timeToMonitor = model.getTime() + (Double)args[i];
						model.getAgentManager().getAgentsWaitingForTimeEvent().put(agentID, timeToMonitor);
					}
					break;
				case PerceptList.BLOCKED:
					paAgent.getPerceptHandler().registerBDIPerceptHandler(paAgent.getAgentID(),
							MonitoredEventType.NextLinkBlockedEvent, null, new BDIPerceptHandler() {
								@Override
								public boolean handle(String agentId, String currentLinkId, MonitoredEventType monitoredEvent, EventData event) {
									log.debug("agent with id=" + agentId + " perceiving a " + monitoredEvent + " event on link with id=" +
											currentLinkId);
									PAAgent agent = model.getAgentManager().getAgent(agentId);
									PerceptContent pc = new PerceptContent(PerceptList.BLOCKED, event.getAttributes());
									model.getAgentManager().getAgentDataContainerV2().putPercept(agent.getAgentID(), PerceptList.BLOCKED, pc);

									// If agent was driving then also send back status for the driveTo action
									if (model.getAgentManager().getAgentsPerformingBdiDriveTo().containsKey(agentID)) {
										model.getAgentManager().getAgentsPerformingBdiDriveTo().remove(agentID);
										Object[] params = {currentLinkId, event.getAttributes().get("nextLink")};
										ActionContent ac = new ActionContent(params, ActionContent.State.FAILED, ActionList.DRIVETO);
										model.getAgentManager().getAgentDataContainerV2().putAction(agent.getAgentID(), ActionList.DRIVETO, ac);
									}
									return false; // do not unregister
								}
							}
					);
					break;
				case PerceptList.CONGESTION:
					// And yet another in case the agent gets stuck in congestion on the way
					paAgent.getPerceptHandler().registerBDIPerceptHandler(paAgent.getAgentID(), MonitoredEventType.AgentInCongestionEvent,
							null, new BDIPerceptHandler() {
								@Override
								public boolean handle(String agentId, String currentLinkId, MonitoredEventType monitoredEvent, EventData event) {
									log.debug("agent with id=" + agentId + " perceiving a " + monitoredEvent + " event on link with id=" +
											currentLinkId);
									PAAgent agent = model.getAgentManager().getAgent(agentId);
									PerceptContent pc = new PerceptContent(PerceptList.CONGESTION, event.getAttributes());
									model.getAgentManager().getAgentDataContainerV2().putPercept(agent.getAgentID(), PerceptList.CONGESTION, pc);

									// If agent was driving to this link then also send back status for the driveTo action
									if (model.getAgentManager().getAgentsPerformingBdiDriveTo().containsKey(agentID)) {
										model.getAgentManager().getAgentsPerformingBdiDriveTo().remove(agentID);
										Object[] params = {currentLinkId};
										ActionContent ac = new ActionContent(params, ActionContent.State.FAILED, ActionList.DRIVETO);
										model.getAgentManager().getAgentDataContainerV2().putAction(agent.getAgentID(), ActionList.DRIVETO, ac);
									}

									return false; // do not unregister
								}
							}
					);
					break;
				case PerceptList.ACTIVITY_STARTED:
					paAgent.getPerceptHandler().registerBDIPerceptHandler(paAgent.getAgentID(), MonitoredEventType.ActivityStartEvent,
							null, new BDIPerceptHandler() {
								@Override
								public boolean handle(String agentId, String currentLinkId, MonitoredEventType monitoredEvent, EventData event) {
									log.debug("agent with id=" + agentId + " perceiving a " + monitoredEvent + " event on link with id=" +
											currentLinkId);
									PAAgent agent = model.getAgentManager().getAgent(agentId);
									PerceptContent pc = new PerceptContent(PerceptList.ACTIVITY_STARTED, event.getAttributes());
									model.getAgentManager().getAgentDataContainerV2().putPercept(agent.getAgentID(), PerceptList.ACTIVITY_STARTED, pc);
									return false; // do not unregister
								}
							}
					);
					break;
				case PerceptList.ACTIVITY_ENDED:
					paAgent.getPerceptHandler().registerBDIPerceptHandler(paAgent.getAgentID(), MonitoredEventType.ActivityEndEvent,
							null, new BDIPerceptHandler() {
								@Override
								public boolean handle(String agentId, String currentLinkId, MonitoredEventType monitoredEvent, EventData event) {
									log.debug("agent with id=" + agentId + " perceiving a " + monitoredEvent + " event on link with id=" +
											currentLinkId);
									PAAgent agent = model.getAgentManager().getAgent(agentId);
									PerceptContent pc = new PerceptContent(PerceptList.ACTIVITY_ENDED, event.getAttributes());
									model.getAgentManager().getAgentDataContainerV2().putPercept(agent.getAgentID(), PerceptList.ACTIVITY_ENDED, pc);
									return false; // do not unregister
								}
							}
					);
					break;
				case PerceptList.DEPARTED:
					paAgent.getPerceptHandler().registerBDIPerceptHandler(paAgent.getAgentID(), MonitoredEventType.PersonDepartureEvent,
							null, new BDIPerceptHandler() {
								@Override
								public boolean handle(String agentId, String currentLinkId, MonitoredEventType monitoredEvent, EventData event) {
									log.debug("agent with id=" + agentId + " perceiving a " + monitoredEvent + " event on link with id=" +
											currentLinkId);
									PAAgent agent = model.getAgentManager().getAgent(agentId);
									Map<String, String> attributes = event.getAttributes();

									// Find the destination activity for the trip just started
									MobsimAgent mobsimAgent = model.getMobsimAgentFromIdString(agentId);
									int index = WithinDayAgentUtils.getCurrentPlanElementIndex(mobsimAgent);
									// Extra check to ensure that we have moved out of the activity just finished,
									// otherwise we often get 'trip not found' exceptions for large scenarios.
									// Likely some thread synchornisation issue. dhi 28/jun/19
									if (model.getReplanner().editPlans().isAtRealActivity(mobsimAgent)) {
										if (index + 1 < WithinDayAgentUtils.getModifiablePlan(mobsimAgent).getPlanElements().size()) {
											index++;
										} else {
											index--;
										}
									}
									try {
										// Still getting trip not found sometimes in CastlemaineRegionArchetypesIT, dhi, 28/jun/19
										Activity destAct = model.getReplanner().editTrips().findTripAtPlanElementIndex(mobsimAgent,index).getDestinationActivity();
										attributes.put("actType", destAct.getType());
									} catch (Exception e) {
										log.error("Could not determine current destination for agent " + agentId + ": " + e.getMessage());
										attributes.put("actType", "unknown");
									}

									PerceptContent pc = new PerceptContent(PerceptList.DEPARTED, event.getAttributes());
									model.getAgentManager().getAgentDataContainerV2().putPercept(agent.getAgentID(), PerceptList.DEPARTED, pc);
									return false; // do not unregister
								}
							}
					);
					break;
				case PerceptList.ARRIVED:
					paAgent.getPerceptHandler().registerBDIPerceptHandler(paAgent.getAgentID(), MonitoredEventType.PersonArrivalEvent,
							null, new BDIPerceptHandler() {
								@Override
								public boolean handle(String agentId, String currentLinkId, MonitoredEventType monitoredEvent, EventData event) {
									log.debug("agent with id=" + agentId + " perceiving a " + monitoredEvent + " event on link with id=" +
											currentLinkId);
									PAAgent agent = model.getAgentManager().getAgent(agentId);
									//double totalLinkLengthTraveled = model.
									PerceptContent pc = new PerceptContent(PerceptList.ARRIVED, event.getAttributes());
									model.getAgentManager().getAgentDataContainerV2().putPercept(agent.getAgentID(), PerceptList.ARRIVED, pc);
									// If agent was driving to this link then also send back status for the driveTo action
									if (model.getAgentManager().getAgentsPerformingBdiDriveTo().containsKey(agentID)
											&& model.getAgentManager().getAgentsPerformingBdiDriveTo().get(agentID).equals(currentLinkId)) {
										model.getAgentManager().getAgentsPerformingBdiDriveTo().remove(agentID);

										//DistanceWorkaroundRead dw = new DistanceWorkaroundRead();
										//Object[] params = {currentLinkId,  dw.totalLinkLengthResult(agentId) };
										String total = Double.toString((TotalLinkLengthSingleton.INSTANCE.getValue(agentID)));

										Object[] params = {currentLinkId, total};
										ActionContent ac = new ActionContent(params, ActionContent.State.PASSED, ActionList.DRIVETO);
										model.getAgentManager().getAgentDataContainerV2().putAction(agent.getAgentID(), ActionList.DRIVETO, ac);
									}
									return false; // do not unregister
								}
							}
					);
					break;
				//added oemer
				case PerceptList.SUM_LINK_LENGTH:
					paAgent.getPerceptHandler().registerBDIPerceptHandler(paAgent.getAgentID(), MonitoredEventType.TotalLinkLengthTraveledEvent,
							null, new BDIPerceptHandler() {
								@Override
								public boolean handle(String agentId, String currentLinkId, MonitoredEventType monitoredEvent, EventData event) {
									log.debug("agent with id=" + agentId + " perceiving a " + monitoredEvent + " event on link with id=" +
											currentLinkId);
									PAAgent agent = model.getAgentManager().getAgent(agentId);
									PerceptContent pc = new PerceptContent(PerceptList.SUM_LINK_LENGTH, event.getAttributes());
									model.getAgentManager().getAgentDataContainerV2().putPercept(agent.getAgentID(), PerceptList.SUM_LINK_LENGTH, pc);
									// If agent was driving to this link then also send back status for the driveTo action
									if (model.getAgentManager().getAgentsPerformingBdiDriveTo().containsKey(agentID)
											&& model.getAgentManager().getAgentsPerformingBdiDriveTo().get(agentID).equals(currentLinkId)){
										model.getAgentManager().getAgentsPerformingBdiDriveTo().remove(agentID);
										Object[] params = {currentLinkId, event.getAttributes().get("totalLinkLengthTraveled")};
										ActionContent ac = new ActionContent(params, ActionContent.State.PASSED, ActionList.DRIVETO); //ActionContent.State.PASSED
										model.getAgentManager().getAgentDataContainerV2().putAction(agent.getAgentID(), ActionList.DRIVETO, ac);
									}
									return false; // do not unregister
								}
							}
					);
					break;

				case PerceptList.STUCK:
					paAgent.getPerceptHandler().registerBDIPerceptHandler(paAgent.getAgentID(),
							MonitoredEventType.PersonStuckEvent, null, new BDIPerceptHandler() {
								@Override
								public boolean handle(String agentId, String currentLinkId, MonitoredEventType monitoredEvent, EventData event) {
									log.debug("agent with id=" + agentId + " perceiving a " + monitoredEvent + " event on link with id=" +
											currentLinkId);
									PAAgent agent = model.getAgentManager().getAgent(agentId);
									PerceptContent pc = new PerceptContent(PerceptList.STUCK, null);
									if(currentLinkId != null) {
										Link link = model.getScenario().getNetwork().getLinks().get(Id.createLinkId(currentLinkId));
										Object[] obj = {
												link.getId().toString() + ":" + link.getFromNode().getId().toString(),
												link.getFromNode().getCoord().getX(),
												link.getFromNode().getCoord().getY(),
												link.getId().toString() + ":" + link.getToNode().getId().toString(),
												link.getToNode().getCoord().getX(),
												link.getToNode().getCoord().getY()
										};
										pc.setParameters(obj);
									}
									model.getAgentManager().getAgentDataContainerV2().putPercept(agent.getAgentID(), PerceptList.STUCK, pc);

									// If agent was driving then also send back status for the driveTo action
									if (model.getAgentManager().getAgentsPerformingBdiDriveTo().containsKey(agentID)) {
										model.getAgentManager().getAgentsPerformingBdiDriveTo().remove(agentID);
										Object[] params = {currentLinkId, event.getAttributes().get("nextLink")};
										ActionContent ac = new ActionContent(params, ActionContent.State.FAILED, ActionList.DRIVETO);
										model.getAgentManager().getAgentDataContainerV2().putAction(agent.getAgentID(), ActionList.DRIVETO, ac);
									}
									return false; // do not unregister
								}
							}
					);
					break;
				default:
					throw new RuntimeException("Cannot register for unknown percept type '" + eventToPerceive + "'");
			}
		}
		ActionContent ac = new ActionContent(null, ActionContent.State.PASSED, ActionList.PERCEIVE);
		model.getAgentManager().getAgentDataContainerV2().putAction(agentID, ActionList.PERCEIVE, ac);

		return ActionContent.State.PASSED;
	}
}