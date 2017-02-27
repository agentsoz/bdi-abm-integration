package io.github.agentsoz.bushfiretute;

import java.util.ArrayList;
import java.util.Iterator;

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

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.agentsoz.bdimatsim.MATSimActionHandler;
import io.github.agentsoz.bdimatsim.MATSimAgent;
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.bdimatsim.MatsimPerceptHandler;
import io.github.agentsoz.bdimatsim.MatsimPerceptList;
import io.github.agentsoz.bdimatsim.app.BDIActionHandler;
import io.github.agentsoz.bdimatsim.app.MATSimApplicationInterface;
import io.github.agentsoz.bdimatsim.app.BDIPerceptHandler;
import io.github.agentsoz.bushfiretute.shared.ActionID;
import io.github.agentsoz.bushfiretute.shared.PerceptID;

public class ABMModel implements MATSimApplicationInterface {

    final Logger logger = LoggerFactory.getLogger("");      
    final MATSimModel model;
    
	public ABMModel(MATSimModel model) {
		this.model = model;
		model.registerPlugin(this);
	}

	@Override
	public void notifyBeforeCreatingBDICounterparts(List<Id<Person>> bdiAgentsIDs) {
	}

	@Override
	public void notifyAfterCreatingBDICounterparts(List<Id<Person>> bdiAgentsIDs) {
		
		// DSingh, 21/02/17: The code in hawkesbury branch Utils.sendInitialPlanData()
		// can go here. It sends MATSIM_AGENT_UPDATES via the data server 
		// which are then eventually picked up by ScenarioTWO.java.
		// However, now that we have this hook back into the app, this info
		// can be retrieved directly, and probably there is no longer a need
		// to use the DataServer etc.

		//Map<Id<Link>,? extends Link> links = model.getScenario().getNetwork().getLinks();
		//for(Id<Person> agentId: model.getBDIAgentIDs()) {
			//MATSimAgent agent = model.getBDIAgent(agentId);
            //m.params = new Object[]{//agent.getId().toString(),links.get(agent.getCurrentLinkId()).getFromNode().getId().toString(),"T"};
            //        agent.getId().toString(),
            //        links.get(agent.getCurrentLinkId()).getFromNode().getCoord().getX(),
            //        links.get(agent.getCurrentLinkId()).getFromNode().getCoord().getY()
			//};
	}

	@Override
	public void registerNewBDIActions(MATSimActionHandler withHandler) {
		// overwrite default DRIVETO
		withHandler.registerBDIAction(ActionID.DRIVETO, new BDIActionHandler() {
			@Override
			public boolean handle(String agentID, String actionID, Object[] args, MATSimModel model) {
				logger.debug("found DRIVETO action for agent : " + agentID);
				// Get nearest link ID and calls the Replanner to map to MATSim.
				Id<Link> newLinkId;
				if (args[1] instanceof double[]) {
					double[] coords = (double[]) args[1];
					logger.debug("received coords for driveTo : " + coords[0] + " " + coords[1]);
					newLinkId = ((NetworkImpl) model.getScenario().getNetwork())
							.getNearestLinkExactly(new CoordImpl(coords[0], coords[1])).getId();
					logger.debug("generated link Id for driveTo : " + newLinkId.toString());
				} else {
					throw new RuntimeException("Destination coordinates are not given");
				}

				String destination = (String) args[2];
				// this.matSimModel.getReplanner().attachNewActivityAtEndOfPlan(
				// newLinkId, Id.createPersonId( agentID ));

				// this.matSimModel.getReplanner().addNewLegToPlan(
				// Id.createPersonId( agentID ),newLinkId, destination);
				
				//FIXME dhi: model.getReplanner().addNewLegToPlan(Id.createPersonId(agentID), newLinkId, destination);

				// Saving actionValue (which for DRIVETO actions is the link id)
				// We use this again in the AgentActivityEventHandler, to check
				// if the agent has arrived at this link, at which point we can
				// mark this BDI-action as PASSED
				MATSimAgent agent = model.getBDIAgent(agentID);
				agent.newDriveTo(newLinkId);
				return true;
			}
		});
		
		// register new action
		withHandler.registerBDIAction(ActionID.CONNECT_TO, new BDIActionHandler() {
			@Override
			public boolean handle(String agentID, String actionID, Object[] args, MATSimModel model) {
				logger.debug("found CONNECT_TO action for agent : " + agentID);
				String destination = (String) args[1];
				// connect To route replanner method
				Id<Link> newLinkId = null;
				//FIXME dhi: newLinkId = model.getReplanner().replanCurrentRoute(Id.createPersonId(agentID), destination);
				if (newLinkId == null) {
					logger.debug("CONNECT_TO: returned a null link from the target activity");
				}
				MATSimAgent agent = model.getBDIAgent(agentID);
				//FIXME dhi: agent.newConnectTo(newLinkId);
				return true;
			}
		});

		// register new action
		withHandler.registerBDIAction(ActionID.DRIVETO_AND_PICKUP, new BDIActionHandler() {
			@Override
			public boolean handle(String agentID, String actionID, Object[] args, MATSimModel model) {
				// Get nearest link ID and calls the Replanner to map to MATSim.
				Id<Link> newLinkId;
				if (args[1] instanceof double[]) {
					double[] coords = (double[]) args[1];
					logger.trace("received coords for driveToAndPickUp: " + coords[0] + " " + coords[1]);

					newLinkId = ((NetworkImpl) model.getScenario().getNetwork())
							.getNearestLinkExactly(new CoordImpl(coords[0], coords[1])).getId();
					logger.debug("driveToAndPickUp: agentID {} | received coords - {} {} | generated link Id {} ",
							agentID, coords[0], coords[1], newLinkId.toString());
				} else {
					throw new RuntimeException("Destination coordinates are not given");
				}

				int pickupTime = (int) args[3];
				//FIXME dhi: model.getReplanner().addNewLegAndActvityToPlan(Id.createPersonId(agentID), newLinkId, pickupTime);
				logger.trace(" finished calling addLegAndActvityToNextIndex method in Replanner");

				MATSimAgent agent = model.getBDIAgent(agentID);
				// FIXME dhi: agent.newDriveToAndPickUp(newLinkId);
				return true;
			}
		});

		// register new action
		withHandler.registerBDIAction(ActionID.PICKUP, new BDIActionHandler() {
			@Override
			public boolean handle(String agentID, String actionID, Object[] args, MATSimModel model) {
				int pickupTime = (int) args[1];

				Id<Link> newLinkId; // just to distinguish PICKUP actions when
									// returning the action state
				if (args[2] instanceof double[]) {
					double[] coords = (double[]) args[2];
					logger.trace("received coords for driveToAndPickUp: " + coords[0] + " " + coords[1]);
					newLinkId = ((NetworkImpl) model.getScenario().getNetwork())
							.getNearestLinkExactly(new CoordImpl(coords[0], coords[1])).getId();
				} else {
					throw new RuntimeException("Destination coordinates are not given");
				}

				logger.debug("PICKUP: agentID {} | newLinkId: {} | pickupTime: {} ", agentID, newLinkId, pickupTime);
				// FIXME dhi: model.getReplanner().addNewActivityToPlan(Id.createPersonId(agentID), pickupTime);
				logger.trace(" finished calling addNewActivityToPlan method in Replanner");

				MATSimAgent agent = model.getBDIAgent(agentID);
				//FIXME dhi: agent.newPickUp(newLinkId);

				return true;
			}
		});

		// register new action
		withHandler.registerBDIAction(ActionID.SET_DRIVE_TIME, new BDIActionHandler() {
			@Override
			public boolean handle(String agentID, String actionID, Object[] args, MATSimModel model) {
                double newEndTime = (double) args[1];
                String actType = (String) args[2];        
                logger.debug("setDriveTime: agentID {} | actType  {} | delayTime {} ",agentID,actType,newEndTime);
                //FIXME dhi: model.getReplanner().forceEndActivity(Id.createPersonId( agentID ),actType, newEndTime);
                return true;
			}
		});


	}

