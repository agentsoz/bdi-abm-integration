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
import io.github.agentsoz.abmjadex.super_central.SuperCentralServerInterface;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.util.logging.Logger;


public class ExternalCommunicator extends Thread
{
	private final static Logger LOGGER = Logger.getLogger(ExternalCommunicator.class.getName());
	
	public enum Methods 
	{
		INFORM, INTER_CENTRAL
	}
	
	public enum TargetType
	{
		JADEX_CENTRAL, REPAST 
	}
	
	private Methods method;
	private Object targetServer;
	private Object callerServer;
	private Object[] parameters;
	
	public ExternalCommunicator (Object targetServer
								, Object callerServer, Methods methodID, Object[] parameters)
	{
		ABMBDILoggerSetter.setup(LOGGER);
		this.targetServer = targetServer;
		this.method = methodID;
		this.parameters = parameters;
		this.callerServer = callerServer;
	}
			
	/**
	 * The command to do remote method invocation
	 * @param targetAddress
	 * @param methodToCall
	 * @return
	 */
	public Object startAndWait ()
	{
		Object returnValue = null;
		System.setSecurityManager(new RMISecurityManager());
		try 
		{
			if (method.equals(Methods.INFORM))
			{
				informIdle();
			}
			else if (method.equals(Methods.INTER_CENTRAL))
			{
				interCentralSent();
			}
		
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
		return returnValue;
	}
	
	private void informIdle() throws MalformedURLException, RemoteException, NotBoundException
	{
		((SuperCentralServerInterface)targetServer).informIdleState((CentralServerInterface)callerServer);
	}
	
	private void interCentralSent() throws MalformedURLException, RemoteException, NotBoundException
	{
		((SuperCentralServerInterface)targetServer).interCentralSent(parameters[0], (CentralServerInterface)callerServer);
	}
	
	@Override
	public void run ()
	{
		startAndWait();
	}
}
