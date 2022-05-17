package io.github.agentsoz.nonmatsim;

import java.util.*;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2022 by its authors. See AUTHORS file.
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

import io.github.agentsoz.bdiabm.data.*;
import io.github.agentsoz.bdiabm.v2.AgentDataContainer;
import io.github.agentsoz.bdimatsim.EventsMonitorRegistry;
import io.github.agentsoz.util.PerceptList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class holds MatsimAgent objects and information
 *         related to the agents in the Matsim system such the replanner
 *         MatsimAgents and extension of Matsim Agents are stored in
 *         matSimAgents LinkedHashMap
 *         
 * @author Edmund Kemsley 
 */
public final class PAAgentManager {

	private static final Logger log = LoggerFactory.getLogger(PAAgentManager.class);

	/**
	 * full agent information, that is, actions, percepts, actionHandlers, perceptHandlers, states.  Since actions&percepts
	 * on the one hand, and state on the other hand are already in flat, indexed containers, the agent model ends up a bit 
	 * inconsistent (for matsim taste). 
	 */
	private final LinkedHashMap<String, PAAgent> agentsWithPerceptsAndActions;

	private final Map<String, String> agentsPerformingBdiDriveTo;
	private final Map<String, Double> agentsWaitingForTimeEvent;


	private final EventsMonitorRegistry eventsMonitors;

	private final io.github.agentsoz.bdiabm.v2.AgentDataContainer adc = new io.github.agentsoz.bdiabm.v2.AgentDataContainer();

	public final io.github.agentsoz.bdiabm.v2.AgentDataContainer getAgentDataContainerV2() {
		return adc;
	}

	public PAAgentManager(EventsMonitorRegistry eventsMonitors) {
		this.eventsMonitors = eventsMonitors;
		agentsWithPerceptsAndActions = new LinkedHashMap<>();
		agentsPerformingBdiDriveTo = new HashMap<>();
		agentsWaitingForTimeEvent = new HashMap<>();
	}

	public final PAAgent getAgent(String agentID) {
		return agentsWithPerceptsAndActions.get(agentID);
	}

	public Map<String, String> getAgentsPerformingBdiDriveTo() { return agentsPerformingBdiDriveTo; }

	public Map<String, Double> getAgentsWaitingForTimeEvent() { return agentsWaitingForTimeEvent; }


	/*
	 * Creates a new matsimAgent and it to the list Passes the ActionHandler,
	 * PerceptHandler, ActionContainer and PerceptContainer to matSimAgent
	 * Override this method and change the four parameters above to change
	 * functionality
	 */
	public final boolean createAndAddBDIAgent(String agentID) {
		PAAgent agent = new PAAgent( eventsMonitors, agentID,
				new ActionPerceptContainer() // own copy, *NOT* connected to agentDataContainer, ds 28/nov/17
		);
		agentsWithPerceptsAndActions.put(agentID, agent);
		return true;
	}

	public final Collection<String> getBdiAgentIds() {
		return agentsWithPerceptsAndActions.keySet() ;
	}

	final boolean removeAgent(String agentID) {
		agentsWithPerceptsAndActions.remove(agentID);
		// don't you need to also remove this from agentDataContainer??
		return true;
	}

	/*
	 * BDI side passed an action with state INITIATED Pass action parameters to
	 * ActionHandler then update action to RUNNING
	 */
	private final ActionContent.State initiateNewAction(String agentID, String actionID, ActionContent action) {
		if (agentsWithPerceptsAndActions.containsKey(agentID) ) {
			PAAgent agent = getAgent(agentID);
			Object[] parameters = action.getParameters();
			boolean didRegister = agent.getActionContainer().register(actionID,parameters);
			if (!didRegister) {
				log.warn("failed to register new action {}:{}" + actionID, action);
			}
			ActionContent.State res = agent.getActionHandler().processAction(agentID, actionID, parameters);
			agent.getActionContainer().get(actionID).setState(res);
			return res;
		}
		return ActionContent.State.FAILED;
	}

	/*
	 * BDI side wants to drop an action
	 */
	private final void dropAction(String agentID, String actionID, ActionContent action) {
		if (agentsWithPerceptsAndActions.containsKey(agentID)) {
			PAAgent agent = getAgent(agentID);
			agent.getActionHandler().deregisterBDIAction(actionID);
			agent.getActionContainer().register(actionID, action.getParameters());
			agent.getActionContainer().get(actionID).setState(ActionContent.State.DROPPED);
			log.info("agent {} dropped BDI action {}; MATSim leg is running detached now", agentID, actionID);
		}
	}

	public void updateActions(io.github.agentsoz.bdiabm.v2.AgentDataContainer inAdc, io.github.agentsoz.bdiabm.v2.AgentDataContainer outAdc) {
		if (inAdc != null) {
			Iterator<String> it = inAdc.getAgentIdIterator();
			while (it.hasNext()) {
				String agentId = it.next();
				// Process the incoming action updates
				Map<String, ActionContent> actions = inAdc.getAllActionsCopy(agentId);
				for (String actionId : actions.keySet()) {
					ActionContent content = actions.get(actionId);
					if (content.getState()== ActionContent.State.INITIATED) {
						if (agentsWithPerceptsAndActions.containsKey(agentId)) {
							PAAgent agent = getAgent(agentId);
							Object[] parameters = content.getParameters();
							ActionContent.State res = agent.getActionHandler().processAction(agentId, actionId, parameters);
							content.setState(res);
							outAdc.putAction(agentId, actionId, content);
						}
					} else if (content.getState()== ActionContent.State.DROPPED) {
						if (agentsWithPerceptsAndActions.containsKey(agentId)) {
							PAAgent agent = getAgent(agentId);
							agent.getActionHandler().deregisterBDIAction(actionId);
							content.setState(ActionContent.State.DROPPED);
							outAdc.putAction(agentId, actionId, content);
							log.info("agent {} dropped BDI action {}; MATSim leg is running detached now", agentId, actionId);
						}
					}
				}
			}
		}
	}

	/**
	 * Adds TIME percept for agents waiting for given time to lapse
	 * @param adc the data container to add the TIME percepts to
	 * @param now the current time, to calculate lapsed timers against
	 */
	public void addTimePerceptForLapsedTimers(AgentDataContainer adc, double now) {
		for (Iterator<String> i = agentsWaitingForTimeEvent.keySet().iterator(); i.hasNext();) {
			String agentId = i.next();
			Double timer = agentsWaitingForTimeEvent.get(agentId);
			if (timer <= now ) {
				PerceptContent pc = new PerceptContent(PerceptList.TIME, now);
				adc.putPercept(agentId, PerceptList.TIME, pc);
				i.remove(); // remove this timer since the time has passed
			}
		}
	}
}