	@Override
	public void registerNewBDIPercepts(MatsimPerceptHandler withHandler) {
		// regsiter new percept
		withHandler.registerBDIPercepts(PerceptID.ARRIVED_CONNECT_TO, new BDIPerceptHandler() {
			@Override
			public Object[] process(String agentID, String perceptID, MATSimModel model) {
                logger.debug("inside processPercept method,  handling percepts of percept type CONNECT_TO of agent : " + agentID);
                MATSimAgent agent = model.getBDIAgent( Id.createPersonId(agentID));
                ArrayList<Id<Link>> passedActions = new ArrayList<Id<Link>>();
                //FIXME dhi: passedActions = agent.getpassedConnectToActions();
                if (passedActions.isEmpty()){
                        return null;
                }
                logger.debug("number of passed final drive to actions : " + passedActions.size() );
                String[] array=new String[passedActions.size()];
                Iterator<Id<Link>> it = passedActions.iterator();
                int i=0;
                while (it.hasNext()){
                        Id<Link> action = it.next();
                        array[i++] = action.toString();
                }
                //FIXME dhi: agent.clearAllConnectToActions();
                return array;
			}
		});
		
		// regsiter new percept
		withHandler.registerBDIPercepts(PerceptID.ARRIVED_AND_PICKED_UP, new BDIPerceptHandler() {
			@Override
			public Object[] process(String agentID, String perceptID, MATSimModel model) {
                /*
                 * Returns all passed pick up actions held in the MatsimAgent object as an array
                 * Designed to handle multiple arrivals in one percept
                 */
                logger.debug("inside processPercept method,  handling percepts of percept type ARRIVED_AND_PICKED_UP for agent : " + agentID);
                MATSimAgent agent = model.getBDIAgent(agentID);
                ArrayList<Id<Link>> passedActions = new ArrayList<Id<Link>>();
                //FIXME dhi: passedActions = agent.getpassedDriveToAndPickUpActions();
                if (passedActions.isEmpty()){
                        return null;
                }
                logger.debug("number of passed driveToAndPickup  actions : " + passedActions.size() );
                String[] array=new String[passedActions.size()];
                Iterator<Id<Link>> it = passedActions.iterator();
                int i=0;
                while (it.hasNext()){
                        Id<Link> action = it.next();
                        array[i++] = action.toString();
                }
                //FIXME dhi: agent.clearPassedDriveToAndPickupActions();;
                return array;
			}
		});
	}

	public void run(String file, String[] args) {
		model.run(file, args);
	}
}