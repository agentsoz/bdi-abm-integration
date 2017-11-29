package io.github.agentsoz.abmjack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import aos.jack.jak.agent.Agent;
import io.github.agentsoz.abmjack.shared.ActionManager;

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
import io.github.agentsoz.bdiabm.BDIServerInterface;
import io.github.agentsoz.bdiabm.data.ActionContainer;
import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.ActionContent.State;
import io.github.agentsoz.bdiabm.data.ActionPerceptContainer;
import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.bdiabm.data.AgentState;
import io.github.agentsoz.bdiabm.data.AgentStateList;
import io.github.agentsoz.bdiabm.data.PerceptContainer;

import io.github.agentsoz.util.Global;

/**
 * 
 * @author Alex Lutman, Oscar Francis
 * 
 *         This class sits between the BDI agents and the ABM. When extended for
 *         a specific application, it handles creating agents and translating
 *         actions and percepts.
 */
public abstract class JACKModel implements BDIServerInterface, ActionManager {

	final Logger logger = LoggerFactory.getLogger("");

	protected Map<String, Agent> agents = new LinkedHashMap<>();
	// need deterministically sorted map for testing.  kai, oct'17

	// dsingh, 29/nov/17, all Jack side updates should be collected in this
	// local container, and will get copied over once when passing control
	// back to the abm side
	private AgentDataContainer lastContainer = new AgentDataContainer();
	private AgentDataContainer nextContainer = new AgentDataContainer();

	public final String GLOBAL_AGENT = "global";

	@Override
	// set up connections and get any needed parameters from the ABM server
	public boolean init(AgentDataContainer agentDataContainer,
			AgentStateList agentList, ABMServerInterface abmServer,
			Object[] params) {
		setup(abmServer);
		takeControl(agentDataContainer);
		if (params != null) {
			this.createAgents((String[]) params);
		}
		return true;
	}

	@Override
	public void finish() {
		this.killAllAgents();
	}

	@Override
	public void start() {
	};

	public Agent[] getAgents() {

		Iterator<Map.Entry<String, Agent>> it = agents.entrySet().iterator();
		Agent[] ids = new Agent[agents.size()];
		int i = 0;
		while (it.hasNext()) {
			ids[i++] = it.next().getValue();
		}

		return ids;
	}

	private AgentDataContainer getNextContainer() {

		return nextContainer;
	}

	// package new agent action into the agent data container
	@Override
	public void packageAction(String agentID, String actionID,
			Object[] parameters) {

		ActionContainer ac = nextContainer.getOrCreate(agentID)
				.getActionContainer();
		boolean isNewAction = ac.register(actionID, parameters);
		if (!isNewAction) {
			ac.get(actionID).setParameters(parameters);
			ac.get(actionID).setState(ActionContent.State.INITIATED);
		}
		logger.debug(
				"added {} into ActionContainer: agent:{}, action id:{}, content:{}",
				((isNewAction) ? "new action" : ""), agentID, actionID,
				ac.get(actionID));
	}

