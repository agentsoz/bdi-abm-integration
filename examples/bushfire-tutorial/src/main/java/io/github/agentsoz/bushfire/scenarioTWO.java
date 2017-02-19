package io.github.agentsoz.bushfire;

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
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.agentsoz.bushfire.shared.ActionID;
import io.github.agentsoz.abmjack.shared.ActionManager;
import io.github.agentsoz.abmjack.shared.GlobalTime;
import io.github.agentsoz.bdiabm.ABMServerInterface;
import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.bdiabm.data.ActionContent.State;
import io.github.agentsoz.bdimatsim.moduleInterface.data.SimpleMessage;
import io.github.agentsoz.dataInterface.DataServer;
import aos.jack.jak.agent.Agent;
import scenarioTWO.agents.EvacResident;
import io.github.agentsoz.bushfire.datacollection.ScenarioTwoData;

public class scenarioTWO extends BDIModel{

		
		public  HashMap<Integer,EvacResident> residents = new  HashMap<Integer,EvacResident>();
		public  HashMap<Integer,Double> depTimes = new HashMap<Integer,Double>();
		
		final Logger logger = LoggerFactory.getLogger("");
		

		public static boolean initialized =false;

//		static boolean fireResponse = false;  // agents's response to fire : to make the agents post EvacAlert goal only once 
		
		@Override
		public void takeControl(AgentDataContainer adc) {

			super.takeControl(adc);
			checkDepartureFromHome(adc);
		}

		public void checkDepartureFromHome(AgentDataContainer adc) { 
			 Iterator it = this.agents.entrySet().iterator();
			 while(it.hasNext()) { 
				 Map.Entry agentEntry = (Map.Entry)it.next();
				 String agentID = (String) agentEntry.getKey();
				 EvacResident agent = (EvacResident) agentEntry.getValue();
				 if(agent.waitAtHomeFlag == true && agent.getTimeLeftToEvac() <= Config.getDepartureTriggerTime()) { 
					 adc.getOrCreate(agentID).getPerceptContainer().put(DataTypes.LEAVENOW, " start departure");
				 }
			 }
			
		}
		
