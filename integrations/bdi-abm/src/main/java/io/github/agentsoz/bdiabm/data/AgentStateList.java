package io.github.agentsoz.bdiabm.data;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2022 by its authors. See AUTHORS file.
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


import java.io.Serializable;
import java.util.ArrayList;

public class AgentStateList extends ArrayList<AgentState> implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -6362126199555720030L;

	@Override
	public boolean add(AgentState e)
	{
		boolean isAdded = false;
		if (!this.contains(e))
		{
			super.add(e);
		}
		return isAdded;
	}
	
	/**
	 * get the idle state of the agent
	 * @param agentID the unique id of the agent
	 * @return whether the agent is idle or not
	 */
	public boolean isIdle (String agentID)
	{
		AgentState agentState = new AgentState(agentID);
		int index = this.indexOf(agentState);
		return get(index).isIdle();
	}
	
	/**
	 * Set the idleState of the corresponding AgentState, 
	 * instance with the agentID, into the value of 
	 * parameter isIdle.
	 * @param agentID the unique id of the agent
	 * @param isIdle whether the agent is idle or not
	 */
	public void setState (String agentID, Boolean isIdle)
	{
		int index = this.indexOf(new AgentState(agentID));
		this.get(index).setIdleState(isIdle);
	}

	
}
