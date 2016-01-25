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
import io.github.agentsoz.abmjadex.super_central.SuperExternalCommunicator.Methods;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.github.agentsoz.bdiabm.data.AgentState;
import io.github.agentsoz.bdiabm.data.AgentStateList;
import jadex.commons.SimplePropertyObject;
import jadex.extension.envsupport.environment.IEnvironmentSpace;
import jadex.extension.envsupport.environment.ISpaceAction;

public class SuperKillAgentsAction extends SimplePropertyObject implements ISpaceAction 
{
	/**
	 * Performs the action.
	 * @param parameters parameters for the action
	 * @param space the environment space
	 * @return action return value
	 */
	@SuppressWarnings("rawtypes")
	public Object perform (Map parameters, IEnvironmentSpace space)
	{			
		HashMap<Integer, String[]> appKillList = new HashMap<Integer,String[]>();
	
		//Convert the array of agentIDs into List
		String[] agentIDsInArray = (String[])parameters.get("agentIDs");
		ArrayList<String> agentIDs = new ArrayList<String>();
		for (int i = 0; i < agentIDsInArray.length; i++)
		{
			agentIDs.add(agentIDsInArray[i]);
		}
		
		AddressTable addressTable = (AddressTable)space.getProperty("addressTable");
		
		//Divide the agent to be killed information into their respective CO where
		//it is lived in.
		for (int j = 0; j < addressTable.size(); j++)
		{
			ArrayList<String> killListPerApp = new ArrayList<String>();
			AddressAgentListTuple app = addressTable.get(j);
			AgentStateList agentList = app.getAgentList();
			
			if (agentIDs.isEmpty() == false)
			{
				for (AgentState agent : agentList)
				{
					for (String agentID : agentIDs)
					{
						if (agentID.equals(agent.getID()))
						{
							killListPerApp.add(agentID);	
						}
					}
					for (String agentID :killListPerApp)
					{
						agentIDs.remove(agentID);
					}
				}
				for (String agentID :killListPerApp)
				{
					agentList.remove(agentID);
				}
			}
			//Update the list of agent to be killed per every CO.
			
			String[] killArrayPerApp = new String[killListPerApp.size()];
			killArrayPerApp = killListPerApp.toArray(killArrayPerApp);
			
			appKillList.put(j, killArrayPerApp);
		}
		
		for (int k = 0; k < addressTable.size(); k++)
		{
			if (appKillList.get(k).length != 0)
			{
				Object[] mParameters = (Object[])appKillList.get(k);
				AddressAgentListTuple app = addressTable.get(k);
				SuperExternalCommunicator comm 
					= new SuperExternalCommunicator(null, app.getRemoteCO(), Methods.KILL_AGENT, mParameters);
				comm.startAndWait();
			}
		}
		return null;
	}
	
	
		
}

