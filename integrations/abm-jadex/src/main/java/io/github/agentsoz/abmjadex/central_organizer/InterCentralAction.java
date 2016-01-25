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
import io.github.agentsoz.abmjadex.data_structure.ReceiverSenderTuple;
import io.github.agentsoz.abmjadex.super_central.SuperCentralServerInterface;
import jadex.commons.SimplePropertyObject;
import jadex.extension.envsupport.environment.IEnvironmentSpace;
import jadex.extension.envsupport.environment.ISpaceAction;

import java.util.Map;

public class InterCentralAction extends SimplePropertyObject implements ISpaceAction 
{
	//The action to keep all central organizer idle state's integrity
	/**
	 * Performs the action.
	 * @param parameters parameters for the action
	 * @param space the environment space
	 * @return action return value
	 */
	@SuppressWarnings("rawtypes")
	public Object perform(Map parameters, IEnvironmentSpace space)
	{	
		StartPlan.BDIServer server = (StartPlan.BDIServer)space.getProperty("server");
		SuperCentralServerInterface superServer = server.getSCServer();
		CentralServerInterface myServer = server;
		
		Object[] params = new Object[1];
		
		if(parameters.get("receivers") != null)
		{
			//This branch means the info is from the sender of message		
			params[0] = parameters;	
		}
		else
		{
			//This branch means the info is from the receiver of message
			String receiver = (String)parameters.get("receiver");
			String sender = (String)parameters.get("sender");
			ReceiverSenderTuple agentMessage = new ReceiverSenderTuple(receiver,sender);
			
			params[0] = agentMessage;
		}
		
		ExternalCommunicator comm 
			= new ExternalCommunicator(superServer, myServer, Methods.INTER_CENTRAL, params);
		comm.startAndWait();
		return null;
	}
}
