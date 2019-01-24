package io.github.agentsoz.bushfiretute.bdi;

import java.util.Map.Entry;

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

import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.util.evac.ActionList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aos.jack.jak.agent.Agent;
import io.github.agentsoz.abmjack.JACKModel;
import io.github.agentsoz.bdiabm.ABMServerInterface;
import io.github.agentsoz.bdiabm.data.ActionContent.State;
import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.util.evac.PerceptList;
import io.github.agentsoz.bushfiretute.BushfireMain;
import io.github.agentsoz.bushfiretute.Config;
import io.github.agentsoz.bushfiretute.jack.agents.EvacResident;

/* The BDI Model class handles passing percepts and actions to and
 * from the resident agents, passing simulation setup data (locations, routes
 * etc) and agent plan choices to the visualiser, broadcasting alerts to the
 * agents, and managing the EvacController and relief schedule data.
 * 
 * two types of data updates: 
 * passive (timed) data updates - for these data updates the data source has to register to the data server
 * active data updates - do not have to register
 * 
 *  DataClient - receives receiveData from the subscribed data type.
 */

public class BDIModel extends JACKModel {  //DataSource

	final Logger logger = LoggerFactory.getLogger("");
	protected BdiConnector bdiConnector;

//	static boolean startedEvac = false;
	int timeStepcount = 0; // ugly way to post the fire alert at the 4th time step

	boolean firePerceptAdded = false;  // to send the global fire alert percept
	private DataServer dataServer;

	public BDIModel(DataServer dataServer) {
		this.dataServer = dataServer;

	}

	@Override
	public void takeControl(AgentDataContainer adc) {

		double timeInSecs = dataServer.getTime();
		// on receiving the fire alert, insert a global percept into the agent data containers
		if (dataServer.getTime() == 4.0 && !firePerceptAdded ) {
			adc.getOrCreate("global").getPerceptContainer()
					.put(PerceptList.FIRE_ALERT, "bushfire started");
			logger.debug("broadcasted fire alert global percept at timestep : {}", timeInSecs);
			firePerceptAdded = true;

		}
		adc.getOrCreate(PerceptList.BROADCAST).getPerceptContainer().put(PerceptList.TIME, timeInSecs);
		super.takeControl(adc);
		
		checkDepartureFromHome(adc);
	}


	private void checkDepartureFromHome(AgentDataContainer adc) { 
		for ( Entry<String,Agent> agentEntry : this.agents.entrySet() ) {
			 String agentID = agentEntry.getKey();
			 EvacResident agent = (EvacResident) agentEntry.getValue();
			 if(agent.waitAtHomeFlag == true && agent.getTimeLeftToEvac() <= Config.getDepartureTriggerTime()) { 
				 adc.getOrCreate(agentID).getPerceptContainer().put(PerceptList.LEAVENOW, " start departure");
			 }
			 //waitUntilIdle(); // yyyyyy try to get code deterministic
		 }
		
	}
	

	@Override
	public void setup(ABMServerInterface abmServer) {
		bdiConnector = new BdiConnector();
		this.bdiConnector.print_S2JACKModelConfigs();
	}

	// Implemented here so we can cleanly close our log files.
	@Override
	public void finish() {
		logger.info("shut down");
	}
	

	@Override
	public void packageAction(String agentID, String actionID,
			Object[] parameters) {

		// BDI agent has completed all of its goals - publish update
//		if ((String) parameters[0] == "done" && dataServer != null) {
//			SimpleMessage message = new SimpleMessage();
//			message.name = "done";
//			message.params = new Object[] { agentID, "done" };
//			dataServer.publish(DataTypes.BDI_AGENT_UPDATES, message);
//			logger.debug("agent {} made it to relief centre", agentID);
//
//			return;
//		}

		// if the parameter is a BDI DriveTo action
		if ((String) parameters[0] == ActionList.DRIVETO) {
				logger.trace("received a DRIVETO action in package action");
				startDriving(agentID, parameters);
		
		}
		
		//pick up action
		if ((String) parameters[0] == ActionList.DRIVETO_AND_PICKUP) {
			logger.trace("received a DRIVETO_AND_PICKUP action in package action");
			callSuperPackageAction(agentID, ActionList.DRIVETO_AND_PICKUP, parameters);
	
		}	
		
		//pick up action
		if ((String) parameters[0] == ActionList.PICKUP) {
			logger.trace("received a PICKUP action in package action");
			callSuperPackageAction(agentID, ActionList.PICKUP, parameters);
	
		}
		
		//pick up action
		if ((String) parameters[0] == ActionList.SET_DRIVE_TIME) {
			logger.trace("received a SET_DRIVE_TIME action in package action");
			callSuperPackageAction(agentID, ActionList.SET_DRIVE_TIME, parameters);
	
		}
		
		//finalt drive to action
		if ((String) parameters[0] == ActionList.CONNECT_TO) {
			logger.trace("received a CONNECT_TO action in package action");
			startDriving(agentID, parameters);
	
	}
		
	}
	
