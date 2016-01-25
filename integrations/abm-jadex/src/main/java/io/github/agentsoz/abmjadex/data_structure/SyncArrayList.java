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


import java.util.ArrayList;

public class SyncArrayList 
{
	@SuppressWarnings("rawtypes")
	private ArrayList list;
	
	@SuppressWarnings("rawtypes")
	public SyncArrayList ()
	{
		list = new ArrayList();
	}
	
	@SuppressWarnings("unchecked")
	public boolean add (Object e)
	{
		return list.add(e);
	}
	
	public Object remove (int index)
	{
		return list.remove(index);
	}
	
	public  Object remove (Object o)
	{
		return list.remove(o);
	}
	
	public boolean contains (Object o)
	{
		return list.contains(o);
	}
	
	public  int indexOf (Object o)
	{
		return list.indexOf(o);
	}
	
	public  Object get (int index)
	{
		return list.get(index);
	}
	
	public int size ()
	{
		return list.size();
	}
}
