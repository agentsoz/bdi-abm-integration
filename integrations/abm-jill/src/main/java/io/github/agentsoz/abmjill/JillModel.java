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
import io.github.agentsoz.bdiabm.data.ActionContainer;
import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.ActionPerceptContainer;
import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.bdiabm.data.AgentStateList;
import io.github.agentsoz.bdiabm.data.PerceptContainer;
import io.github.agentsoz.bdiabm.data.ActionContent.State;
import io.github.agentsoz.jill.Main;
import io.github.agentsoz.jill.core.GlobalState;
import io.github.agentsoz.jill.util.Log;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.github.agentsoz.jill.config.Config;
import io.github.agentsoz.jill.util.ArgumentsLoader;

public abstract class JillModel implements BDIServerInterface {

	private static final String BROADCAST = "global";
	PrintStream writer = null;
	private static AgentDataContainer nextContainer;
	private Config config;
	
	public JillModel() {
	}

	public Agent getAgent(int id) {
		return (Agent) GlobalState.agents.get(id);
	}

	public Agent getAgentByName(String agentName) {
		return (Agent) GlobalState.agents.find(agentName);
	}

	public List<Agent> getAllAgents() {
		List<Agent> agents = new ArrayList<Agent>();

		for (int i = 0; i < GlobalState.agents.size(); i++) {
			agents.add((Agent) GlobalState.agents.get(i));
		}

		return agents;
	}

	@Override
	public boolean init(AgentDataContainer agentDataContainer,
			AgentStateList agentList, 
			ABMServerInterface abmServer,
			Object[] params) {
		nextContainer = agentDataContainer;
		// Parse the command line options
		ArgumentsLoader.parse((String[])params);
		// Load the configuration 
		config = ArgumentsLoader.getConfig();
		// Now initialise Jill with the loaded configuration
		try {
			Main.init(config);
		} catch(Exception e) {
			Log.error("While initialising JillModel: " + e.getMessage());
			return false;
		}
		return true;
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
			Object[] parameters) {
		
		((Agent) GlobalState.agents.get(Integer.valueOf(agentID))).packageAction(actionID, parameters);

		ActionContainer ac = nextContainer.getOrCreate(agentID)
				.getActionContainer();
		boolean isNewAction = ac.register(actionID, parameters);
		if (!isNewAction) {
			ac.get(actionID).setParameters(parameters);
			ac.get(actionID).setState(ActionContent.State.INITIATED);
		}
		Log.debug("added " + ((isNewAction) ? "new action" : "")
				+ " into ActionContainer: agent:" + agentID + ", action id:"
				+ actionID + ", content:" + ac.get(actionID));
	}

	@Override
	// send percepts to individual agents
	public void takeControl(AgentDataContainer agentDataContainer) {

		Log.trace("Received " + agentDataContainer);

		if (agentDataContainer == null || agentDataContainer.isEmpty()) {
			Log.debug("Received empty container, nothing to do.");
			return;
		}
		nextContainer = agentDataContainer;

		boolean global = false;
		HashMap<String, Object> globalPercepts = new HashMap<String, Object>();

		try {
			PerceptContainer gPC = agentDataContainer.get(BROADCAST)
					.getPerceptContainer();
			String[] globalPerceptsArray = gPC.perceptIDSet().toArray(
					new String[0]);
			for (int g = 0; g < globalPerceptsArray.length; g++) {
				String globalPID = globalPerceptsArray[g];
				Object gaParameters = gPC.read(globalPID);
				globalPercepts.put(globalPID, gaParameters);
				global = true;
			}
		}
		// no global agent
		catch (NullPointerException npe) {
			global = false;
		}
		// post global percepts to all agents - this was moved out of the below
		// while loop
		// since not all agents will have an ActionPerceptContainer when program
		// starts
		if (global) {
			Iterator<Map.Entry<String, Object>> globalEntries = globalPercepts
					.entrySet().iterator();
			while (globalEntries.hasNext()) {
				Map.Entry<String, Object> gme = globalEntries.next();
				String gPerceptID = gme.getKey();
				Object gParameters = gme.getValue();
				for (int i = 0; i < GlobalState.agents.size(); i++) {
					((Agent) GlobalState.agents.get(i)).handlePercept(
							gPerceptID, gParameters);
				}
			}
		}

		Iterator<Entry<String, ActionPerceptContainer>> i = agentDataContainer
				.entrySet().iterator();
		// For each ActionPercept (one for each agent)
		while (i.hasNext()) {
			Map.Entry<String, ActionPerceptContainer> entry = (Map.Entry<String, ActionPerceptContainer>) i
					.next();
			if (entry.getKey().equals(BROADCAST)) {
				continue;
			}
			ActionPerceptContainer apc = entry.getValue();
			PerceptContainer pc = apc.getPerceptContainer();
			ActionContainer ac = apc.getActionContainer();
			if (!pc.isEmpty()) {
				Set<String> pcSet = pc.perceptIDSet();
				String[] pcArray = pcSet.toArray(new String[0]);

				for (int pcI = 0; pcI < pcArray.length; pcI++) {
					String perceptID = pcArray[pcI];
					Object parameters = pc.read(perceptID);
					try {
						int id = Integer.parseInt(entry.getKey());
						((Agent) GlobalState.agents.get(id)).handlePercept(
								perceptID, parameters);
					} catch (Exception e) {
						Log.error("While sending percept to Agent "
								+ entry.getKey() + ": " + e.getMessage());
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
						int id = Integer.parseInt(entry.getKey());
						((Agent) GlobalState.agents.get(id)).updateAction(
								actionID, content);
					} catch (Exception e) {
						Log.error("While updating action status for Agent "
								+ entry.getKey() + ": " + e.getMessage());
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
	}

}