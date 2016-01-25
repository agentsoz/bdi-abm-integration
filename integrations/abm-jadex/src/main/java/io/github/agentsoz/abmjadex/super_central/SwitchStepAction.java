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



import io.github.agentsoz.abmjadex.data_structure.AddressTable;
import io.github.agentsoz.abmjadex.miscellaneous.ABMBDILoggerSetter;
import io.github.agentsoz.abmjadex.super_central.SuperExternalCommunicator.Methods;
import io.github.agentsoz.abmjadex.super_central.SuperExternalCommunicator.TargetType;
import jadex.commons.SimplePropertyObject;
import jadex.extension.envsupport.environment.IEnvironmentSpace;
import jadex.extension.envsupport.environment.ISpaceAction;

import java.util.Map;
import java.util.logging.Logger;

import io.github.agentsoz.bdiabm.ABMRemoteServerInterface;
import io.github.agentsoz.bdiabm.data.AgentDataContainer;

public class SwitchStepAction extends SimplePropertyObject implements ISpaceAction
{	
	private final static Logger LOGGER = Logger.getLogger(SwitchStepAction.class.getName());
	/**
	 * Performs the action.
	 * @param parameters parameters for the action
	 * @param space the environment space
	 * @return action return value
	 */
	@SuppressWarnings("rawtypes")
	public Object perform(Map parameters, IEnvironmentSpace space)
	{	
		ABMBDILoggerSetter.setup(LOGGER);
		
		AddressTable addressTable = (AddressTable)space.getProperty("addressTable");
		
		AgentDataContainer totalData = new AgentDataContainer();
		for (int i = 0; i < addressTable.size(); i++)
		{
			totalData.putAll (addressTable.get(i).getAgentDataContainer());
		}
//		System.out.println("Hola "+totalData.size());
//		Set<String> agentID = totalData.keySet();
//		for (String x : agentID)
//		{
//			System.out.println(x+"APC: "+(totalData.get(x)==null));
//		}
		SuperStartPlan.BDIServer server = (SuperStartPlan.BDIServer)space.getProperty("server");
		ABMRemoteServerInterface abmServer = server.getABMInstance();
		Object[] methodParams = new Object[1];
		methodParams[0] = totalData;
		LOGGER.info("--Give Control To Repast--");
		SuperExternalCommunicator extComm 
			= new SuperExternalCommunicator(TargetType.REPAST, abmServer, Methods.TAKE_CONTROL, methodParams);
		
		extComm.start();
		
		return null;
	}

}


