package io.github.agentsoz.abmjadex.central_organizer;

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


import io.github.agentsoz.abmjadex.central_organizer.ExternalCommunicator.Methods;
import io.github.agentsoz.abmjadex.data_structure.SyncInteger;
import io.github.agentsoz.abmjadex.super_central.SuperCentralServerInterface;
import jadex.commons.SimplePropertyObject;
import jadex.extension.envsupport.environment.IEnvironmentSpace;
import jadex.extension.envsupport.environment.ISpaceAction;
import jadex.extension.envsupport.environment.ISpaceObject;





import java.util.HashMap;
import java.util.Map;

import io.github.agentsoz.bdiabm.data.ActionContainer;
import io.github.agentsoz.bdiabm.data.ActionPerceptContainer;
import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.bdiabm.data.AgentState;
import io.github.agentsoz.bdiabm.data.AgentStateList;
import io.github.agentsoz.bdiabm.data.PerceptContainer;

/**
 *  Action for starting interapp application
 */
public class IdleAction extends SimplePropertyObject implements ISpaceAction
{
	@SuppressWarnings("rawtypes")
	public Object perform(Map parameters, IEnvironmentSpace space)
	{	
		//Update the corresponding agent into idle
		String agentID = (String)parameters.get("agentID");
		AgentStateList agentStateList = (AgentStateList)space.getProperty("agentStateList");		
		agentStateList.setState(agentID, true);
		
		//Update global ActionPerceptContainer
		ISpaceObject spaceObject = space.getSpaceObject(parameters.get("objectID"));
		ActionContainer actionContainer = (ActionContainer) spaceObject.getProperty("actionContainer");
		PerceptContainer perceptContainer = (PerceptContainer) spaceObject.getProperty("perceptContainer");
		
		AgentDataContainer agentDataContainer 
					= (AgentDataContainer)space.getProperty("agentDataContainer");
		ActionPerceptContainer actionPerceptContainer = agentDataContainer.get(agentID);
		actionPerceptContainer.setActionContainer(actionContainer);
		actionPerceptContainer.setPerceptContainer(perceptContainer);
		space.setProperty("agentDataContainer", agentDataContainer);
		
		//Check All agent Idle or not
		boolean allIdle = true;
	///int noOfIdle = 0;
		for (AgentState agentState : agentStateList)
		{
			if (!agentState.isIdle())
			{
				allIdle = false;
			}
			else
			{
				//noOfIdle++;
			}
		}
		
		/*
		 * To prevent multiple calls of Repast's server in a time step,
		 * server's control status need to be took care of.
		 * (Calls to take control.)
		 */
		StartPlan.BDIServer server = (StartPlan.BDIServer)space.getProperty("server");
		int floatingMsg = readFloatingMsgNum(space);
	//System.out.println("In CO :" + floatingMsg +" "+allIdle+"-"+noOfIdle );
		if (allIdle && server.isInControl() && floatingMsg == 0)
		{
			server.setControlStatus(false);
			StartPlan.BDIServer myServer = server;
			SuperCentralServerInterface superCentral = server.getSCServer();
			ExternalCommunicator extComm = new ExternalCommunicator(superCentral, myServer, Methods.INFORM, null);
			extComm.start();
		}
		
		return null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private int readFloatingMsgNum (IEnvironmentSpace env)
	{
		Map parameters = new HashMap();
		parameters.put("value", 0);
		//int retVal = (Integer)env.performSpaceAction("update_float_action", parameters);
		SyncInteger floatingMsgNum = (SyncInteger)env.getProperty("floatingMsgNum");
		int retVal = floatingMsgNum.read();
		return retVal;
	}
}


