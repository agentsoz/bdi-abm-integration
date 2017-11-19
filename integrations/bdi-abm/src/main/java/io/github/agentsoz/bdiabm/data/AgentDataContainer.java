package io.github.agentsoz.bdiabm.data;

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
import java.util.LinkedHashMap;
/**
 * 2/20/2013
 * AgentDataContainer is a Map of 
 * agentID as the keys (which is a String), 
 * 			this shall be the one instantiated by Repast
 * 			It has the format of "SIMULATION_NAME.AGENT_TYPE@REPAST_UNIQUE", this is
 * 			according to the format of ID created by Repast.
 * ActionPerceptContainer as the value
 * 			This is the container of actions and percepts of an agent
 * @author Andreas
 *
 */
public class AgentDataContainer extends LinkedHashMap <String, ActionPerceptContainer> implements Serializable
{	 
	/**
	 * 
	 */
	private static final long serialVersionUID = -3486877478758677517L;
	

	/**
	 * Get an actionPerceptContainer, if it did not exist then
	 * create a new data.
	 * @param agentID
	 * @return
	 * ActionPerceptContainer
	 */
	public ActionPerceptContainer getOrCreate (String agentID) {
		// yy I think that one should remove the level of ActionPerceptContainer and have "getOrCreatePerceptContent" & 
		// "getOrCreateActionContent" directly.  kai, nov'17
		
		ActionPerceptContainer container = this.get(agentID);
		
		if (container == null)
		{
			container = new ActionPerceptContainer();
			this.put(agentID, container);
		}
		
		return container;
	}
	
	@Override
	public String toString() {
		if (isEmpty()) {
			return "{}";
		}
		String s = "{";
		for (String key : this.keySet()) {
			ActionPerceptContainer apc = this.get(key);
			if (!apc.isEmpty()) {
				s += "\n\""+key+"\":" + apc.toString();
			}
		}
		s += "}";
		return s;
		//return (isEmpty()) ? "{}" : new Gson().toJson(this);
	}

}
