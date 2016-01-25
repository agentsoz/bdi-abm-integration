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
import io.github.agentsoz.abmjadex.data_structure.AddressAgentListTuple;
import io.github.agentsoz.abmjadex.data_structure.AddressTable;
import io.github.agentsoz.abmjadex.super_central.SuperExternalCommunicator.Methods;
import io.github.agentsoz.abmjadex.super_central.SuperExternalCommunicator.TargetType;
import jadex.commons.SimplePropertyObject;
import jadex.extension.envsupport.environment.IEnvironmentSpace;
import jadex.extension.envsupport.environment.ISpaceAction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.github.agentsoz.bdiabm.data.ActionPerceptContainer;
import io.github.agentsoz.bdiabm.data.AgentState;

public class SuperCreateAgentAction extends SimplePropertyObject implements ISpaceAction 
{
	/**
	 * Performs the action.
	 * @param parameters parameters for the action
	 * @param space the environment space
	 * @return action return value
	 */
	@SuppressWarnings("rawtypes")
	public Object perform(Map parameters, IEnvironmentSpace space)
	{	
		HashMap<Integer, ArrayList<String>> addressAgentMap = divideAgentTobeBorn (parameters, space);
		
		Set<Integer> addressIndexes = addressAgentMap.keySet();
		
		AddressTable addressTable = (AddressTable)space.getProperty("addressTable");
		for (Integer addressIndex : addressIndexes)
		{
			AddressAgentListTuple targetCentralOrganizer = addressTable.get(addressIndex);
			Object[] methodParameters = addressAgentMap.get(addressIndex).toArray();
			String[] methodParametersInString = objectToStringConverter(methodParameters);
			CentralServerInterface targetInstance = targetCentralOrganizer.getRemoteCO();
			
			// Sent Command to CO
			SuperExternalCommunicator extComm = new SuperExternalCommunicator (TargetType.JADEX_CENTRAL
					,targetInstance,Methods.CREATE_AGENT,methodParametersInString);
			extComm.start();
		}
		
		return null;
	}
	
	private String[] objectToStringConverter (Object[] methodParameters)
	{
		String[] stringArray = new String[methodParameters.length];
		for (int i = 0; i < methodParameters.length; i++)
		{
			stringArray[i] = (String)methodParameters[i];
		}
		return stringArray;
	}
	@SuppressWarnings("rawtypes")
	public HashMap<Integer, ArrayList<String>> divideAgentTobeBorn (Map parameters, IEnvironmentSpace space)
	{
		HashMap<Integer, ArrayList<String>> addressAgentMap = new HashMap<Integer, ArrayList<String>>();
		String[] agentIDs = (String[])parameters.get("agentIDs");
		AddressTable addressTable = (AddressTable)space.getProperty("addressTable");

		for (int k = 0; k < agentIDs.length; k++)
		{
			int minSize = 1000;
			int maxSize = 0;
			int indexMin = -1;
			int indexMax = -1;
			int indexUsed = -1;
			//Searching for the CentralOrganizer which have the highest number of agent
			//and the lowest number of agent
			for (int i = 0; i < addressTable.size(); i++)
			{
				int size = addressTable.get(i).getAgentList().size();
				if (size < minSize)
				{
					minSize = size;
					indexMin = i;
				}
				if (size > maxSize)
				{
					maxSize = size;
					indexMax = i;
				}
				if(size < AgentGroupper.getMaxCapacity()/2 && size > 0)
				{
					indexUsed = i;
				}
			}
			
			//If there are no CentralOrganizer which holds agents less then its 
			//half of maximum capacity
			if (indexUsed == -1)
			{
				if (indexMin != -1 && minSize < AgentGroupper.getMaxCapacity())
				{
					indexUsed = indexMin;
				}
				else if (maxSize < AgentGroupper.getMaxCapacity()) 
				{
					indexUsed =indexMax;
				}
				else
				{
					throw new RuntimeException("Capacity Exceeded, Agent Could not be created.");
				}
			}
			
			// Archived the agentID groupping into addressAgentMap
			if (addressAgentMap.containsKey(indexUsed) == false)
			{
				addressAgentMap.put(indexUsed, new ArrayList<String>());
			}
			ArrayList<String> agentIDsPerApp = addressAgentMap.get(indexUsed);
			agentIDsPerApp.add(agentIDs[k]);
			
			// Insert the agents info into addressTable
			AddressAgentListTuple targetCentralOrganizer = addressTable.get(indexUsed);
			targetCentralOrganizer.getAgentList().add(new AgentState(agentIDs[k]));
			targetCentralOrganizer.getAgentDataContainer().put(agentIDs[k], new ActionPerceptContainer());
		}

		return addressAgentMap;
	}
}