		@Override
		public Agent createAgent(String agentID, Object[] initData) {
			//return new BasicResident(agentID, this, bdiConnector, getKids(""),getRels(""));
			logger.trace("agent " +agentID +" initiated from scenarioTWO");
			logger.trace("config bypass status : " + Config.getBypassController());
			return new EvacResident(agentID, this.bdiConnector, this); //Test package agent
		}
		
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public void handlePercept(Agent agent, String perceptID, Object parameters) {

		//	if (Config.getBypassController()) {
		// no neeed of bypasscontroller check- agents always handle their percepts themselves
			
				EvacResident resident = (EvacResident) agent;
				if (perceptID.equals(DataTypes.FIREALERT) && resident.fireResponse == false)
				 {
					
//					dataServer.publish(DataTypes.FIRE_ALERT, null);
					logger.trace("agent posting EvacAlert goal...");
					resident.postEvacAlert("fire started");
					resident.fireResponse= true;

				
				}
				if(perceptID.equals(DataTypes.LEAVENOW)) { 
					logger.debug("recieved a LEAVENOW percept at time {}", GlobalTime.time.getTime());
					resident.postLeaveGoal();
					resident.waitAtHomeFlag = false;
				}
				super.handlePercept(agent, perceptID, parameters); 

				
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
			String passed = state.PASSED.toString();
			
			//selecting action types which have a destination allocated
			if(actionID.equals(ActionID.DRIVETO) || actionID.equals(ActionID.CONNECT_TO) || actionID.equals(ActionID.driveToAndPickUp) ) { 
				
				//if the action state is passed and the action type equals the last initiated action type 
				if( returnedState.compareTo(passed) == 0 && actionID.equals(((EvacResident) agent).initiatedAction) ) {  //equals
					
					((EvacResident) agent).updateCurrentLocation(); //update current location of the agent
					((EvacResident) agent).removeTargetDestination(); // reset the target attributes of the agent
				}
				else {
					((EvacResident) agent).removeTargetDestination(); //remove target dest upon failure
				}
				
				
			}
			
			// first, updating current location of the agent, because, if this thread reach the next action earlier than the above
			//code block, then agent attributes might be changed according to the new BDI action..
			((EvacResident) agent).updateActionState(actionID, state, parameters);
			
			
			}catch(NumberFormatException e) {
				logger.debug("NumberFormatException : {}", e.getMessage());
			}
			

		}

		@Override
		public void setup(ABMServerInterface abmServer) {

			super.setup(abmServer);
			//printing the configuration parameters
			this.bdiConnector.print_S2JACKModelConfigs();
		}
		
		// listens for fire alerts, evac broadcasts and matsim agent updates
		@Override
		public boolean dataUpdate(double time, String dataType, Object data) {

			switch (dataType) {

			case DataTypes.FIRE_ALERT: {

				logger.debug("received fire alert");
				return true;
			}
			// establish starting location and home region for each agent
			case DataTypes.MATSIM_AGENT_UPDATES: {
				logger.trace("received matsim agent updates");
				for (SimpleMessage msg : (SimpleMessage[]) data) {

					String id = (String) msg.params[0];
					EvacResident agent = (EvacResident) agents.get(id);

					//received from Utils.sendInitialPlanData()
					if(msg.name.equals("initPlanData")) {
						logger.debug("initial matsim plan data arrived");
						
						//1. assiging home link and depttime
//						agent.depTime = (double)msg.params[1];  setDepTime
						agent.setDepTime((double)msg.params[1]);
					//	logger.debug("retreived deptime {} for agent {}",agent.depTime, id);
						
						//2.assigning coords of safe destination
						double safeX = (double) msg.params[2];
						double safeY = (double) msg.params[3];
					agent.endLocation = new double[] { safeX, safeY };
					logger.trace("agent {} end location is {},{}", id, agent.endLocation[0],agent.endLocation[1]);
					
					}
						
					//received from Utils.initialiseVisualisedAgents()
					if (agent != null && agent.getStartLocation() == null) {

						double lat = (double) msg.params[1];
						double lon = (double) msg.params[2];
						logger.trace("agent {} start location is {},{} at time {}", id, lon,
								lat,getSimTime());

						agent.startLocation = new double[] { lat, lon };
						agent.currentLocation = "home"; // since each agent's start location is home
						
					//if  the totPickps havent exceeded the maxPickups limit	
					if( ScenarioTwoData.totPickups <= Config.getMaxPickUps() ) {
						
						//3. allocating kids and schools						
						if(getKids() && !agent.relsNeedPickUp) {
							ScenarioTwoData.agentsWithKids++;
							double[] sclCords = Config.getRandomSchoolCoords(id,agent.startLocation);
							if(sclCords != null) { 
								agent.kidsNeedPickUp = true;  
								agent.schoolLocation = sclCords;
								agent.prepared_to_evac_flag = false;
								ScenarioTwoData.totPickups++;
								logger.debug("agent {} has kids |"
										+ " school location: {} {} |", id, sclCords[0], sclCords[1]);
							}
							else{
								logger.debug("no school found for agent {}  assigned with kids ", id);
								ScenarioTwoData.agentsWithKidsNoSchools++;
							}
							
						}

						
						//4. assign relatives
						if(getRels() && !agent.kidsNeedPickUp) {
							ScenarioTwoData.agentsWithRels++;
							agent.relsNeedPickUp = true;
							agent.prepared_to_evac_flag = false;
							ScenarioTwoData.totPickups++;
							logger.debug(" agent {} has rels", id);
						}
						
					}
						if(agent.kidsNeedPickUp && agent.relsNeedPickUp) { 
							logger.warn("agent with TWO-PICKUPs found: {}", id);
						}
						
					}
					}
					return true;
				}
			
			}

			return false;
		}
		
		
//		public double[] checkForKidsAndAssignScl(String name,double[] agentLoc) {
//			double[] sclCords=null;
//			
//			
//			Boolean haveKids = new Random().nextDouble() < Config.getProportionWithKids();
//			logger.trace("agent {} havekids, checknig for schools... ", name);
//			if(haveKids) {
//				ScenarioTwoData.agentsWithKids++;
//				sclCords = Config.getRandomSchoolCoords(name, agentLoc);
//				if(sclCords != null) {
//					ScenarioTwoData.agentsWithSchools++;
//				}
//				else {
//					logger.debug("no school found for agent {}  ", name);
//					ScenarioTwoData.agentsWithKidsNoSchools++;
//				}
//			}
//			
//			return sclCords;
//		}

		@Override
		public boolean getRels() {
			return new Random().nextDouble() < Config.getProportionWithRelatives();
		}
		
		@Override
		public boolean getKids() {
			return new Random().nextDouble() < Config.getProportionWithKids();
		}
		
		@Override
		public void finish() {
			logger.debug("agents with kids: {}"
					+ "| agents with Relatives: {}"
					+ "| agents with schools: {}"
					+ "| agents with kids but no schools: {}"
					+ "| totPickups: {} | maxPickups: {}"
					+ "", ScenarioTwoData.agentsWithKids,ScenarioTwoData.agentsWithRels,ScenarioTwoData.agentsWithSchools,ScenarioTwoData.agentsWithKidsNoSchools,ScenarioTwoData.totPickups, Config.getMaxPickUps());
			
			//store data
			ScenarioTwoData.writeToFile();
			ScenarioTwoData.writeConnectToDepTimesToFile();
			
			logger.info("shut down");
//			EvacuationReport.close();
		}
}
