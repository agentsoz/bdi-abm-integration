package io.github.agentsoz.abmjadex.data_structure;

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

import java.io.Serializable;

import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.bdiabm.data.AgentStateList;

public class AddressAgentListTuple implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -639482928634207120L;
	
	private CentralServerInterface remoteCO;
	private boolean isIdle;
	private AgentStateList agentStateList;
	private AgentDataContainer agentDataContainer;
	
	public AddressAgentListTuple (CentralServerInterface remoteCO) 
	{
		this.remoteCO = remoteCO;
		agentStateList = new AgentStateList();
		agentDataContainer = new AgentDataContainer();
		isIdle = false;
	}
	
	public boolean isIdle ()
	{
		return isIdle;
	}
	
	public void setIdleState(boolean isIdle)
	{
		this.isIdle = isIdle;
	}
	public CentralServerInterface getRemoteCO()
	{
		return remoteCO;
	}
	
	public void setAgentList (AgentStateList agentList)
	{
		this.agentStateList = agentList;
	}
	
	public void setAgentDataContainer (AgentDataContainer container)
	{
		this.agentDataContainer = container;
	}
	
	public AgentStateList getAgentList ()
	{
		return this.agentStateList;
	}
	
	public AgentDataContainer getAgentDataContainer ()
	{
		return this.agentDataContainer;
	}
	
	@Override
	public boolean equals (Object o)
	{
		boolean isEquals = false;
		if (o.getClass().equals(getClass()))
		{
			AddressAgentListTuple other = (AddressAgentListTuple)o;
			if (remoteCO.equals(other.remoteCO))
			{
				isEquals = true;
			}
		}
		return isEquals;
	}
}
