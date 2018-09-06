package io.github.agentsoz.abmjill;

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

import io.github.agentsoz.bdiabm.ABMServerInterface;
import io.github.agentsoz.bdiabm.Agent;
import io.github.agentsoz.bdiabm.BDIServerInterface;
import io.github.agentsoz.bdiabm.QueryPerceptInterface;
import io.github.agentsoz.bdiabm.data.*;
import io.github.agentsoz.bdiabm.data.ActionContent.State;
import io.github.agentsoz.jill.Main;
import io.github.agentsoz.jill.config.Config;
import io.github.agentsoz.jill.core.GlobalState;
import io.github.agentsoz.jill.struct.AObject;
import io.github.agentsoz.jill.util.ArgumentsLoader;
import io.github.agentsoz.util.evac.PerceptList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.*;

public abstract class JillModel implements BDIServerInterface {

	private static final Logger logger = LoggerFactory.getLogger(JillModel.class);

	PrintStream writer = null;
	private static AgentDataContainer nextContainer;
	private static AgentDataContainer lastContainer;

	private Config config;
	private QueryPerceptInterface queryInterface;

	private static io.github.agentsoz.bdiabm.v2.AgentDataContainer adc; // !! Be warned, this is static

	public JillModel() {
	}

	protected static Agent getAgent(int id) {
		AObject agent = GlobalState.agents.get(id);
		if (agent != null && agent instanceof Agent) {
			return (Agent)agent;
		}
		throw new RuntimeException("Agent " + id + " is not of type io.github.agentsoz.bdiabm.Agent; found " + agent);
	}

	@Override
	public boolean init(AgentDataContainer agentDataContainer,
			AgentStateList agentList, // not used
			ABMServerInterface abmServer,
			Object[] params) {
		nextContainer = new AgentDataContainer();
		lastContainer = new AgentDataContainer();
		// Parse the command line options
		ArgumentsLoader.parse((String[])params);
		// Load the configuration 
		config = ArgumentsLoader.getConfig();
		// Now initialise Jill with the loaded configuration
		try {
			Main.init(config);
		} catch(Exception e) {
			logger.error("While initialising JillModel: {}", e.getMessage());
			return false;
		}
		return true;
	}

	@Override
	public void setQueryPerceptInterface(QueryPerceptInterface queryInterface) {
		this.queryInterface = queryInterface;
	}

	@Override
	public QueryPerceptInterface getQueryPerceptInterface() {
		return queryInterface;
	}

	@Override
	public void start() {
		Main.start(config);
	}

	@Override
	public void finish() {
		Main.finish();
	}

	// package new agent action into the agent data container
	public static void packageAgentAction(String agentID, String actionID,
										  Object[] parameters, String actionState) {
		
		getAgent(Integer.valueOf(agentID)).packageAction(actionID, parameters);

		ActionContainer ac = nextContainer.getOrCreate(agentID)
				.getActionContainer();
		boolean isNewAction = ac.register(actionID, parameters);
		if (!isNewAction) {
			ac.get(actionID).setParameters(parameters);
			ActionContent.State state = ActionContent.State.INITIATED;
			if (actionState != null) {
				try {
					state = ActionContent.State.valueOf(actionState);
				} catch (Exception e) {
					logger.warn("ignoring unknown action state: "
							+ " agent:" + agentID
							+ ", action id:" + actionID
							+ ", content:" + ac.get(actionID));
				}
			}
			ac.get(actionID).setState(state);
		}
		logger.debug("added " + ((isNewAction) ? "new action" : "")
				+ " into ActionContainer: agent:" + agentID + ", action id:"
				+ actionID + ", content:" + ac.get(actionID));
	}

	public static io.github.agentsoz.bdiabm.v2.AgentDataContainer getAgentDataContainer() {
		return adc;
	}

    public void setAgentDataContainer(io.github.agentsoz.bdiabm.v2.AgentDataContainer adc) {
        JillModel.adc = adc;
    }


    public static void packageAgentActionV2(String agentId,
											String actionId,
											Object[] parameters,
											String actionState) {
		ActionContent.State state = ActionContent.State.INITIATED;
		if (actionState != null) {
			try {
				state = ActionContent.State.valueOf(actionState);
			} catch (Exception e) {
				logger.warn("agent {} ignoring unknown action state {}", agentId, actionState);
			}
		}
		ActionContent ac = new ActionContent(parameters, state, actionId );
		adc.putAction(agentId, actionId, ac);
	}