	// send percepts to individual agents
	@Override
	public void takeControl(AgentDataContainer agentDataContainer) {

		logger.trace("Received {}", agentDataContainer);
		Global.updateTime();

		// save the container
		lastContainer.removeAll();
		lastContainer.copy(agentDataContainer);

		if (agentDataContainer.isEmpty()) {
			return;
		}

		Map<String, Object> globalPercepts = new LinkedHashMap<>();
		// need deterministically sorted map for testing.  kai, oct'17

        PerceptContainer gPC = lastContainer.getPerceptContainer(GLOBAL_AGENT);
        if (gPC != null) {
			String[] globalPerceptsArray = gPC.perceptIDSet().toArray(new String[0]);
			for (int g = 0; g < globalPerceptsArray.length; g++) {
				String globalPID = globalPerceptsArray[g];
				Object gaParameters = gPC.read(globalPID);
				globalPercepts.put(globalPID, gaParameters);
			}
            Iterator<Map.Entry<String, Object>> globalEntries = globalPercepts .entrySet().iterator();
            while (globalEntries.hasNext()) {
                Map.Entry<String, Object> gme = globalEntries.next();
                String gPerceptID = gme.getKey();
                Object gParameters = gme.getValue();
                for( Agent agent : agents.values() ) {
                    handlePercept(agent, gPerceptID, gParameters);
                }
            }
            gPC.clear();
		}

        Iterator<String> i = lastContainer.getAgentIDs();
        // For each ActionPercept (one for each agent)
        while (i.hasNext()) {
            String agentID = i.next();
			if (agentID.equals(GLOBAL_AGENT)) {
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
					handlePercept(agents.get(agentID), perceptID, parameters);
				}
				// now remove the percepts
				pc.clear();
			}
			if (!ac.isEmpty()) {
				Iterator<String> k = ac.actionIDSet().iterator();
				// for each action, update the agent action state
				while (k.hasNext()) {

					String actionID = k.next();
					// convert from state definition in bdimatsim to definition in
					// jack part
					State state = State.valueOf(ac.get(actionID).getState()
							.toString());
					Object[] params = ac.get(actionID).getParameters();
					updateAction(agents.get(agentID), actionID, state,
							params);

					// remove completed states
					if (!(state.equals(State.INITIATED) || state
							.equals(State.RUNNING))) {
						ac.remove(actionID);
					}
				}
			}
		}
		waitUntilIdle();

        // now transfer all the new updates to the container
		agentDataContainer.copy(lastContainer); // copies status changes
		lastContainer.removeAll();
		agentDataContainer.copy(nextContainer); // copies new actions
		nextContainer.removeAll();
	}

	// wait until all JACK agents have finished processing before returning
	// control to ABM system
	/*private*/ protected void waitUntilIdle() {

		boolean allIdle = true;
		do {
			allIdle = true;
			Iterator<Map.Entry<String, Agent>> i = agents.entrySet().iterator();
			while (i.hasNext()) {
				Agent next = i.next().getValue();
				if (!next.isIdle()) {
					allIdle = false;
				}
			}
		} while (allIdle == false);
	}

	private void createAgents(String[] agentIDs) {
		createAgents(agentIDs, null);
	}

	public void createAgents(String[] agentIDs, Object initData) {
		for (int i = 0; i < agentIDs.length; i++) {
			Agent agent = createAgent(agentIDs[i], new Object[] { initData });
			agents.put(agentIDs[i], agent);
		}
		logger.debug("created agents: {}", new Gson().toJson(agentIDs));
	}

	public void createAgents(ArrayList<AgentState> alist) {
		String[] agentList = new String[alist.size()];
		for (int i = 0; i < alist.size(); i++) {
			agentList[i] = alist.get(i).getID();
		}
		createAgents(agentList);
	}

	public void killAgents(String[] agentIDs) {
		if (agentIDs == null) {
			return;
		}
		for (int i = 0; i < agentIDs.length; i++) {
			logger.debug("killed agent:{}", agentIDs[i]);
			agents.remove(agentIDs[i]);
			nextContainer.remove(agentIDs[i]);
		}
	}

	private void killAllAgents() {
		Agent[] ar1 = agents.values().toArray(new Agent[0]);
		String[] ar2 = new String[ar1.length];

		for (int i = 0; i < ar1.length; i++) {
			ar2[i] = ar1[i].getBasename();
		}
		killAgents(ar2);
	}
	
	// To be implemented by domain-specific application
	public abstract void setup(ABMServerInterface abmServer);

	public abstract Agent createAgent(String agentID, Object[] initData);

	public abstract void handlePercept(Agent agent, String perceptID,
			Object parameters);

	public abstract void updateAction(Agent agent, String actionID,
			State state, Object[] parameters);
}
