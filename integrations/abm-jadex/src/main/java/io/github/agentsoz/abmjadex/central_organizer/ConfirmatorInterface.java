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


import java.rmi.RemoteException;

public interface ConfirmatorInterface extends java.rmi.Remote
{
	public enum Events
	{
		REGISTER, UNREGISTER, START_SUPER, RETRIEVE
	}
	
	/**
	 * Confirmation of previous method calls from
	 * the caller as listed in the address param
	 * @param address
	 * Address of the remote object who do the confirmation
	 * @param eventType
	 * The type of event to be confirmed
	 */
	public void confirm (CentralServerInterface remoteCO, Events eventType
			, boolean isSuccessful, Object[] parameters) throws RemoteException;
}
