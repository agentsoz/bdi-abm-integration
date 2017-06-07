package io.github.agentsoz.conservation;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2017 by its authors. See AUTHORS file.
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
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.conservation.LandholderHistory.AuctionRound;
import io.github.agentsoz.conservation.jill.agents.Landholder;

/**
 * Extension Office keeps track of its extension officers and their
 * visits to the landholders given constraints/policies.
 * 
 * @author dsingh
 *
 */
public class ExtensionOffice {
	
    final private Logger logger = LoggerFactory.getLogger(Main.LOGGER_NAME);

	/**
	 * Extension office visiting policy. The impact of a visit on a landholder
	 * is an increase in her conservation ethics baromaeter value, as determined
	 * by the config parameter {@link ConservationUtils#getVisitConservationEthicBoostValue()}:
	 * <ul>
	 * <li> NEVER: No visits are ever conducted
	 * <li> ALWAYS: Landholders are visited prior to every auction
	 * </ul>
	 * 
	 */
	public enum Policy {
		NEVER,
		ALWAYS
	}
	
	// Handle to the global agent data container
	private AgentDataContainer adc;
	
	// Keep track of visits per agent
	private HashMap<String, Integer> visits;
	
	// Visitsig policy in use
	private VisitPolicy policy = new VisitPolicy(0,0,0);
	
	// Restricts the number of visits (due to resource constraints) per round;
	// expressed as a proportion of all agents
	private final double visitQuota = 1.0;

	public ExtensionOffice() {
		visits = new HashMap<String, Integer>();
	}
	
	public void setPolicy(Policy policy) {
		switch (policy) {
		case NEVER:
			setPolicy(0,0,0);
			break;
		case ALWAYS:
			setPolicy(0, 1, Integer.MAX_VALUE);
		}
	}
	
	public void setPolicy(int start, int frequency, int end) {
		VisitPolicy p = new VisitPolicy(start, frequency, end); 
		if (start < 0 || end < start || frequency < 1) {
			logger.warn("Extension Office received invalid visits policy" + p + "; will ignore");
			return;
		}
		policy = p;
	}
	
	public void conductVisits(int cycle) {
		// Check if the current cycle falls in the visiting policy
		if (cycle < policy.getStartCycle() || 
				cycle > policy.getEndCycle() || 
				policy.getFrequency() == 0 || 
				(cycle-policy.getStartCycle())%policy.getFrequency() != 0) 
		{
			logger.info("Cycle "+cycle+" not an extension office visit round" + 
				" (policy is " + policy + "), so no visits conducted");
			return;
		}
		logger.info("Extension office will conduct visits; cycle is "+ cycle + 
				" and policy is " + policy);

		// Winning and in-contract land holders will be visited by an extension officer
		for (String name : visits.keySet()) {
			// If the agent has at least one active contract]
			Landholder agent = Main.getLandholder(name);
			int active = agent.getContracts().activeCount(); 
			if ( active > 0 ) {
				logger.info("Agent "+name+" with contracts "+agent.getContracts()+" will be visited by extension officer" );
				adc.getOrCreate(name).getPerceptContainer().put(
						Global.percepts.EXTENSION_OFFICER_VISIT.toString(), 
						null);
				// Record the visit
				visits.put(name, visits.get(name)+1);
				// Decrement the contracts remaining time
				agent.getContracts().decrementYearsLeftOnAllContracts();
			}
		}
	}
	
	public void init(AgentDataContainer adc, String[] agents) {
		this.adc = adc;

		// Nothing else to do if no agents were given
		if (agents == null || agents.length == 0) {
			return;
		}
		// Initialise visits to all agents 
		for (String agent : agents) {
			visits.put(agent, 0);
		}
	}


	private class VisitPolicy {
		private int startCycle;
		private int frequency;
		private int endCycle;
		
		
		public VisitPolicy(int startCycle, int frequency, int endCycle) {
			super();
			this.startCycle = startCycle;
			this.frequency = frequency;
			this.endCycle = endCycle;
		}
		
		public int getStartCycle() {
			return startCycle;
		}
		public void setStartCycle(int startCycle) {
			this.startCycle = startCycle;
		}
		public int getFrequency() {
			return frequency;
		}
		public void setFrequency(int frequency) {
			this.frequency = frequency;
		}
		public int getEndCycle() {
			return endCycle;
		}
		public void setEndCycle(int endCycle) {
			this.endCycle = endCycle;
		}
		
		public String toString() {
			return "[" + 
					startCycle + "," + 
					frequency + "," + 
					endCycle + 
					"]";
		}
	}
}
