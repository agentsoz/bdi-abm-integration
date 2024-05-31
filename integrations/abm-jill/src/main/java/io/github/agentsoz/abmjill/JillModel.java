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

import io.github.agentsoz.bdiabm.Agent;
import io.github.agentsoz.bdiabm.BDIServerInterface;
import io.github.agentsoz.bdiabm.EnvironmentActionInterface;
import io.github.agentsoz.bdiabm.ModelInterface;
import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.PerceptContent;
import io.github.agentsoz.bdiabm.v2.AgentDataContainer;
import io.github.agentsoz.bdiabm.v3.QueryPerceptInterface;
import io.github.agentsoz.jill.Main;
import io.github.agentsoz.jill.config.Config;
import io.github.agentsoz.jill.core.GlobalState;
import io.github.agentsoz.jill.struct.AObject;
import io.github.agentsoz.jill.util.ArgumentsLoader;
import io.github.agentsoz.util.PerceptList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class JillModel implements BDIServerInterface, ModelInterface, EnvironmentActionInterface {

	private static final Logger logger = LoggerFactory.getLogger(JillModel.class);

	private Config config;
	private QueryPerceptInterface queryInterface;

	private AgentDataContainer outAdc;

	public JillModel() {
		outAdc = new AgentDataContainer();
	}

	protected static Agent getAgent(int id) {
		AObject agent = GlobalState.agents.get(id);
		if (agent != null && agent instanceof Agent) {
			return (Agent)agent;
		}
		throw new RuntimeException("Agent " + id + " is not of type io.github.agentsoz.bdiabm.Agent; found " + agent);
	}

	@Override
	public void init(Object[] params) {
		// Parse the command line options
		ArgumentsLoader.parse((String[])params);
		// Load the configuration 
		config = ArgumentsLoader.getConfig();
		// Now initialise Jill with the loaded configuration
		try {
			Main.init(config);
			for(int i = 0; i < GlobalState.agents.size(); i++) {
				AObject agent = GlobalState.agents.get(i);
				if (agent != null && agent instanceof io.github.agentsoz.bdiabm.Agent) {
					((io.github.agentsoz.bdiabm.Agent)agent).setEnvironmentActionInterface(this);
				}
			}

		} catch(Exception e) {
			throw new RuntimeException("ERROR while initialising JillModel", e);
		}
	}

	@Override
	public void setQueryPerceptInterface(QueryPerceptInterface queryInterface) {
		this.queryInterface = queryInterface;
	}

	@Override
	public QueryPerceptInterface getQueryPerceptInterface() {
		return queryInterface;
	}

	@Override
	public void start() {
		Main.start(config);
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
	public void finish() {
		Main.finish();
	}

	@Override
    public void packageAction(String agentId,
							  String actionId,
							  Object[] parameters,
							  String actionState) {
		ActionContent.State state = ActionContent.State.INITIATED;
		if (actionState != null) {
			try {
				state = ActionContent.State.valueOf(actionState);
			} catch (Exception e) {
				logger.warn("agent {} ignoring unknown action state {}", agentId, actionState);
			}
		}
		ActionContent ac = new ActionContent(parameters, state, actionId );
		outAdc.putAction(agentId, actionId, ac);
	}

	@Override
	public AgentDataContainer takeControl(double time, AgentDataContainer inAdc) {
		// Sending TIME percept always to every agent is too costly, dhi 20/jun/19
		//for(int i = 0; i < GlobalState.agents.size(); i++) {
		//	getAgent(i).handlePercept(PerceptList.TIME, time);
		//}
		if (inAdc != null) {
			Iterator<String> it = inAdc.getAgentIdIterator();
			while (it.hasNext()) {
				String agentId = it.next();
				// Process the incoming percepts
				Map<String, PerceptContent> percepts = inAdc.getAllPerceptsCopy(agentId);
				for (String perceptId : percepts.keySet()) {
					PerceptContent content = percepts.get(perceptId);
					if (content != null) {
						try {
							int id = Integer.parseInt(agentId);
							getAgent(id).handlePercept(PerceptList.TIME, time);
							getAgent(id).handlePercept(content.getPercept_type(), content.getValue());
						} catch (Exception e) {
							logger.error("While handling percept for Agent {}: {}", agentId, e.getMessage());
							e.printStackTrace();
						}
					}
				}

				// Process the incoming action updates
				Map<String, ActionContent> actions = inAdc.getAllActionsCopy(agentId);
				for (String actionId : actions.keySet()) {
					ActionContent content = actions.get(actionId);
					try {
						int id = Integer.parseInt(agentId);
						getAgent(id).updateAction(content.getInstance_id(), content);
					} catch (Exception e) {
						logger.error("While updating action status for Agent {}: {}", agentId, e.getMessage());
					}
				}
			}
		}
		// Wait until idle
		Main.waitUntilIdle();
		return outAdc;
	}

	@Override
	public void setAgentDataContainer(AgentDataContainer adc) {
		outAdc = adc;
	}

	@Override
	public AgentDataContainer getAgentDataContainer() {
		return outAdc;
	}

}
