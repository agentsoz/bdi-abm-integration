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


import io.github.agentsoz.abmjadex.data_structure.SyncInteger;
import jadex.commons.SimplePropertyObject;
import jadex.extension.envsupport.environment.IEnvironmentSpace;
import jadex.extension.envsupport.environment.ISpaceAction;

import java.util.Map;


/**
 *  Action for updating the booking keeping of floating messages in a CO
 */
public class FloatingMsgUpdateAction extends SimplePropertyObject implements ISpaceAction
{
	
	@SuppressWarnings("rawtypes")
	public  Object perform(Map parameters, IEnvironmentSpace space)
	{	
		Integer retVal = null;
		
//		synchronized(this)
//		{
			try
			{
			//System.out.println("INSIDE FLOATING MSG UPDATE - ");
			SyncInteger floatingMsgNum = (SyncInteger)space.getProperty("floatingMsgNum");
			Integer valueToAdd = (Integer)parameters.get("value");
			floatingMsgNum.add(valueToAdd);
			retVal = floatingMsgNum.read();
			
//			int val = valueToAdd;
//			if (val != 0)
//				System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~In :"+ retVal +" - valueToAdd :"+val);
//			else
//				System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~Read :"+ retVal);
//			
			space.setProperty("floatingMsgNum", floatingMsgNum);
			//System.out.println("END OF FLOATING MSG UPDATE");
			}
			catch (Exception e)
			{
				//System.out.println("EXCEPTION IN FLOATING MSG UPDATE");
				e.printStackTrace();
			}
//		}
		
		return retVal;
	}


}


