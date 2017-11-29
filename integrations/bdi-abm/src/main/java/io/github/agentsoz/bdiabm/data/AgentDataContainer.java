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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
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
public class AgentDataContainer implements Serializable
{	 
	/**
	 * 
	 */
	private static final long serialVersionUID = -3486877478758677517L;

	Map <String, ActionPerceptContainer> map;

	public AgentDataContainer() {
	  map = Collections.synchronizedMap(new LinkedHashMap <String, ActionPerceptContainer>());
	}
	
	
	/**
	 * Get an actionPerceptContainer, if it did not exist then
	 * create a new data.
	 * @param agentID
	 * @return
	 * ActionPerceptContainer
	 */
	public synchronized ActionPerceptContainer getOrCreate (String agentID) {
		// yy I think that one should remove the level of ActionPerceptContainer and have "getOrCreatePerceptContent" & 
		// "getOrCreateActionContent" directly.  kai, nov'17
		if (!map.containsKey(agentID)) {
			map.put(agentID, new ActionPerceptContainer());
		}
		return map.get(agentID);
	}
	
	@Override
	public synchronized String toString() {
		if (map.isEmpty()) {
			return "{}";
		}
		String s = "{";
		for (String key : map.keySet()) {
			ActionPerceptContainer apc = map.get(key);
			if (!apc.isEmpty()) {
				s += "\n\""+key+"\":" + apc.toString();
			}
		}
		s += "}";
		return s;
		//return (isEmpty()) ? "{}" : new Gson().toJson(this);
	}


	public synchronized void copy(AgentDataContainer adc) {
		// lock both containers before proceeding
		synchronized (adc) {
			Iterator<String> i = adc.getAgentIDs();
			while (i.hasNext()) {
				String agentID = i.next();
				ActionPerceptContainer apcNext = adc.getOrCreate(agentID);
				ActionPerceptContainer apc = this.getOrCreate(agentID);
				apc.getPerceptContainer().copy(apcNext.getPerceptContainer());
				apc.getActionContainer().copy(apcNext.getActionContainer());
				map.put(agentID,apc);
			}
		}
	}

	public synchronized boolean isEmpty() {
    return map.isEmpty();
  }

  public synchronized PerceptContainer getPerceptContainer(String agentID) {
    if (map.containsKey(agentID)) {
      return map.get(agentID).getPerceptContainer();
    }
    return null;
  }
  
  public synchronized ActionContainer getActionContainer(String agentID) {
    if (map.containsKey(agentID)) {
      return map.get(agentID).getActionContainer();
    }
    return null;
  }
  
  public synchronized Iterator<String> getAgentIDs() {
    return map.keySet().iterator();
  }


  public synchronized ActionPerceptContainer remove(String agentID) {
    return map.remove(agentID);
  }

  public synchronized void removeAll() {
	map.clear();
  }

}
