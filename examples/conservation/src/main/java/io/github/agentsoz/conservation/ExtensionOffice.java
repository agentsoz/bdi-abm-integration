package io.github.agentsoz.conservation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.github.agentsoz.bdiabm.data.AgentDataContainer;

/**
 * Extension Office keeps track of its extension officers and their
 * visits to the landholders given constraints/policies.
 * 
 * @author dsingh
 *
 */
public class ExtensionOffice {
	
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
			Log.warn("Extension Office received invalid visits policy" + p + "; will ignore");
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
			Log.info("Cycle "+cycle+" not an extension office visit round" + 
				" (policy is " + policy + "), so no visits conducted");
			return;
		}
		Log.info("Extension office will conduct visits; cyle is "+ cycle + 
				" and policy is " + policy);

		// Visit from extension officers
		adc.getOrCreate("global").getPerceptContainer()
		.put(Global.percepts.EXTENSION_OFFICER_VISIT.toString(), null);
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
