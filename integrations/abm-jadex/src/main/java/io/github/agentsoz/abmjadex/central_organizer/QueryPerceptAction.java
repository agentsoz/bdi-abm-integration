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



import io.github.agentsoz.abmjadex.miscellaneous.ABMBDILoggerSetter;
import jadex.commons.SimplePropertyObject;
import jadex.extension.envsupport.environment.IEnvironmentSpace;
import jadex.extension.envsupport.environment.ISpaceAction;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.logging.Logger;

import io.github.agentsoz.bdiabm.ABMRemoteServerInterface;

/**
 *  Action for starting interapp application
 */
public class QueryPerceptAction extends SimplePropertyObject implements ISpaceAction
{
	private final static Logger LOGGER = Logger.getLogger(QueryPerceptAction.class.getName());
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
		Object perceptValue = null;
		String agentID = (String)parameters.get("agentID");
		String perceptID = (String)parameters.get("perceptID");
		try 
		{
			ABMRemoteServerInterface abmServer =(ABMRemoteServerInterface)Naming.lookup(ABMRemoteServerInterface.ADDRESS);
			perceptValue = abmServer.queryPercept(agentID, perceptID);
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
		
		return perceptValue;
	}

}


