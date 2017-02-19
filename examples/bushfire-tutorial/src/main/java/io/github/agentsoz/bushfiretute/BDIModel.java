package io.github.agentsoz.bushfiretute;

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

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.agentsoz.abmjack.JACKModel;
import io.github.agentsoz.abmjack.shared.ActionManager;
import io.github.agentsoz.abmjack.shared.GlobalTime;
import io.github.agentsoz.bdiabm.ABMServerInterface;
import io.github.agentsoz.bdiabm.data.ActionContent.State;
import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.bdimatsim.moduleInterface.data.SimpleMessage;
import io.github.agentsoz.bushfiretute.DataTypes;
import io.github.agentsoz.bushfiretute.bdi.BdiConnector;
import io.github.agentsoz.bushfiretute.shared.ActionID;
import io.github.agentsoz.dataInterface.DataClient;
import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.dataInterface.DataSource;
import aos.jack.jak.agent.Agent;
//import test.EvacResident;
import scenarioTWO.agents.EvacResident;

/* The BDI Model class handles passing percepts and actions to and
 * from the resident agents, passing simulation setup data (locations, routes
 * etc) and agent plan choices to the visualiser, broadcasting alerts to the
 * agents, and managing the EvacController and relief schedule data.
 * 
 * two types of data updates: 
 * passive (timed) data updates - for these data updates the data source has to register to the data server
 * active data updates - do not have to register
 * 
 *  DataClient - receives dataUpdate from the subscribed data type.
 */

public class BDIModel extends JACKModel implements DataClient, DataSource,
		 ActionManager {  //DataSource

	final Logger logger = LoggerFactory.getLogger("");
	protected BdiConnector bdiConnector;

//	static boolean startedEvac = false;
	int timeStepcount = 0; // ugly way to post the fire alert at the 4th time step

	static boolean firePerceptAdded = false;  // to send the global fire alert percept
	@Override
	public void takeControl(AgentDataContainer adc) {

		// on receiving the fire alert, insert a global percept into the agent data containers
		if (GlobalTime.time.getTime() == 4.0 && !firePerceptAdded ) {
			adc.getOrCreate("global").getPerceptContainer()
					.put(DataTypes.FIREALERT, "bushfire started");
			logger.debug("broadcasted fire alert global percept at timestep : {}",GlobalTime.time.getTime());
			firePerceptAdded = true;

		}
		
		GlobalTime.increaseNewTime();
		super.takeControl(adc);
	}



	@Override
	public void setup(ABMServerInterface abmServer) {

		dataServer = DataServer.getServer("Bushfire");
		dataServer.subscribe(this, DataTypes.FIRE_ALERT);
		dataServer.subscribe(this, DataTypes.MATSIM_AGENT_UPDATES);
		dataServer.registerSource("time", this); //DATA source

		bdiConnector = new BdiConnector(this); //passing this instance of the bushfire application
	}

	// Implemented here so we can cleanly close our log files.
	@Override
	public void finish() {
		logger.info("shut down");
//		EvacuationReport.close();
	}

	void promptControllerInput() {
		SimpleMessage msg = new SimpleMessage();
		msg.name = DataTypes.UI_PROMPT;
		String prompt = "Specify input for EvacController agent?";
		String[] options = new String[] { "Yes", "No" };
		msg.params = new Object[] { prompt, options };
		dataServer.publish(DataTypes.UI_PROMPT, msg);
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
		if ((String) parameters[0] == ActionID.DRIVETO) {
				logger.trace("received a DRIVETO action in package action");
				startDriving(agentID, parameters);
		
		}
		
		//pick up action
		if ((String) parameters[0] == ActionID.driveToAndPickUp) {
			logger.trace("received a driveToAndPickUp action in package action");
			callSuperPackageAction(agentID, ActionID.driveToAndPickUp, parameters);
	
		}	
		
		//pick up action
		if ((String) parameters[0] == ActionID.PICKUP) {
			logger.trace("received a PICKUP action in package action");
			callSuperPackageAction(agentID, ActionID.PICKUP, parameters);
	
		}
		
		//pick up action
		if ((String) parameters[0] == ActionID.setDriveTime) {
			logger.trace("received a setDriveTime action in package action");
			callSuperPackageAction(agentID, ActionID.setDriveTime, parameters);
	
		}
		
		//finalt drive to action
		if ((String) parameters[0] == ActionID.CONNECT_TO) {
			logger.trace("received a CONNECT_TO action in package action");
			startDriving(agentID, parameters);
	
	}
		
	}
	
	protected void startDriving(String agentID,
			Object[] parameters){

		String action = (String) parameters[0];
//		double[] destination = (double[]) parameters[1];
//		String destinationName = (String) parameters[2];

//		logger.debug("agent {} driving to {}, coordinates: {}", agentID,
//				destinationName, destination);
		if(action.equals(ActionID.DRIVETO)) { 
			callSuperPackageAction(agentID, ActionID.DRIVETO, parameters);
		}
		else if(action.equals(ActionID.CONNECT_TO)){ 
			callSuperPackageAction(agentID, ActionID.CONNECT_TO, parameters);
		}

	}
//	
	protected void callSuperPackageAction(String agentID, String action, Object[] parameters){
		super.packageAction(agentID, action, parameters);

		if (dataServer != null) {
			// publish new agent actions
			SimpleMessage message = new SimpleMessage();
			message.name = "updateAgentBDI";
			message.params = new Object[] { agentID, parameters[0],
					parameters[1], parameters[2] };
			
			dataServer.publish(DataTypes.BDI_AGENT_UPDATES, message);
		}
	}


	@Override
	public Agent createAgent(String agentID, Object[] initData) {

		logger.debug("agent {} initiated from bushfire application", agentID);
		return new EvacResident(agentID,bdiConnector, this); //Test package agent

	}

	@Override
	public void handlePercept(Agent agent, String perceptID, Object parameters) {
		
			if(perceptID.equals("Arrived"))
			{
				logger.debug(" received percept : Arrived for agent {} ", agent.getBasename() );
			}
			if(perceptID.equals("Arrived to final dest"))
			{
				logger.debug(" received percept : Arrived to final dest for agent {} ", agent.getBasename() );
			}
			if(perceptID.equals("arrived and picked up") )
			{
				logger.debug(" received percept : arrived and picked up for agent {} ", agent.getBasename());
			}
			if(perceptID.equals("picked up") )
			{
				logger.debug(" received percept : picked up for agent {} ", agent.getBasename());
			}
	}

	@Override
	public boolean dataUpdate(double time, String dataType, Object data) {
		return false;
	}
	
	@Override
	public void updateAction(Agent agent, String actionID, State state,
			Object[] parameters) {

	}

	public boolean getKids() {
		return new Random().nextDouble() < Config.getProportionWithKids();
	}

	public boolean getRels() {
		return new Random().nextDouble() < Config.getProportionWithRelatives();
	}

	@Override
	public Object getNewData(double time, Object parameters) {


		return null;
	}

	
	@Override
	public double getSimTime() {

		return dataServer.getTime();
	}
	
}