package io.github.agentsoz.abmjill;

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

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.github.agentsoz.bdiabm.ABMServerInterface;
import io.github.agentsoz.bdiabm.data.ActionContainer;
import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.bdiabm.data.AgentState;
import io.github.agentsoz.bdiabm.data.AgentStateList;
import io.github.agentsoz.bdiabm.data.ActionContent.State;
import io.github.agentsoz.jill.util.Log;

public class JillModelTest {

    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;

	private TestModel jillmodel;
	private AgentDataContainer adc;
	private AgentStateList asl;
	private StubABM abmstub;

	@Before
	public void setUp() throws Exception {	
		// Set up I/O
        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        System.setErr(new PrintStream(err));
	}

	@After
	public void tearDown() throws Exception {
		jillmodel.finish();
	}

	@Test
	public void testDurativeActions() {
		// number of agents
		final int nAgents = 3;
		// number of steps per action
		final int durationPerAction = 4;
		// Output printed by TestAgent.updateAction(...) once per step
		final String[] output = { 
				"0:TESTACTION:PASSED",
				"0:TESTACTION:RUNNING",
				"0:TESTACTION:RUNNING",
				"0:TESTACTION:RUNNING",
				"0:TESTACTION:RUNNING",
				"1:TESTACTION:PASSED",
				"1:TESTACTION:RUNNING",
				"1:TESTACTION:RUNNING",
				"1:TESTACTION:RUNNING",
				"1:TESTACTION:RUNNING",
				"2:TESTACTION:PASSED",
				"2:TESTACTION:RUNNING",
				"2:TESTACTION:RUNNING",
				"2:TESTACTION:RUNNING",
				"2:TESTACTION:RUNNING",
		};
		
		// initialise the BDI and ABM system
		init(nAgents, durationPerAction);
		
		// Run for at least durationPerAction steps plus some to make sure 
		// we are definitely finished
		for (int i = 0; i < durationPerAction+10; i++) {
			jillmodel.takeControl(adc);
			abmstub.takeControl(adc);
		}
		
		// Check the output
		String[] list = out.toString().split(",");
		Arrays.sort(list);
		assertTrue(list.length == output.length);
		for (int i = 0; i < output.length; i++) {
			assertTrue(list[i].equals(output[i]));
		}
	}

	private void init(int nAgents, int durationPerAction) {
		// Initialise the data containers and add data for the agents
		adc = new AgentDataContainer();
		asl = new AgentStateList();
		for (int i = 0; i < nAgents; i++) {
			String agentID = String.format("%d", i);
			asl.add(new AgentState(agentID));
			adc.getOrCreate(agentID);
		}

		// Initialise the jillmodel
		String[] args = {
				"--config",
				"{agents:[{classname:io.github.agentsoz.abmjill.testagent.TestAgent, args:null, count:"+nAgents+"}]," +
				"logLevel: ERROR," +
				"logFile: " + JillModelTest.class.getSimpleName() + ".log," +
				"numThreads: 1" +
				"}"
				};
		jillmodel = new TestModel();
		abmstub = new StubABM(durationPerAction);
		jillmodel.init(adc, asl, abmstub, args);
		// Now start the BDI (Jill) system
		jillmodel.start();		
	}
	
	private class TestModel extends JillModel {
		
	}
	
	private class StubABM implements ABMServerInterface {

		// All actions will take this many steps to complete
		private int durationPerAction;
		
		// A map of agent:action -> count to keep track of how long
		// an action has been running
		private HashMap<String, Integer> agentsDurativeCount;
		
		public StubABM(int durationPerAction) {
			this.durationPerAction = durationPerAction;
			agentsDurativeCount = new LinkedHashMap<String, Integer>();
		}
		
		@Override
		public void takeControl(AgentDataContainer adc) {
			Log.trace("Stub ABM Received " + adc);
	        Iterator<String> i = adc.getAgentIDs();
	        while (i.hasNext()) {
	            String agentId = i.next();
				ActionContainer ac = adc.getOrCreate(agentId).getActionContainer();
				ActionContent acc = ac.get("TESTACTION");
				if (acc != null) {
					// The unique id for this agent:action
					String mapid = agentId+acc.getAction_type();
					// If just initiated then add to the map
					if (acc.getState() == State.INITIATED) {
						acc.setState(State.RUNNING);
						agentsDurativeCount.put(mapid, durationPerAction);
					}
					// If running then 
					if (acc.getState() == State.RUNNING) {
						// Count down
						int count = agentsDurativeCount.get(mapid);
						count--;
						agentsDurativeCount.put(mapid, count);
						// If done
						if (count < 0) {
							acc.setState(State.PASSED);
						}
					}
				}
			}
		}
	}
	
}