	public void takeControlV2(io.github.agentsoz.bdiabm.v2.AgentDataContainer adc) {
		if (adc != null) {
			Iterator<String> it = adc.getAgentIdIterator();
			while (it.hasNext()) {
				String agentId = it.next();
				// Process the incoming percepts
				Map<String, PerceptContent> percepts = adc.getAllPerceptsCopy(agentId);
				for (String perceptId : percepts.keySet()) {
					PerceptContent content = percepts.get(perceptId);
					if (content != null) {
						try {
							int id = Integer.parseInt(agentId);
							getAgent(id).handlePercept(content.getPercept_type(), content.getValue());
						} catch (Exception e) {
							logger.error("While handling percept for Agent {}: {}", agentId, e.getMessage());
						}
					}
				}

				// Process the incoming action updates
				Map<String, ActionContent> actions = adc.getAllActionsCopy(agentId);
				for (String actionId : actions.keySet()) {
					ActionContent content = actions.get(actionId);
					try {
						int id = Integer.parseInt(agentId);
						getAgent(id).updateAction(content.getInstance_id(), content);
					} catch (Exception e) {
						logger.error("While updating action status for Agent {}: {}", agentId, e.getMessage());
					}
				}
			}
		}
	}

	@Override
	// send percepts to individual agents
	public void takeControl(AgentDataContainer agentDataContainer) {

		// save the container
		lastContainer.removeAll();
		lastContainer.copy(agentDataContainer);

		if (agentDataContainer == null || agentDataContainer.isEmpty()) {
			logger.debug("Received empty container, nothing to do.");
			return;
		}

		logger.trace("Received {}", agentDataContainer);

		HashMap<String, Object> globalPercepts = new LinkedHashMap<String, Object>();

        PerceptContainer gPC = lastContainer.getPerceptContainer(PerceptList.BROADCAST);
        if (gPC != null) {
          String[] globalPerceptsArray = gPC.perceptIDSet().toArray(new String[0]);
          for (int g = 0; g < globalPerceptsArray.length; g++) {
              String globalPID = globalPerceptsArray[g];
              Object gaParameters = gPC.read(globalPID);
              globalPercepts.put(globalPID, gaParameters);
		  }
          Iterator<Map.Entry<String, Object>> globalEntries = globalPercepts
              .entrySet().iterator();
          while (globalEntries.hasNext()) {
            Map.Entry<String, Object> gme = globalEntries.next();
            String gPerceptID = gme.getKey();
            Object gParameters = gme.getValue();
            for (int i = 0; i < GlobalState.agents.size(); i++) {
              getAgent(i).handlePercept(gPerceptID, gParameters);
            }
          }
        }

		Iterator<String> i = lastContainer.getAgentIDs();
		// For each ActionPercept (one for each agent)
		while (i.hasNext()) {
			String agentID = i.next();
			if (agentID.equals(PerceptList.BROADCAST)) {
				continue;
			}
			ActionPerceptContainer apc = lastContainer.getOrCreate(agentID);
			PerceptContainer pc = apc.getPerceptContainer();
			ActionContainer ac = apc.getActionContainer();
			if (!pc.isEmpty()) {
				Set<String> pcSet = pc.perceptIDSet();
				String[] pcArray = pcSet.toArray(new String[0]);

				for (int pcI = 0; pcI < pcArray.length; pcI++) {
					String perceptID = pcArray[pcI];
					Object parameters = pc.read(perceptID);
					try {
						int id = Integer.parseInt(agentID);
						getAgent(id).handlePercept(perceptID, parameters);
					} catch (Exception e) {
						logger.error("While sending percept to Agent {}: {}", agentID, e.getMessage());
					}
				}
				// now remove the percepts
				pc.clear();
			}
			if (!ac.isEmpty()) {
				Iterator<String> k = ac.actionIDSet().iterator();
				// for each action, update the agent action state
				while (k.hasNext()) {

					String actionID = k.next();
					// convert from state definition in bdimatsim to definition
					// in
					// jack part
					State state = State.valueOf(ac.get(actionID).getState()
							.toString());
					Object[] params = ac.get(actionID).getParameters();
					ActionContent content = new ActionContent(params, state, actionID);
					try {
						int id = Integer.parseInt(agentID);
						getAgent(id).updateAction(actionID, content);
					} catch (Exception e) {
						logger.error("While updating action status for Agent {}: {}", agentID, e.getMessage());
					}

					// remove completed states
					if (!(state.equals(State.INITIATED) || state
							.equals(State.RUNNING))) {
						ac.remove(actionID);
					}
				}
			}
		}
		// Wait until idle
		Main.waitUntilIdle();

		// now transfer all the new updates to the container
		agentDataContainer.copy(lastContainer); // copies status changes
		lastContainer.removeAll();
		agentDataContainer.copy(nextContainer); // copies new actions
		nextContainer.removeAll();
    }
}