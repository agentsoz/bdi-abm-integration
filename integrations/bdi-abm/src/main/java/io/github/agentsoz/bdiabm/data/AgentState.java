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

/**
 * This is a tuple of agentID (a String) and 
 * the agent's Idle state (a boolean). The idle state is used
 * by Jadex to have a book keeping on the agent idle state,
 * Repast would mostly used this only at the start of the system
 * to send list of agent to Jadex.
 * @author Andreas
 *
 */
public class AgentState implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -4639216305591170109L;
	
	private String agentID;
	private boolean isIdle;
	
	/**
	 * Creating an instance with default idle state of
	 * non-idle.
	 * List of agent sent by Repast at the beginning
	 * should be in non-idle state.
	 * @param agentID the unique id of the agent
	 */
	public AgentState (String agentID)
	{
		this.agentID = new String(agentID);
		isIdle = false;
	}
	
	public boolean isIdle ()
	{
		return isIdle;
	}
	
	public String getID ()
	{
		return agentID;
	}
	
	public void setIdleState (boolean isIdle)
	{
		this.isIdle = isIdle;
	}
	
	@Override
	public boolean equals(Object o)
	{
		boolean isEquals = false;
		if(o.getClass() == this.getClass())
		{
			AgentState other = (AgentState)o;
			if(other.agentID.equals(agentID))
			{
				isEquals = true;
			}
		}
		return isEquals;
	}

}
