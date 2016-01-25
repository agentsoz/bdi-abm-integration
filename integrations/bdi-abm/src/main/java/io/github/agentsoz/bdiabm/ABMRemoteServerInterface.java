package io.github.agentsoz.bdiabm;

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


import io.github.agentsoz.bdiabm.data.AgentDataContainer;

import java.rmi.RemoteException;

public interface ABMRemoteServerInterface extends java.rmi.Remote
{
	public final static String ADDRESS = "rmi://localhost:1205/ABMServerK";
	
	/**
	 * Method signaling the time for Repast to 
	 * take over the control of the turns 
	 * @param actionPerceptContainer
	 * @throws RemoteException
	 */
	public void takeControl (AgentDataContainer agentDataContainer) throws RemoteException;
	
	/**
	 * Giving the percept value.
	 * @param agentID
	 * agent identifier
	 * @param perceptID
	 * percept identifier/type
	 * @return
	 * percept value
	 * @throws RemoteException
	 */
	public Object queryPercept (String agentID, String perceptID) throws RemoteException;

}
