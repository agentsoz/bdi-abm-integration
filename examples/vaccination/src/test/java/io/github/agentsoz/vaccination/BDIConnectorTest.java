package io.github.agentsoz.vaccination;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2016 by its authors. See AUTHORS file.
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

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import io.github.agentsoz.bdiabm.ABMServerInterface;
import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.vaccination.controller.BDIConnector;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BDIConnectorTest implements ABMServerInterface {

	private BDIConnector bdiConnector;
	private String[] createAgentIDs = new String[] { "1", "2", "3", "4", "5",
			"6", "7", "8", "9", "10" };
	private String[] killAgentIDs = new String[] { "2", "4", "6", "8", "10" };

	@Before
	public void setUp() throws Exception {
		bdiConnector = new BDIConnector();
		bdiConnector.init(new AgentDataContainer(), null, this, null);
	}

	/**
	 * Test createAgents method
	 */
	@Test
	public void testCreateAgents() {
		bdiConnector.createAgents(createAgentIDs);
		List<String> agentIds = Arrays.asList(bdiConnector.getAgentIDs());

		for (String expectedId : createAgentIDs) {
			if (!agentIds.contains(expectedId)) {
				fail("Error in createAgents method. Could not find the agent "
						+ expectedId);
			}
		}
	}

	/**
	 * Test createAgents method
	 */
	@Test
	public void testKillAgents() {
		// Update agent data container before testing killAgents method
		AgentDataContainer adc = new AgentDataContainer();
		for (String agentID : createAgentIDs) {
			adc.getOrCreate(agentID);
		}
		bdiConnector.takeControl(adc);

		// Test kill agents method
		bdiConnector.killAgents(killAgentIDs);
		List<String> agentIds = Arrays.asList(bdiConnector.getAgentIDs());

		for (String killedAgent : killAgentIDs) {
			if (agentIds.contains(killedAgent)) {
				fail("The killed agent " + killedAgent
						+ " is still avalable in the system");
			}
		}
	}

	@Override
	public void takeControl(AgentDataContainer agentDataContainer) {
		System.out.println("Called take controll method");
	}

	@Override
	public Object queryPercept(String agentID, String perceptID) {
		System.out.println("Called queryPercept method");
		return null;
	}
}
