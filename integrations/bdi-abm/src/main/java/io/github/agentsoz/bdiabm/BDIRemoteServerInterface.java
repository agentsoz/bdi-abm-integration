package io.github.agentsoz.bdiabm;

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


import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.bdiabm.data.AgentStateList;

import java.rmi.RemoteException;

public interface BDIRemoteServerInterface extends java.rmi.Remote
{
	public final static String ADDRESS = "rmi://localhost:1205/BDIServer";
	
	/**
	 * The method to start the Jadex system to function with Repast
	 * @param APContainer
	 * The list of initial Action Percept Container
	 * @param agentList
	 * The list of agent shall be created in Jadex
	 * @throws RemoteException*/
	 
	public boolean start (AgentDataContainer agentDataContainer, AgentStateList agentList, 
			ABMRemoteServerInterface abmServer) throws RemoteException;
	
	/**
	 * Method signaling the time for Jadex to 
	 * take over the control of the turns 
	 * @param actionPerceptContainer
	 * @throws RemoteException
	 */
	public void takeControl (AgentDataContainer agentDataContainer) throws RemoteException;
	
	/**
	 * Method (called by Repast) to queue new agent to be created in Jadex
	 * Agent will be created in the next time step of Jadex
	 * @param agentID
	 */
	public void createAgents (String[] agentIDs) throws RemoteException;
	
	/**
	 * To delete BDI agents in Jadex
	 * @param agentIDs
	 * @throws RemoteException
	 */
	public void killAgents (String[] agentIDs) throws RemoteException;
	
	/**
	 * To terminate the server
	 * @throws RemoteException
	 */
	public void terminateProgram () throws RemoteException;

}
