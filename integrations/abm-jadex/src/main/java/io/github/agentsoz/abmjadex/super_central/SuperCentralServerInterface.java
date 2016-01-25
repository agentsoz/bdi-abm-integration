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

import java.rmi.RemoteException;

public interface SuperCentralServerInterface extends CentralServerInterface
{
	public boolean REGISTER = true;
	public boolean UNREGISTER = false;

	/**
	 * A method called by Central Organizer
	 * to be under this Super Central Organizer Management
	 * @param registrationAction
	 * To decide whether it want to REGISTER(true) or UNREGISTER(false)
	 * @param address
	 * The address of the respective Central Organizer
	 */
	public void administerRegistration (boolean registrationAction
			, CentralServerInterface remoteCO) throws RemoteException;
	
	/**
	 * Informing the Super Central that a central organizer
	 * with the respective address is decided to be idle.
	 * @param appAddress
	 * @throws RemoteException
	 */
	public void informIdleState(CentralServerInterface remoteApp) throws RemoteException;
	
	/**
	 * Informing the super central that a central sent an inter-central
	 * message
	 * @param params
	 * @throws RemoteException
	 */
	public void interCentralSent(Object params, CentralServerInterface callerInstance) throws RemoteException;
}
