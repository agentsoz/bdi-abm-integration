package io.github.agentsoz.abmjadex.agent;

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


import io.github.agentsoz.abmjadex.data_structure.SyncArrayList;
import io.github.agentsoz.abmjadex.data_structure.SyncInteger;
import io.github.agentsoz.abmjadex.miscellaneous.ABMBDILoggerSetter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import io.github.agentsoz.bdiabm.data.PerceptContainer;
import jadex.bdi.runtime.IBeliefSet;
import jadex.bdi.runtime.IGoal;
import jadex.extension.envsupport.environment.IEnvironmentSpace;
import jadex.extension.envsupport.environment.ISpaceObject;

public class StepManagerPlan extends StepPlan
{
	
	/**
	 * This is the point where an agent mostly interact with Central Organizer(CO).
	 * When CO unidle an agent, the only plan which is unidled is this StepManagerPlan.
	 * StepManagerPlan would then updates all the action's state and percepts of the
	 * corressponding agent, and then release the halting point of the rest of plans
	 * of the agent.
	 */
	private static final long serialVersionUID = 6622160171911646456L;
	private final static Logger LOGGER = Logger.getLogger(StepManagerPlan.class.getName());
	
	//Cases of BDI-ABM switch over's agent's state
	public final static int IDLE = 1;
	public final static int RUN_ACTION_PERCEPT = 2;
	public final static int RUN_NOACTION_PERCEPT = 3;
	//in case of being awake due to agent's message
	public final static int RUN_NOACTION_NOPERCEPT = 4;
	public final static int RUN_ACTION_NOPERCEPT = 5;
	
	/**
	 * To Count and register the new goals created.
	 * @param oldGoals
	 * This are the old data to be compared with the current
	 * collection of goals.
	 * @return noOfNewGoals
	 */
	private int getNoOfNewlyGeneratedGoalSince(IGoal[] oldGoals)
	{
		int i = 0;
		IGoal[] goals = getGoalbase().getGoals();
		SyncArrayList dispatchedTopLevelGoal 
				= (SyncArrayList)getBeliefbase().getBelief("dispatchedTopLevelGoal").getFact();
		
		for (int j = 0; j < goals.length; j++)
		{
			boolean isExistBefore = false;
			for(int k = 0; k < oldGoals.length; k++)
			{
				if (oldGoals[k].equals(goals[j]))
				{
					isExistBefore = true;
				}
			}
			if (!isExistBefore)
			{	
				dispatchedTopLevelGoal.add(goals[j]);
				i++;
			}
		}
	
		getBeliefbase().getBelief("dispatchedTopLevelGoal").setFact(dispatchedTopLevelGoal);
		return i;
	}
	
