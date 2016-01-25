package io.github.agentsoz.vaccination.controller;

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


import io.github.agentsoz.abmjack.shared.ActionManager;
import io.github.agentsoz.abmjack.shared.GlobalTime;
import io.github.agentsoz.vaccination.Global;
import io.github.agentsoz.vaccination.jack.Householder;

import java.util.*;

import io.github.agentsoz.bdiabm.ABMServerInterface;
import io.github.agentsoz.bdiabm.data.ActionContainer;
import io.github.agentsoz.bdiabm.data.ActionContent.State;
import io.github.agentsoz.bdiabm.data.ActionPerceptContainer;
import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.bdiabm.data.AgentState;
import io.github.agentsoz.bdiabm.data.AgentStateList;
import io.github.agentsoz.bdiabm.data.PerceptContainer;

/**
 * 
 * @author Alex Lutman
 *
 */
public class BDIConnector implements VaccinationBDIServerInterface, ActionManager{

	private HashMap<String, Householder> agents = new HashMap<String, Householder>();
	private ABMServerInterface abmServer;
	private AgentDataContainer nextContainer;
	private final String GLOBAL_AGENT = "global";
	
	public Householder[] getAgents() {
		Iterator<Map.Entry<String, Householder>> it = agents.entrySet().iterator();
		Householder[] ids = new Householder[agents.size()];
		int i = 0;
		while(it.hasNext()) {
			ids[i++] = it.next().getValue();
		}
		return ids;
	
	}
	
	public String[] getAgentIDs(){
		String[] agentIDs = new String[agents.size()];
		
		int counter = 0;
		for (Object agentID : agents.keySet()) {
			agentIDs[counter++] = (String)agentID;
		}
		
		return agentIDs;
	}
	
	public AgentDataContainer getNextContainer() {
		return nextContainer;
	}

	
	/* ----------------------------------------------------------------------
	 *  VaccinationBDIServerInterface Implementation
	 * ----------------------------------------------------------------------
	 */
	
	@Override
	public boolean init(AgentDataContainer agentDataContainer,
			AgentStateList agentList, ABMServerInterface abmServer,
			Object[] params) {
		this.abmServer = abmServer;
		return true;
	}
	
	@Override
	public void start() {
	}

	@Override
	public void takeControl (AgentDataContainer agentDataContainer) {
		System.out.println("---BDI taking control---");
		nextContainer = agentDataContainer;
		GlobalTime.updateTime();

		//Pull apart data container
		//Perform actions
		if(nextContainer == null) {
			waitUntilIdle();
			return;
		}
		//System.out.println(nextContainer.toString());
		boolean global = false;
		HashMap<String, Object> globalPercepts = new HashMap<String, Object>();
		try {
			PerceptContainer gPC = agentDataContainer.get(GLOBAL_AGENT).getPerceptContainer();
			String[] globalPerceptsArray = gPC.perceptIDSet().toArray(new String[0]);
			for(int g = 0; g < globalPerceptsArray.length; g++) {
				String globalPID = globalPerceptsArray[g];
				Object gaParameters = gPC.read(globalPID);
				globalPercepts.put(globalPID, gaParameters);
				global = true;
			}
		}
		catch (NullPointerException npe) {
			//no global agent
			global = false;
		}
		Iterator i = agentDataContainer.entrySet().iterator();
		//For each ActionPercept
		while(i.hasNext()) {
			Map.Entry<String, ActionPerceptContainer> entry = (Map.Entry<String, ActionPerceptContainer>)i.next();
			if(entry.getKey().equals(GLOBAL_AGENT)) {
				continue;
			}
			//post global percepts
			if(global) {
				Iterator<Map.Entry<String, Object>> globalEntries = globalPercepts.entrySet().iterator();
				while(globalEntries.hasNext()) {
					Map.Entry<String, Object> gme = globalEntries.next();
					String gPerceptID = gme.getKey();
					Object gParameters = gme.getValue();
					agents.get(entry.getKey()).doHandlePercept(gPerceptID, gParameters);
				}
			}
			//individual percepts
			ActionPerceptContainer apc = entry.getValue();
			PerceptContainer pc = apc.getPerceptContainer();
			Set<String> pcSet = pc.perceptIDSet();
			String[] pcArray = pcSet.toArray(new String[0]);
			for(int pcI = 0; pcI < pcArray.length; pcI++) {
				String perceptID = pcArray[pcI];
				Object parameters = pc.read(perceptID);
				agents.get(entry.getKey()).doHandlePercept(perceptID, parameters);
			}
			ActionContainer ac = apc.getActionContainer();
			Iterator<String> k = ac.actionIDSet().iterator();
			//for each action, update the agent action state
			while(k.hasNext()) {
				String actionID = k.next();
				State state = ac.get(actionID).getState();
				//remove completed states
				Object[] params = ac.get(actionID).getParameters();
				agents.get(entry.getKey()).updateActionState(actionID, state, params);
				if(!(state.equals(State.INITIATED) || state.equals(State.RUNNING))) {
					ac.remove(actionID);
				}
				
			}
		}
		waitUntilIdle();
	}

	@Override
	public void createAgents (String[] agentIDs) {
		for(int i = 0; i < agentIDs.length; i++) {
			System.out.println("Agent created: "+agentIDs[i]);
			Householder h = new Householder(agentIDs[i], this);
			agents.put(agentIDs[i], h);
		}
	}

	@Override
	public void killAgents (String[] agentIDs) {
		if(agentIDs == null) {
			return;
		}
		for(int i = 0; i < agentIDs.length; i++) {
			System.out.println("Agent killed: "+agentIDs[i]);
			agents.remove(agentIDs[i]);
			nextContainer.remove(agentIDs[i]);
		}
	}
	
	@Override
	public void finish() {
		System.out.println("BDI Shutting down");
	}

	
	/* ----------------------------------------------------------------------
	 *  ActionManager Implementation
	 * ----------------------------------------------------------------------
	 */

	
	public void packageAction(String agentID, String actionID, Object[] parameters) {
		boolean b = nextContainer.getOrCreate(agentID).getActionContainer().register(actionID, parameters);
		if(!b) {
			nextContainer.getOrCreate(agentID).getActionContainer().get(actionID).setParameters(parameters);
			nextContainer.getOrCreate(agentID).getActionContainer().get(actionID).setState(State.INITIATED);
		};
		System.out.println("Action agID: "+agentID+", acID: "+actionID+", new action?: "+b);

	}

	@Override
	public double getSimTime() {
		try {
			throw new Exception("This function is actually used!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	
	/* ----------------------------------------------------------------------
	 *  Utility functions
	 * ----------------------------------------------------------------------
	 */
	
	
	private void waitUntilIdle() {

		boolean allIdle = true;
		do {
			allIdle = true;
			Iterator<Map.Entry<String, Householder>> i = agents.entrySet().iterator();
			while(i.hasNext()) {
				Householder next = i.next().getValue();
				if(!next.isIdle()) {
					allIdle = false;
				}
			}
		}
		while(allIdle == false);
		abmServer.takeControl(nextContainer);
	}
}
