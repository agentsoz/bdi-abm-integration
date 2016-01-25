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


import java.io.Serializable;

/**
 * A class which holds an agent IComponentIdentifier
 * and the respective message that is sent to them
 * @author Andreas
 *
 */
public class ReceiverSenderTuple implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 6361043954192412533L;
	
	private String receiver;
	private String sender;
	
	public ReceiverSenderTuple (String receiver, String sender)
	{
		this.receiver = receiver;
		this.sender = sender;
	}
	
	@Override
	public String toString()
	{
		return "ReceiverSenderTuple{"+receiver+"-"+sender+"}";
	}
	
	@Override
	public boolean equals(Object o)
	{
		boolean isEquals = false;
		if (o.getClass().equals(this.getClass()))
		{
			ReceiverSenderTuple other = (ReceiverSenderTuple)o;
			if (other.receiver.equals(this.receiver) && other.sender.equals(this.sender))
			{
				isEquals = true;
			}
		}
		return isEquals;
	}
}