	private void updateAgentBeliefs()
	{
		ISpaceObject myself = (ISpaceObject)getBeliefbase().getBelief("myself").getFact();
		PerceptContainer localPerceptContainer = (PerceptContainer)myself.getProperty("perceptContainer");
		//Update Agent Beliefs
		Set<String> perceptIDSet = localPerceptContainer.perceptIDSet();
		//System.out.println("~~~~~Inside belief update :" + perceptIDSet.size() +"-"+this);
		//for (String perceptID : perceptIDSet)
		Object[] perceptIDSetA = perceptIDSet.toArray();
		for(int m = 0; m < perceptIDSetA.length; m++)
		{
			String perceptID = (String)perceptIDSetA[m];
			startAtomic();
			IGoal[] oldGoal = this.getGoalbase().getGoals();
			try
			{
				if (getBeliefbase().containsBelief(perceptID))
				{
					Object oldFact = getBeliefbase().getBelief(perceptID).getFact();
					Object newFact = localPerceptContainer.read(perceptID);
//					if (oldFact == null)
//						System.out.println("NULLL OLD FACT CREATE EXCEPTION!");
					if (!newFact.equals(oldFact))
					{
						getBeliefbase().getBelief(perceptID).setFact(newFact);	
					}
					//System.out.println("------Inside belief update :" + perceptID +"-"+this);
				}
				else if (getBeliefbase().containsBeliefSet(perceptID))
				{
					IBeliefSet beliefSet = getBeliefbase().getBeliefSet(perceptID);
					Object newFact = localPerceptContainer.read(perceptID);
					
					if (newFact.getClass().equals(beliefSet.getClazz()))
					{
						//Replace the old fact with the new one
						if (beliefSet.containsFact(newFact) == true)
							beliefSet.removeFact(newFact);
						beliefSet.addFact(newFact);
					}
					else
					{
						Object[] newFacts = (Object[]) newFact;
						for (int i = 0; i < newFacts.length; i++)
						{
							if (beliefSet.containsFact(newFacts[i]) == true)
								beliefSet.removeFact(newFacts[i]);
							beliefSet.addFact(newFacts[i]);
						}
						String nh = "";
						for(int i = 0; i < newFacts.length; i++)
						{
							nh = nh + newFacts[i].toString();
						}
						System.out.println(agentID + nh);
					}
				}
				else
				{
					//System.out.println("------Inside belief update");
					LOGGER.severe("perceptID is not recognized by Jadex's belief/beliefset");
					//throw new RuntimeException ("perceptID is not recognized by Jadex's belief/beliefset");
				}
			} catch (Exception e)
			{
				LOGGER.severe(e.getMessage());
			}
			
			
			//No need to track new number of goals, because the goal itself is created and the plan
			//are in queue to be launched even before this idleplan could dispatch another idleplan
			int noOfNewlyGeneratedGoal = this.getNoOfNewlyGeneratedGoalSince(oldGoal);
			endAtomic();
			if (noOfNewlyGeneratedGoal > 0)
			{
				SyncInteger noOfTopLevelGoal 
						= (SyncInteger)getBeliefbase().getBelief("noOfTopLevelGoal").getFact();
//				noOfTopLevelGoal = noOfTopLevelGoal + noOfNewlyGeneratedGoal;
				noOfTopLevelGoal.add(noOfNewlyGeneratedGoal);
				getBeliefbase().getBelief("noOfTopLevelGoal").setFact(noOfTopLevelGoal);
			}
			//System.out.println("------Inside belief update :" + perceptID +"-"+this);
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void planBody() 
	{
		ABMBDILoggerSetter.setup(LOGGER);
	//*System.out.println("["+agentID+"]"+"---IN IDLE PLAN BEFORE CALCULATE ROOT");
		int noOfRoot = calculateNoOfRoot();
		noOfRoot--; //This plan itself is considered as a TopLevelGoal root.
		int noOfWait = ((SyncInteger)getBeliefbase().getBelief("noOfWait").getFact()).read();
   //System.out.println("["+agentID+"]"+"---NO WAIT :"+noOfWait + "---NO ROOT :"+noOfRoot);
		//This rechecks of condition is required, 
		//as there is a possibility of messages to be handed down during
		if (noOfRoot == noOfWait)
		{
			startAtomic();
		//*System.out.println("["+agentID+"]"+"*****************IDLE*********");
			//Prevent other plan to execute
			getBeliefbase().getBelief("isIdle").setFact(true);
			ISpaceObject myself = (ISpaceObject)getBeliefbase().getBelief("myself").getFact();
			//idle
			getBeliefbase().getBelief("agentState").setFact(IDLE);

			Map parameters = new HashMap();
			parameters.put("objectID", myself.getId());
			parameters.put("objectType", myself.getType());
			parameters.put("agentID", myself.getProperty("agentID"));
			
			callEnvironmentAction("idle_action", parameters);
			//LOGGER.info(myself.getProperty("agentID") + " IDLE");
			endAtomic();
			super.waitForFactChanged("agentState"); //agentState change will be triggered by Environment
			
			int agentState = (Integer)getBeliefbase().getBelief("agentState").getFact();
			//*System.out.println(agentID+"------just wake up");
			//updateBelief
			if (agentState == RUN_ACTION_PERCEPT || agentState == RUN_NOACTION_PERCEPT)
			{
				updateAgentBeliefs();
			}
			//System.out.println("------belief updated :" + this);
			//Signal Changes of Action State.
			if (agentState == RUN_ACTION_PERCEPT || agentState == RUN_ACTION_NOPERCEPT)
			{
				getBeliefbase().getBelief("actionChange").setFact(!((Boolean)getBeliefbase().getBelief("actionChange").getFact()));
				//System.out.println("------belief updated :" + this);
			}
			getBeliefbase().getBelief("isIdle").setFact(false);
			
		}
		else
		{
			fail();
		}
	}
	
	/**
	 * To call a Jadex's Environment Action.
	 * @param actionID
	 * The action identifier of Jadex's action
	 * @param parameters
	 * The parameters to be passed to the Jadex's action
	 */
	@SuppressWarnings("rawtypes")
	private void callEnvironmentAction (String actionID, Map parameters)
	{
		IEnvironmentSpace env = (IEnvironmentSpace)getBeliefbase().getBelief("environment").getFact();
		SyncResultListener srl	= new SyncResultListener();
		env.performSpaceAction(actionID, parameters, srl);
	}
	
//	@SuppressWarnings({ "rawtypes", "unchecked" })
//	private int addFloatingMsgNum (int addValue)
//	{
//		Map parameters = new HashMap();
//		parameters.put("value", addValue);
//		int retVal = (Integer)callEnvironmentActionAndWait("update_float_action",parameters);
//		return retVal;
//	}
//	
//	@SuppressWarnings("rawtypes")
//	private Object callEnvironmentActionAndWait (String actionID, Map parameters)
//	{
//		Object retVal = null;
//		IEnvironmentSpace env = (IEnvironmentSpace)getBeliefbase().getBelief("environment").getFact();
//		SyncResultListener srl	= new SyncResultListener();
//		env.performSpaceAction(actionID, parameters, srl);
//		try 
//		{
//			retVal = srl.waitForResult();
//		} 
//		catch (Exception e) 
//		{
//			LOGGER.severe(e.getMessage());
//		}
//		return retVal;
//	}
	
	/**
	 * Calculate the number of roots, which 
	 * could caused a reasoning process in Jadex.
	 * @return
	 */
	private int calculateNoOfRoot()
	{
		SyncInteger noOfTopLevelGoal = (SyncInteger)getBeliefbase().getBelief("noOfTopLevelGoal").getFact();
		SyncInteger noOfMessageEvent = (SyncInteger)getBeliefbase().getBelief("noOfMessageEvent").getFact();
		int noOfRoot = noOfTopLevelGoal.read() + noOfMessageEvent.read();
		return noOfRoot;
	}
}
