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
import java.util.ArrayList;

public class AddressTable extends ArrayList<AddressAgentListTuple> implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3468132987669107776L;

	/**
	 * This function is override from the ArrayList default implementation
	 * When the things to add is already contained in the list then
	 * this addition action is ignored.
	 */
	public boolean add (AddressAgentListTuple o)
	{
		boolean isAdded = false;
		if (!contains(o))
		{
			isAdded = super.add(o);
		}
		return isAdded;
	}
	
	/**
	 * Take a a data based on the address
	 * @param address
	 * @return
	 */
	public AddressAgentListTuple get (CentralServerInterface remoteCO)
	{
		AddressAgentListTuple val = null;
		AddressAgentListTuple x = new AddressAgentListTuple(remoteCO);
		int index = indexOf(x);
		if (index != -1)
		{
			val = get(index);
		}
		return val;
	}
}
