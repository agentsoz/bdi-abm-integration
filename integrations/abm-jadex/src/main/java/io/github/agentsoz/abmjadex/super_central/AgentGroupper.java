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


import io.github.agentsoz.abmjadex.data_structure.AddressAgentListTuple;
import io.github.agentsoz.abmjadex.data_structure.AddressTable;
import io.github.agentsoz.bdiabm.data.ActionPerceptContainer;
import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.bdiabm.data.AgentState;
import io.github.agentsoz.bdiabm.data.AgentStateList;

/*
 * Term used :
 * apps : a Jadex .application which govern over a number of Jadex's Agents
 * MAX_CAPACITY : the maximum number of agent handled by an apps, as performance is a concern 
 * MIN_CAPACITY : the minimum number of agents in a Central, until it is decided 
 * 				  that another Central is needed to be the medium for the agent
 */
public class AgentGroupper
{
	/*
	 * This consists of the method to group agents as distributed as it could
	 */
	private static int MAX_CAPACITY = 100;
	private static int MIN_CAPACITY = 25;
	private AgentStateList agentStateList;
	private AgentDataContainer agentDataContainer;
	private AddressTable addressTable;
	private int totalNoOfAgents;
	
	public enum Mode {
		ADD, FILL
	}
		
	public AgentGroupper (AgentStateList agentList, AgentDataContainer agentDataContainer
									,AddressTable addressTable)
	{
		this.agentStateList = agentList;
		this.agentDataContainer = agentDataContainer;
		this.addressTable = addressTable;
		this.totalNoOfAgents = agentList.size();
	}
	
	//static method to set the MIN_CAPACITY value.
	//set this to 1 for testing the inter-centralOrganizer sending message
	//protocol with just 2 agents.
	public static void setMinCapacity (int value)
	{
		MIN_CAPACITY = value;
	}
	
	public static void setMaxCapacity (int value)	
	{
		MAX_CAPACITY = value;
	}
	
	public static int getMaxCapacity ()
	{
		return MAX_CAPACITY;
	}
	/**
	 * Default command to create groups with
	 * minimum capacity of 25 per Central Organizer
	 * @return
	 * modified AddressTable
	 */
	public AddressTable makeGroups ()
	{
		return makeGroups(MIN_CAPACITY);
	}
	
	public AddressTable makeGroups (int minCapacity) 
	{
		int noOfApps = addressTable.size();
		if (minCapacity == 0 || noOfApps == 0 || (MAX_CAPACITY*noOfApps < totalNoOfAgents)
				|| minCapacity > 100)
		{
			throw new RuntimeException("Grouping could not be made");
		}
		
		if (totalNoOfAgents <= (minCapacity * noOfApps))
		{
			balanceRoundRobin (minCapacity, minCapacity, noOfApps, Mode.FILL);
		}
		else
		{
			int x = totalNoOfAgents / noOfApps;
			balanceRoundRobin (x, minCapacity, noOfApps, Mode.FILL);
		}
		
		return addressTable;
	}
	
