package io.github.agentsoz.abmjill.testagent;

import io.github.agentsoz.bdiabm.EnvironmentActionInterface;
import io.github.agentsoz.bdiabm.v3.QueryPerceptInterface;
import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.ActionContent.State;

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

import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.AgentInfo;
import io.github.agentsoz.jill.util.Log;

import java.io.PrintStream;

@AgentInfo(hasGoals={"io.github.agentsoz.abmjill.testagent.GoalA", "io.github.agentsoz.abmjill.genact.EnvironmentAction"})
public class TestAgent extends Agent implements io.github.agentsoz.bdiabm.Agent {

	private PrintStream writer = null;
	private EnvironmentActionInterface environmentActionInterface = null;
	public TestAgent(String str) {
		super(str);
	}

	@Override
	public void start(PrintStream writer, String[] params) {
		this.writer = writer;
		post(new GoalA("gA"));
	}
	
	@Override
	public void finish() {
	}

	@Override
	public void init(String[] args) {
		Log.warn("TestAgent using a stub for io.github.agentsoz.bdiabm.Agent.init(...)");
	}

	@Override
	public void start() {
		Log.warn("TestAgent using a stub for io.github.agentsoz.bdiabm.Agent.start()");
	}

	@Override
	public void handlePercept(String perceptID, Object parameters) {
		Log.warn("TestAgent using a stub for io.github.agentsoz.bdiabm.Agent.handlePercept(...)");
	}

	@Override
	public void updateAction(String actionID, ActionContent content) {
		Log.info("Agent"+getId()+" received action update: "+content);
		if (content.getAction_type().equals("TESTACTION")) {
			writer.print(getId()+":"+content.getAction_type()+":"+content.getState()+",");
			if (content.getState()==State.PASSED) {
				// Wake up the agent that was waiting for external action to finish
				suspend(false);
			}
		}
	}

	@Override
	public void kill() {
		// Intentionally left empty
	}

	@Override
	public void setQueryPerceptInterface(QueryPerceptInterface queryInterface) {
		// Intentionally left empty
	}

	@Override
	public QueryPerceptInterface getQueryPerceptInterface() {
		return null;
	}

	@Override
	public void setEnvironmentActionInterface(EnvironmentActionInterface envActInterface) {
		environmentActionInterface = envActInterface;
	}

	@Override
	public EnvironmentActionInterface getEnvironmentActionInterface() {
		return environmentActionInterface;
	}

}