	private void startDriving(String agentID,
			Object[] parameters){

		String action = (String) parameters[0];
//		double[] destination = (double[]) parameters[1];
//		String destinationName = (String) parameters[2];

//		logger.debug("agent {} driving to {}, coordinates: {}", agentID,
//				destinationName, destination);
		if(action.equals(ActionList.DRIVETO)) {
			callSuperPackageAction(agentID, ActionList.DRIVETO, parameters);
		}
		else if(action.equals(ActionList.CONNECT_TO)){
			callSuperPackageAction(agentID, ActionList.CONNECT_TO, parameters);
		}

	}
//	
	private void callSuperPackageAction(String agentID, String action, Object[] parameters){
		super.packageAction(agentID, action, parameters);
	}

	@Override
	public Agent createAgent(String agentID, Object[] initData) {
		logger.debug("agent {} initiated from bushfire application", agentID);
		final EvacResident evacResident = new EvacResident(agentID, bdiConnector, this, BushfireMain.writer);
		//waitUntilIdle(); // yyyyyy try to get code deterministic
		return evacResident;
	}

	@Override
	public void handlePercept(Agent agent, String perceptID, Object params) {

		EvacResident resident = (EvacResident) agent;
		if (perceptID.equals(PerceptList.TIME)) {
			if (params instanceof Double) {
				resident.setCurrentTime((double)params);
			}
		} else if (perceptID.equals(PerceptList.FIRE_ALERT) && resident.fireResponse == false) {
			// dataServer.publish(DataTypes.FIRE_ALERT, null);
			resident.log("received alert " + params);
			resident.postEvacAlert("fire started");
			resident.fireResponse = true;
		}
		else if (perceptID.equals(PerceptList.LEAVENOW)) {
			resident.log("recieved percept to leave now at time " + dataServer.getTime());
			resident.postLeaveGoal();
			resident.waitAtHomeFlag = false;
		}
		else if (perceptID.equals(PerceptList.ARRIVED)) {
			Object[] args = (Object[])params;
			resident.log("reached " + args[0] + " at time " + args[1]);
		}
		else if (perceptID.equals(PerceptList.ARRIVED_CONNECT_TO)) {
			Object[] args = (Object[])params;
			resident.log("arrived at conection link " + args[0] + " at time " + args[1]);
		}
		else if (perceptID.equals(PerceptList.ARRIVED_AND_PICKED_UP)) {
			resident.log("arrived and picked up from link " + ((Object[])params)[0]);
		}
		else if (perceptID.equals(PerceptList.PICKED_UP)) {
			resident.log("picked up " + params);
		}
		
		//waitUntilIdle(); // yyyyyy try to get code deterministic

	}

	@Override
	public double getSimTime() {
		// TODO Auto-generated method stub
		return 0;
	}



	// update agent action state within the JACK program
	@Override
	public void updateAction(Agent agent, String actionID, State state,
			Object[] parameters) {
		try{
		logger.debug("scenarioTWO-update action: actionID {} state {}",actionID, state.toString());
					
		//int currentLink = Integer.parseInt((String)parameters[0] );
		//logger.debug(" parameters " + currentLink );
		
		//checking the target destination
		String returnedState = state.toString();
		String passed = State.PASSED.toString();
		
		//selecting action types which have a destination allocated
		if(actionID.equals(ActionList.DRIVETO) || actionID.equals(ActionList.CONNECT_TO) || actionID.equals(ActionList.DRIVETO_AND_PICKUP) ) {
			
			//if the action state is passed and the action type equals the last initiated action type 
			if( returnedState.compareTo(passed) == 0 && actionID.equals(((EvacResident) agent).initiatedAction) ) {  //equals
				
				((EvacResident) agent).updateCurrentLocation(); //update current location of the agent
				((EvacResident) agent).removeTargetDestination(); // reset the target attributes of the agent
			}
			else if (State.FAILED.toString().equals(returnedState)) {
				((EvacResident) agent).removeTargetDestination(); //remove target dest upon failure
			}
			
			
		}
		
		// first, updating current location of the agent, because, if this thread reach the next action earlier than the above
		//code block, then agent attributes might be changed according to the new BDI action..
		((EvacResident) agent).updateActionState(actionID, state, parameters);
		
		
		}catch(NumberFormatException e) {
			logger.debug("NumberFormatException : {}", e.getMessage());
		}
		
		//waitUntilIdle(); // yyyyyy try to get code deterministic

	}

	public EvacResident getBDICounterpart(String id) {
		if (agents.containsKey(id)) { 
			return (EvacResident) agents.get(id);
		} 
		return null;
	}

}