	private void balanceRoundRobin (int noOfAgentsPerApp, int minCapacity, int maxNoApp, Mode mode)
	{
		int[] agentDivision = new int[maxNoApp];
		int agentDivisionIndex = 0;
		
		int agentIndex = 0;
		int noRequiredApps = totalNoOfAgents / noOfAgentsPerApp;
		int remainderNoAgents = totalNoOfAgents % noOfAgentsPerApp;
		
		if (noRequiredApps == 0)
		{
			noRequiredApps = 1;
			noOfAgentsPerApp = 0;
		}
		
		boolean extendsNoOfApp = false;
		int additionalNoOfAgentsPerApp = remainderNoAgents / noRequiredApps;
		int noOfAppsFilled = 0;
		
		if ((remainderNoAgents < minCapacity && remainderNoAgents != 0 
				&& (additionalNoOfAgentsPerApp+noOfAgentsPerApp+1) <= MAX_CAPACITY) 
				|| noRequiredApps == maxNoApp)
		{
			int modulusOfRemainder = remainderNoAgents % noRequiredApps;
			int i = 0;
			
			for (i=0; i < modulusOfRemainder; i++)
			{
				int noToBeFilled = noOfAgentsPerApp + additionalNoOfAgentsPerApp + 1;			
				if (mode.equals(Mode.FILL) == true)
					agentIndex = fillApps(agentIndex, noToBeFilled, i);
				else
					agentIndex = addApps(agentIndex, noToBeFilled, i);
				{
					agentDivision[agentDivisionIndex] = noToBeFilled;
					agentDivisionIndex++;
				}
			}
			
			for (;i < noRequiredApps; i++)
			{
				int noToBeFilled = noOfAgentsPerApp + additionalNoOfAgentsPerApp;		
				if (mode.equals(Mode.FILL) == true)
					agentIndex = fillApps(agentIndex, noToBeFilled, i);
				else
					agentIndex = addApps(agentIndex, noToBeFilled, i);
				{
					agentDivision[agentDivisionIndex] = noToBeFilled;
					agentDivisionIndex++;
				}
			}
			noOfAppsFilled = i;
		}
		else if (remainderNoAgents >= minCapacity || (additionalNoOfAgentsPerApp+noOfAgentsPerApp+1) > MAX_CAPACITY)
		{
			noRequiredApps++;
			extendsNoOfApp = true;
		}
		
		for (int k = noOfAppsFilled; k < (noRequiredApps); k++)
		{
			int noToBeFilled = 0;
			if ((noRequiredApps - k) == 1 && extendsNoOfApp)
			{
				noToBeFilled = remainderNoAgents;
				{
					agentDivision[agentDivisionIndex] = noToBeFilled;
					agentDivisionIndex++;
				}
			}
			else
			{
				noToBeFilled = noOfAgentsPerApp;
				{
					agentDivision[agentDivisionIndex] = noToBeFilled;
					agentDivisionIndex++;
				}
			}
			
			if (mode.equals(Mode.FILL) == true)
				agentIndex = fillApps(agentIndex, noToBeFilled, k);
			else
				agentIndex = addApps(agentIndex, noToBeFilled, k);
		}	
	}
	
	public int fillApps (int agentIndex, int noToBeFilled, int appIndex)
	{
		int endIndex = agentIndex + noToBeFilled;
		AgentStateList newAgentStateList = new AgentStateList();
		AgentDataContainer newAgentDataContainer = new AgentDataContainer();
		
		for (;agentIndex < endIndex; agentIndex++)
		{
			AgentState agentState = agentStateList.get(agentIndex);
			String agentID = agentState.getID();
			ActionPerceptContainer actionPerceptContainer = agentDataContainer.get(agentID);
			
			newAgentStateList.add(agentState);
			newAgentDataContainer.put(agentID, actionPerceptContainer);
		}
		
		AddressAgentListTuple appsInfo = addressTable.get(appIndex);
		appsInfo.setAgentList(newAgentStateList);
		appsInfo.setAgentDataContainer(newAgentDataContainer);
		
		return agentIndex;
	}
	
	public int addApps (int agentIndex, int noToBeFilled, int appIndex)
	{
		AddressAgentListTuple appsInfo = addressTable.get(appIndex);
		int endIndex = agentIndex + noToBeFilled;
		AgentStateList newAgentStateList = appsInfo.getAgentList();
		AgentDataContainer newAgentDataContainer = appsInfo.getAgentDataContainer();
		
		for (;agentIndex < endIndex; agentIndex++)
		{
			AgentState agentState = agentStateList.get(agentIndex);
			String agentID = agentState.getID();
			
			ActionPerceptContainer actionPerceptContainer = agentDataContainer.get(agentID);
			if (actionPerceptContainer == null)
			{
				actionPerceptContainer = new ActionPerceptContainer();
			}
			
			newAgentStateList.add(agentState);
			newAgentDataContainer.put(agentID, actionPerceptContainer);
		}
		
		
		appsInfo.setAgentList(newAgentStateList);
		appsInfo.setAgentDataContainer(newAgentDataContainer);
		
		return agentIndex;
	}

}
