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


import jadex.bdi.runtime.IGoal;

public class SuspendedGoals 
{
	private IGoal goal = null;
	private Boolean isTopLevel = false;
	private Boolean isMessage = false;
	/**
	 * 
	 * @param goal
	 * @param isTopLevel
	 * The goal type
	 * @param isMessage
	 */
	public SuspendedGoals(IGoal goal, Boolean isTopLevel, Boolean isMessage)
	{
		this.goal = goal;
		this.isTopLevel = isTopLevel;
		this.isMessage = isMessage;
	}
	
	public IGoal getGoal()
	{
		return this.goal;
	}
	
	public Boolean isTopLevel()
	{
		return this.isTopLevel;
	}
	
	public Boolean isMessage()
	{
		return this.isMessage;
	}
	
	@Override
	public boolean equals(Object o)
	{
		Boolean isEquals = false;
		if (o.getClass() == this.getClass())
		{
			if (this.goal.equals(((SuspendedGoals)o).getGoal()))
			{
				isEquals = true;
			}	
		}
		return isEquals;
	}
}
