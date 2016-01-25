package io.github.agentsoz.abmjadex.super_central;

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


import io.github.agentsoz.abmjadex.central_organizer.CentralServerInterface;
import io.github.agentsoz.abmjadex.central_organizer.ConfirmatorInterface;
import io.github.agentsoz.abmjadex.data_structure.AddressAgentListTuple;
import io.github.agentsoz.abmjadex.miscellaneous.ABMBDILoggerSetter;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.util.logging.Logger;

import io.github.agentsoz.bdiabm.ABMRemoteServerInterface;
import io.github.agentsoz.bdiabm.BDIRemoteServerInterface;
import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.bdiabm.data.AgentStateList;


public class SuperExternalCommunicator extends Thread
{
	private final static Logger LOGGER = Logger.getLogger(SuperExternalCommunicator.class.getName());
	
	public enum Methods 
	{
		TAKE_CONTROL, QUERY_PERCEPT, START, RETRIEVE, CREATE_AGENT, 
		KILL_AGENT, TERMINATE
	}
	
	public enum TargetType
	{
		JADEX_CENTRAL, REPAST 
	}
	
	private Methods method;
	private TargetType targetType;
	private Object targetServer;
	private Object[] parameters;
	
	public SuperExternalCommunicator (TargetType targetType, Object targetServer
									, Methods methodID, Object[] parameters)
	{
		ABMBDILoggerSetter.setup(LOGGER);
		this.targetType = targetType;
		this.targetServer = targetServer;
		this.method = methodID;
		this.parameters = parameters;
	}
			
	/**
	 * The command to do remote method invocation
	 * @param targetAddress
	 * @param methodToCall
	 * @return
	 */
	public Object startAndWait ()
	{
		Object returnValue = null;
		System.setSecurityManager(new RMISecurityManager());
		try 
		{
			if (method.equals(Methods.START))
			{
				startCentralOrganizer((AddressAgentListTuple)parameters[0], (ABMRemoteServerInterface)parameters[1]);
			}
			else if (method.equals(Methods.RETRIEVE))
			{
				returnValue = retrieve();
			}
			else if (method.equals(Methods.TAKE_CONTROL))
			{
				takeControl();
			}
			else if (method.equals(Methods.CREATE_AGENT))
			{
				createAgent();
			}
			else if (method.equals(Methods.KILL_AGENT))
			{
				killAgent();
			}
			else if (method.equals(Methods.TERMINATE))
			{
				terminate();
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			LOGGER.severe(e.getMessage());
		} catch (RemoteException e) {
			e.printStackTrace();
			LOGGER.severe(e.getMessage());
		} catch (NotBoundException e) {
			e.printStackTrace();
			LOGGER.severe(e.getMessage());
		}
		return returnValue;
	}
	
	private void startCentralOrganizer (AddressAgentListTuple addressAgentList, ABMRemoteServerInterface abmServer) throws MalformedURLException, RemoteException, NotBoundException
	{
		BDIRemoteServerInterface targetServer = (BDIRemoteServerInterface)this.targetServer;
		AgentDataContainer agentDataContainer = addressAgentList.getAgentDataContainer();
		AgentStateList agentList = addressAgentList.getAgentList();
		
		targetServer.start(agentDataContainer, agentList, abmServer);
	}
	
	private Object[] retrieve () throws MalformedURLException, RemoteException, NotBoundException
	{
		return ((CentralServerInterface)targetServer).retrieveData((ConfirmatorInterface)parameters[0]);
	}
	
	private void takeControl () throws MalformedURLException, RemoteException, NotBoundException
	{
		Object target = targetServer;
		if (targetType.equals(TargetType.REPAST))
		{
			ABMRemoteServerInterface targetServer = (ABMRemoteServerInterface)target;
			AgentDataContainer aContainer = (AgentDataContainer)parameters[0];
			targetServer.takeControl(aContainer);
		}
		else if (targetType.equals(TargetType.JADEX_CENTRAL))
		{
			BDIRemoteServerInterface targetServer = (BDIRemoteServerInterface)target;
			AgentDataContainer aContainer = (AgentDataContainer)parameters[0];
			targetServer.takeControl(aContainer);
		}
	}
	
	private void createAgent() throws MalformedURLException, RemoteException, NotBoundException
	{
		BDIRemoteServerInterface targetServer = (BDIRemoteServerInterface)this.targetServer;
		targetServer.createAgents((String[])parameters);
	}
	
	private void killAgent() throws MalformedURLException, RemoteException, NotBoundException
	{
		BDIRemoteServerInterface targetServer = (BDIRemoteServerInterface)this.targetServer;
		targetServer.killAgents((String[])parameters);
	}
	
	private void terminate() throws MalformedURLException, RemoteException, NotBoundException
	{
		BDIRemoteServerInterface targetServer = (BDIRemoteServerInterface)this.targetServer;
		targetServer.terminateProgram();
	}
	
	@Override
	public void run ()
	{
		startAndWait();
	}
}
