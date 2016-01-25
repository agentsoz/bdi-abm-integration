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
import io.github.agentsoz.abmjadex.data_structure.AddressTable;
import io.github.agentsoz.abmjadex.super_central.SuperExternalCommunicator.Methods;
import io.github.agentsoz.abmjadex.super_central.SuperExternalCommunicator.TargetType;
import jadex.commons.SimplePropertyObject;
import jadex.extension.envsupport.environment.IEnvironmentSpace;
import jadex.extension.envsupport.environment.ISpaceAction;

import java.util.Map;
import java.util.Set;

import io.github.agentsoz.bdiabm.data.ActionPerceptContainer;
import io.github.agentsoz.bdiabm.data.AgentDataContainer;

public class SuperTakeControlAction extends SimplePropertyObject implements ISpaceAction
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
		AgentDataContainer agentDataContainer = (AgentDataContainer)parameters.get("agentDataContainer");
		AddressTable addressTable = (AddressTable)space.getProperty("addressTable");
		Set<String> agentIDSet = agentDataContainer.keySet();
		
		//Update The addressTable
		for (String agentID : agentIDSet)
		{
			for (int i = 0; i < addressTable.size(); i++)
			{
				AgentDataContainer thisADContainer = addressTable.get(i).getAgentDataContainer();
				ActionPerceptContainer thisAPContainer = thisADContainer.get(agentID);
				//If the agent is contained in the Central Organizer we are looking at
				//Note : Central Organizer is an instance of Jadex's application
				if (thisAPContainer != null)
				{
					addressTable.get(i).setIdleState(false);
					thisAPContainer.replace(agentDataContainer.get(agentID));
					break;
				}
			}
		}
		
		
		//Give Control to each apps
		int noOfAppUsed = 0;
		for (int i = 0; i < addressTable.size(); i++)
		{
			//if (addressTable.get(i).getAgentList().isEmpty() == false)
			{
				//Update the status of the Central Organizer to be unidle
				addressTable.get(i).setIdleState(false);
				noOfAppUsed++;
				Object[] methodParameters = new Object[1];
				methodParameters[0] = addressTable.get(i).getAgentDataContainer();
				CentralServerInterface targetInstance = addressTable.get(i).getRemoteCO();
				
				//Actual unidling of the Central Organizer
				SuperExternalCommunicator extComm = new SuperExternalCommunicator (TargetType.JADEX_CENTRAL
													,targetInstance,Methods.TAKE_CONTROL,methodParameters);
				
				extComm.start();
			}
		}
		space.setProperty("noOfAppToConfirm", noOfAppUsed);
		return null;
	}

}

