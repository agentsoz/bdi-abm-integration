package io.github.agentsoz.abmjadex.agent;

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


import io.github.agentsoz.abmjadex.central_organizer.StartPlan;
import io.github.agentsoz.abmjadex.miscellaneous.ABMBDILoggerSetter;

import java.rmi.RemoteException;
import java.util.logging.Logger;

import io.github.agentsoz.bdiabm.ABMRemoteServerInterface;

public class AdhocPerceptQuery 
{
	private final static Logger LOGGER = Logger.getLogger(AdhocPerceptQuery.class.getName());
	/**
	 * The static class which consist of method to
	 * get a percept value from Repast. This is used
	 * within the Belief definition
	 * @param agentID
	 * @param perceptID
	 * @return
	 */
	public static Object query(String agentID, String perceptID, Object bdiServer)
	{
		ABMBDILoggerSetter.setup(LOGGER);
		
		Object perceptValue = null;	
		ABMRemoteServerInterface abmServer  = 
				((StartPlan.BDIServer)bdiServer).getAbmServer();
		try 
		{
			perceptValue = abmServer.queryPercept(agentID, perceptID);
		} 
		catch (RemoteException e) 
		{
			e.printStackTrace();
			LOGGER.severe(e.getMessage());
		} 
		
		return perceptValue;
	}
}
