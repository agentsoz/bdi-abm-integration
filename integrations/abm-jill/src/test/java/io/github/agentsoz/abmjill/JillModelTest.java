package io.github.agentsoz.abmjill;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2022 by its authors. See AUTHORS file.
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
import java.util.*;

import io.github.agentsoz.bdiabm.Agent;
import io.github.agentsoz.bdiabm.ModelInterface;
import io.github.agentsoz.bdiabm.QueryPerceptInterface;
import io.github.agentsoz.jill.core.GlobalState;
import io.github.agentsoz.jill.struct.AObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import io.github.agentsoz.bdiabm.ABMServerInterface;
import io.github.agentsoz.bdiabm.data.ActionContainer;
import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.v2.AgentDataContainer;
import io.github.agentsoz.bdiabm.data.AgentState;
import io.github.agentsoz.bdiabm.data.AgentStateList;
import io.github.agentsoz.bdiabm.data.ActionContent.State;
import io.github.agentsoz.jill.util.Log;

public class JillModelTest {

    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;

	private TestModel jillmodel;
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
		AgentDataContainer adc1 = null, adc2 = null;

		for (int i = 0; i < durationPerAction+10; i++) {

			adc2 = jillmodel.takeControl(i, adc1);
			adc1 = abmstub.takeControl(i, adc2);
			adc2.clear();
		}
		
		// Check the output
		String[] list = out.toString().split(",");
		Arrays.sort(list);
		assertEquals(Arrays.toString(output), Arrays.toString(list));
	}

	private void init(int nAgents, int durationPerAction) {

		// Initialise the jillmodel
		String[] args = {
				"--config",
				"{agents:[{classname:io.github.agentsoz.abmjill.testagent.TestAgent, args:null, count:"+nAgents+"}]," +
				"logLevel: ERROR," +
				"logFile: " + JillModelTest.class.getSimpleName() + ".log," +
				"numThreads: 1" +
				"}"
				};
		// construct the models
		abmstub = new StubABM(durationPerAction);
		jillmodel = new TestModel();
		// set the data containers to use
		abmstub.setAgentDataContainer(new AgentDataContainer());
		jillmodel.setAgentDataContainer(new AgentDataContainer());
		// initialise the models
		jillmodel.init(args);
		abmstub.init(null);
		// start
		jillmodel.start();
		abmstub.start();
	}
	
	private class TestModel extends JillModel {
		
	}
	
	private class StubABM implements ABMServerInterface, ModelInterface {

		private AgentDataContainer outAdc;

		// All actions will take this many steps to complete
		private int durationPerAction;
		
		// A map of agent:action -> count to keep track of how long
		// an action has been running
		private Map<String, Integer> agentsDurativeCount;

		Map<String, ActionContent> runningActions;
		
		public StubABM(int durationPerAction) {
			outAdc = new AgentDataContainer();
			runningActions = new HashMap<>();
			this.durationPerAction = durationPerAction;
			agentsDurativeCount = new LinkedHashMap<String, Integer>();
		}

		@Override
		public void init(Object[] params) {
		}

		@Override
		public void start() {

		}

		@Override
		public Object[] step(double time, Object[] args) {
			if (args != null && args[0] != null && args[0] instanceof AgentDataContainer) {
				AgentDataContainer inAdc = (AgentDataContainer) args[0];
				return new Object[]{takeControl(time, inAdc)};
			}
			return null;
		}


		@Override
		public AgentDataContainer takeControl(double time, AgentDataContainer adc) {
			outAdc.clear();
			Log.trace("Stub ABM Received " + adc);
			Iterator<String> it = adc.getAgentIdIterator();
			while (it.hasNext()) {
				String agentId = it.next();
				// Process the incoming action updates
				Map<String, ActionContent> actions = adc.getAllActionsCopy(agentId);
				for (String actionId : actions.keySet()) {
					ActionContent acc = actions.get(actionId);
					// The unique id for this agent:action
					String mapid = agentId + ":" + acc.getAction_type();
					// If just initiated then add to the map
					if (acc.getState() == State.INITIATED) {
						acc.setState(State.RUNNING);
						agentsDurativeCount.put(mapid, durationPerAction);
						runningActions.put(mapid, acc);
					}
				}
			}
			for (String mapid : runningActions.keySet()) {
				String[] tokens = mapid.split(":");
				String agentId = tokens[0];
				String actionId = tokens[1];
				ActionContent acc = runningActions.get(mapid);
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
					outAdc.putAction(agentId, actionId, acc);
				}
			}

			return outAdc;
		}

		@Override
		public void finish() {

		}

		@Override
		public void setAgentDataContainer(AgentDataContainer adc) {
			outAdc = adc;

		}

		@Override
		public AgentDataContainer getAgentDataContainer() {
			return outAdc;
		}

		Agent getAgent(int id) {
			AObject agent = GlobalState.agents.get(id);
			if (agent != null && agent instanceof Agent) {
				return (Agent)agent;
			}
			throw new RuntimeException("Agent " + id + " is not of type io.github.agentsoz.bdiabm.Agent; found " + agent);
		}
	}
	
}
