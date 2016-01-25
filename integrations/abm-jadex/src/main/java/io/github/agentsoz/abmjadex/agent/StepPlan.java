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



import io.github.agentsoz.abmjadex.central_organizer.StartPlan;
import io.github.agentsoz.abmjadex.data_structure.SuspendedGoals;
import io.github.agentsoz.abmjadex.data_structure.SyncArrayList;
import io.github.agentsoz.abmjadex.data_structure.SyncInteger;
import io.github.agentsoz.abmjadex.miscellaneous.ABMBDILoggerSetter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import io.github.agentsoz.bdiabm.data.ActionContainer;
import io.github.agentsoz.bdiabm.data.ActionContent.State;


import jadex.bdi.runtime.GoalFailureException;
import jadex.bdi.runtime.IExternalCondition;
import jadex.bdi.runtime.IGoal;
import jadex.bdi.runtime.IInternalEvent;
import jadex.bdi.runtime.IMessageEvent;
import jadex.bdi.runtime.Plan;
import jadex.bdi.runtime.PlanFailureException;
import jadex.bdi.runtime.impl.flyweights.GoalFlyweight;
import jadex.bdi.runtime.impl.flyweights.InternalEventFlyweight;
import jadex.bdi.runtime.impl.flyweights.MessageEventFlyweight;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.fipa.SFipa;
import jadex.commons.future.IFuture;
import jadex.extension.envsupport.environment.IEnvironmentSpace;
import jadex.extension.envsupport.environment.ISpaceObject;

public abstract class StepPlan extends Plan
{
	/**
	 * This plan is the replacement to Jadex's Plan instance if a
	 * developer want to use the Jadex-Repast Collaboration system.
	 * 
	 * This Plan consists of counting mechanism of numbers of roots (plan triggers) 
	 * and waiting plans. 
	 * 
	 * Whenever this plan is finished or in waiting situation (which waiting do not
	 * depends on time.) then it would compare the numbers of roots and waits.
	 * When the comparison result is equal, it would launch a goal which will trigger
	 * the instantiation of a StepManagerPlan.
	 */
	private static final long serialVersionUID = -2069576934203261634L;
	
	private final static Logger LOGGER = Logger.getLogger(StepPlan.class.getName());
	
	private final static String WAIT_FOR_CONDITION = "waitForCondition";
	private final static String WAIT_FOR_EVER = "waitForEver";
	private final static String WAIT_FOR_EXT_CONDITION = "waitForExternalCondition";
	private final static String WAIT_FOR_FACT_ADDED = "waitForFactAdded";
	private final static String WAIT_FOR_FACT_ADDED_REMOVED = "waitForFactAddedOrRemoved";
	private final static String WAIT_FOR_FACT_CHANGED = "waitForFactChanged";
	private final static String WAIT_FOR_FACT_REMOVED = "waitForFactRemoved";
	private final static String WAIT_FOR_GOAL = "waitForGoal";
	private final static String WAIT_FOR_INTERNAL_EVENT = "waitForInternalEvent";
	private final static String WAIT_FOR_MESSAGE_EVENT = "waitForMessageEvent";
	
	private boolean isTopLevelGoal = false;
	private boolean isMessageEvent = false;
	private boolean isDispatchSubgoalAndWait = false;
	private boolean isFinished = false;
	private boolean isWaiting = false;
	private ArrayList<IGoal> dispatchSubgoalNoWait = new ArrayList<IGoal>();
	
	private String actionCommenced = null;
	
	@SuppressWarnings("unused")
	private boolean isFail = false;
	
	//private boolean hasWaitPoint = false; 
	public String agentID = "["+(String)((ISpaceObject)getBeliefbase().getBelief("myself").getFact()).getProperty("agentID")+"]";
	private int localNoOfWait = 0;
	
	@Override
	public final void body()
	{
		ABMBDILoggerSetter.setup(LOGGER);
		RuntimeException exception = null;
		
		try 
		{
			planPreparation();	
			//Trap those activity who comes while agent is idle (e.g. inter-agent messages)
			waitUntilNotIdle();
//			if (this.getType().contains("ReplyProcessorPlan"))
//				System.out.println(agentID + " ********************~~~~~~~~~~~~enter"+getType());
			planBody();
		} 
		catch (RuntimeException e)
		{
			if (!(e instanceof GoalFailureException) && !(e instanceof PlanFailureException))
			{
				LOGGER.severe(e.getMessage() +"-"+ getType());
				e.printStackTrace();
			}
			exception = e;
		}
		
		if (exception != null)
			throw exception;
	}
	
	/**
	 * The one acting as Jadex's Plan's body,
	 * if the user want to utilize the Jadex-Repast System.
	 */
	public abstract void planBody();
	
	/**
	 * Decrease the number of wait Point in 
	 * the Agent's belief.
	 */
//	private void decreaseNoOfWait ()
//	{
//		SyncInteger noOfWait = (SyncInteger)getBeliefbase().getBelief("noOfWait").getFact();
//		noOfWait.add(-1);
//		getBeliefbase().getBelief("noOfWait").setFact(noOfWait);
//	}
	
	/**
	 * Increase the number of wait Point in 
	 * the Agent's belief.
	 */
//	private void increaseNoOfWait()
//	{
//		SyncInteger noOfWait = (SyncInteger)getBeliefbase().getBelief("noOfWait").getFact();
//		noOfWait.add(1);
//		getBeliefbase().getBelief("noOfWait").setFact(noOfWait);
//	}
	
	private void addNoOfWait (int addNum)
	{
		SyncInteger noOfWait = (SyncInteger)getBeliefbase().getBelief("noOfWait").getFact();
		//startAtomic();
		noOfWait.add(addNum);
		localNoOfWait =  localNoOfWait + addNum;
	//*System.out.println("["+agentID+"]"+ "ADD NO OF WAIT " + addNum+"~~~~~~~~~~~~~~~~~~~~~~~");
		//endAtomic();
		getBeliefbase().getBelief("noOfWait").setFact(noOfWait);
	}
	
	/**
	 * A filtering procedure to prevent
	 * any reasoning execution as long as the agent itself
	 * belief that it is in idle.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void waitUntilNotIdle ()
	{
		//In case this plan's trigger is a message coming from others Central Organizer
		if (getReason().getClass().equals(MessageEventFlyweight.class))
		{
			IMessageEvent me = (IMessageEvent)getReason();
			IComponentIdentifier sender = (IComponentIdentifier)me.getParameter(SFipa.SENDER).getValue();
			IEnvironmentSpace env = (IEnvironmentSpace)getBeliefbase().getBelief("environment").getFact();
			Map componentAgentIDMap = (Map)env.getProperty("componentAgentIDMap");
			
			if (componentAgentIDMap.containsKey(sender) == false)
			{
				Map params = new HashMap();
				params.put("receiver", this.getComponentIdentifier().toString());
				params.put("sender", ((IComponentIdentifier)me.getParameter(SFipa.SENDER).getValue()).toString());
				this.callEnvironmentActionAndWait("inter_central_action", params);
				
				Boolean isIdle = (Boolean)getBeliefbase().getBelief("isIdle").getFact();
				if (isIdle)
				{
					//In case if it is idle and the messsage is from outerAgent then wake himself up
					StartPlan.BDIServer server = (StartPlan.BDIServer)env.getProperty("server");
					server.setControlStatus(true);
					
					Map parameters = new HashMap();
					IComponentIdentifier[] receivers = new IComponentIdentifier[1];
					receivers[0] = this.getComponentIdentifier();
					parameters.put("receivers", receivers);
					callEnvironmentAction("unidle_agent_action",parameters);
				}
			}
			else
			{
				Boolean isIdle = (Boolean)getBeliefbase().getBelief("isIdle").getFact();
				if (isIdle)
				{
					//In case if it is idle and the messsage is from outerAgent then wake himself up
					StartPlan.BDIServer server = (StartPlan.BDIServer)env.getProperty("server");
					server.setControlStatus(true);
					
					Map parameters = new HashMap();
					IComponentIdentifier[] receivers = new IComponentIdentifier[1];
					receivers[0] = this.getComponentIdentifier();
					parameters.put("receivers", receivers);
					callEnvironmentAction("unidle_agent_action",parameters);
					Object	fact	= getBeliefbase().getBelief("agentState").getFact();
					if (fact.equals(StepManagerPlan.IDLE))
						getBeliefbase().getBelief("agentState").setFact(StepManagerPlan.RUN_NOACTION_NOPERCEPT);
				}
				isIdle = (Boolean)getBeliefbase().getBelief("isIdle").getFact();
				
				//IMessageEvent trigger = (IMessageEvent)getReason();
				//String senderx = ((IComponentIdentifier)trigger.getParameter(SFipa.SENDER).getValue()).toString();
				//System.out.println(agentID+"-["+getComponentIdentifier()+"] Got message from :" +senderx);
				//System.out.println(agentID +" ********************IDLE? "+isIdle);
			//*System.out.println("["+agentID + "]"+" BEFORE FLOATING MODIFICATION");
				//startAtomic();
				addFloatingMsgNum(-1);
				//System.out.println(agentID + "No Of Floating : " + x);
//				((SyncInteger)env.getProperty("floatingMsgNum")).add(-1);
//				Map parameters = new HashMap();
//				parameters.put("value", 1);
//				callEnvironmentAction("unidle_agent_action",parameters);
			//*System.out.println("["+agentID + "]"+" AFTER FLOATING MODIFICATION");
				//System.out.println(newFloatingMsgNum);
//				env.setProperty("floatingMsgNum", newFloatingMsgNum);
				//endAtomic();
			}
			
		}
		Boolean isIdle = (Boolean)getBeliefbase().getBelief("isIdle").getFact();
		
		//if (isIdle)
		while(isIdle)
		{
		//System.out.println(agentID +" ********************STUCK AND GOT MESSAGE ?"+getReason().getClass().equals(MessageEventFlyweight.class));
		//*printRootWaitStatus();
			super.waitForFactChanged("isIdle");
			isIdle = (Boolean)getBeliefbase().getBelief("isIdle").getFact();
		//*System.out.println("["+agentID + "]"+" ********************RELEASE AND GOT MESSAGE");
		}
		
	}
	
	@SuppressWarnings("unused")
	private int readFloatingMsgNum ()
	{
		return addFloatingMsgNum(0);
	}

	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private int addFloatingMsgNum (int addValue)
	{
		Map parameters = new HashMap();
		parameters.put("value", addValue);
		int retVal = (Integer)callEnvironmentAction("update_float_action",parameters);
		return retVal;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public IMessageEvent sendMessageAndWait (IMessageEvent me)
	{
//		System.out.println(agentID + "SENTXXXXXXXXXXXXXXXXXXXXXXXX");
		IMessageEvent reply = super.sendMessageAndWait(me);
//		System.out.println(agentID + "rRECEIVE---------------------------------------");
		//Get sender of message
		IComponentIdentifier sender = (IComponentIdentifier)reply.getParameter(SFipa.SENDER).getValue();
		IEnvironmentSpace env = (IEnvironmentSpace)getBeliefbase().getBelief("environment").getFact();
		Map componentAgentIDMap = (Map)env.getProperty("componentAgentIDMap");
		
		if (componentAgentIDMap.containsKey(sender) == false)
		{
			//If sender is in different CO
			Map params = new HashMap();
			params.put("receiver", this.getComponentIdentifier().toString());
			params.put("sender", ((IComponentIdentifier)reply.getParameter(SFipa.SENDER).getValue()).toString());
			this.callEnvironmentActionAndWait("inter_central_action", params);
			
			Boolean isIdle = (Boolean)getBeliefbase().getBelief("isIdle").getFact();
			if (isIdle)
			{
				//In case if it is idle and the messsage is from outerAgent then wake himself up
				StartPlan.BDIServer server = (StartPlan.BDIServer)env.getProperty("server");
				server.setControlStatus(true);
				
				Map parameters = new HashMap();
				IComponentIdentifier[] receivers = new IComponentIdentifier[1];
				receivers[0] = this.getComponentIdentifier();
				parameters.put("receivers", receivers);
				callEnvironmentAction("unidle_agent_action",parameters);
			}
		}
		else
		{
			//if sender is in the same CO
			Boolean isIdle = (Boolean)getBeliefbase().getBelief("isIdle").getFact();
			if (isIdle)
			{
				//In case if it is idle and the messsage is from outerAgent then wake himself up
				StartPlan.BDIServer server = (StartPlan.BDIServer)env.getProperty("server");
				server.setControlStatus(true);
				
				Map parameters = new HashMap();
				IComponentIdentifier[] receivers = new IComponentIdentifier[1];
				receivers[0] = this.getComponentIdentifier();
				parameters.put("receivers", receivers);
				callEnvironmentAction("unidle_agent_action",parameters);
			}
			//startAtomic();
			addFloatingMsgNum(-1);
//			((SyncInteger)env.getProperty("floatingMsgNum")).add(-1);
//			int newFloatingMsgNum = (Integer)env.getProperty("floatingMsgNum") - 1;
//			//System.out.println(newFloatingMsgNum);
//			env.setProperty("floatingMsgNum", newFloatingMsgNum);
			//endAtomic();
		}
		return reply;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public IFuture sendMessage(IMessageEvent me, byte[] codecids)
	{	
		IComponentIdentifier[] receivers = (IComponentIdentifier[])me.getParameterSet(SFipa.RECEIVERS).getValues();
		IEnvironmentSpace env = (IEnvironmentSpace)getBeliefbase().getBelief("environment").getFact();
		Map componentAgentIDMap = (Map)env.getProperty("componentAgentIDMap");
		
		//Checks on the existence of receivers inside the corresponding Central Organizer
		//outerReceivers : those agent who are not govern 
		//					by the Jadex' Environment (Central Organizer) of this agent
		//innerReceivers : those agent who are govern 
		//					by the Jadex' Environment (Central Organizer) of this agent
		Set outerReceivers = new HashSet();
		Set innerReceivers = new HashSet();
		
		for (int i = 0; i < receivers.length; i++)
		{
			if (componentAgentIDMap.containsKey(receivers[i]))
			{
				innerReceivers.add(receivers[i]);
			}
			else
			{
				outerReceivers.add(receivers[i].toString());
			}
		}
		
		//Action to do to keep integrity of whole agents idle state system.
		if (outerReceivers.size() > 0)
		{
			Map parameters = new HashMap();
			parameters.put("receivers", outerReceivers.toArray());
			parameters.put("sender", this.getComponentIdentifier().toString());
			
			this.callEnvironmentActionAndWait("inter_central_action", parameters);
		}
		
		IFuture result = super.sendMessage(me, codecids);
		
		//Action to do on innerReceivers
		if (innerReceivers.size()>0)
		{
			Map parameters = new HashMap();
			parameters.put("receivers", innerReceivers.toArray());
			//startAtomic();
			addFloatingMsgNum(innerReceivers.size());
//			((SyncInteger)env.getProperty("floatingMsgNum")).add(innerReceivers.size());
//			int newFloatMsgNum = (Integer)env.getProperty("floatingMsgNum") + innerReceivers.size();
//			//System.out.println(newFloatMsgNum);
//			env.setProperty("floatingMsgNum", newFloatMsgNum);
//			endAtomic();
//			callEnvironmentAction("unidle_agent_action",parameters);
		}
		return result;
	}

	/**
	 * Preparing the plan to get all the idle
	 * monitoring system take off.
	 */
	private void planPreparation()
	{
		SyncArrayList suspendedGoals = (SyncArrayList)getBeliefbase().getBelief("suspendedGoals").getFact();
		
		if(getReason().getClass().equals(GoalFlyweight.class))
		{
			boolean isSuspendedGoal = false;
			isSuspendedGoal = suspendedGoals.contains(new SuspendedGoals((IGoal)this.getReason(),false,false));
	
			if(isSuspendedGoal == true)
			{
				//Set the plan status from the saved data in the suspendedGoals list
				int index = suspendedGoals.indexOf(new SuspendedGoals((IGoal)this.getReason(),false,false));
				this.isMessageEvent = ((SuspendedGoals)suspendedGoals.get(index)).isMessage();
				this.isTopLevelGoal = ((SuspendedGoals)suspendedGoals.get(index)).isTopLevel();
				//Update the new suspendGoals list.
				suspendedGoals.remove(index);
		
				getBeliefbase().getBelief("suspendedGoals").setFact(suspendedGoals);
				//decrease the no of waiting goal (suspended goal treated as a waited goal)
				
//				decreaseNoOfWait ();
				addNoOfWait(-1);
			}
			else
			{
				planStatusPreparation();
			}
		}
		else
		{
			planStatusPreparation();
		}
	}
	
	/**
	 * Plan status based on the one triggered it
	 * would be evaluated here.
	 */
	private void planStatusPreparation()
	{
		//Probability of being a subgoal need to be check first, 
		//or else the subgoal could be mistakenly thought as TopLevelGoal
		if (getReason().getClass().equals(GoalFlyweight.class))
		{
			IGoal reason = (IGoal)getReason();
			boolean isNotSubgoal = false;
			SyncArrayList subGoals = (SyncArrayList)getBeliefbase().getBelief("subGoals").getFact();
			
			isNotSubgoal =!subGoals.contains(reason);
			if (isNotSubgoal)
			{
				isTopLevelGoal = true;
				increaseNoOfTopLevelGoal();
			}
		}
		else if (getReason().getClass().equals(MessageEventFlyweight.class))
		{
			isMessageEvent = true;
			SyncInteger noOfMessageEvent = (SyncInteger)getBeliefbase().getBelief("noOfMessageEvent").getFact();
//			noOfMessageEvent++;
			noOfMessageEvent.add(1);
			getBeliefbase().getBelief("noOfMessageEvent").setFact(noOfMessageEvent);
		}
		else if (getReason().getClass() == InternalEventFlyweight.class)
		{
			isMessageEvent = true;
			//No need to increase the number of messageEvent while, it is already increased when dispatch
		}
	}
	
	/**
	 * increasing the no of TopLevelGoal.
	 * a TopLevelGoal is counted as a Root.
	 */
	private void increaseNoOfTopLevelGoal()
	{
		SyncArrayList dispatchedTopLevelGoal = (SyncArrayList)getBeliefbase().getBelief("dispatchedTopLevelGoal").getFact();
		boolean isDispatchedTopLevelGoal = false;
		isDispatchedTopLevelGoal = dispatchedTopLevelGoal.contains(this.getReason());

		if (isDispatchedTopLevelGoal == true)
		{
			dispatchedTopLevelGoal.remove(this.getReason());
			getBeliefbase().getBelief("dispatchedTopLevelGoal").setFact(dispatchedTopLevelGoal);
		}
		else
		{
			SyncInteger noOfTopLevelGoal = (SyncInteger) getBeliefbase().getBelief("noOfTopLevelGoal").getFact();
			noOfTopLevelGoal.add(1);
			
			getBeliefbase().getBelief("noOfTopLevelGoal").setFact(noOfTopLevelGoal);
		}
		
	}
	
	public void procedureAfterActionUpdate()
	{
		
	}
	
	/**
	 * Demanding action to be done in Repast
	 * @param identifier
	 * The action identifier 
	 * (The one agreed to be used in Jadex and Repast)
	 * @param parameters
	 * The parameters of the action.
	 */
	public void act (String identifier, Object[] parameters)
	{
		//30 May 2013
		Boolean isUpdated = false;
		//hasWaitPoint = true;
		//Get the Object's local Container
		ActionContainer localActionContainer = (ActionContainer)((ISpaceObject)
				getBeliefbase().getBelief("myself").getFact()).getProperty("actionContainer");

		//Register to the plan's list of action
		actionCommenced = identifier;
		//Register To the local container
		if (!localActionContainer.register(identifier, parameters))
		{
			localActionContainer.get(identifier).setState(State.RUNNING);
		}
		
		((ISpaceObject)getBeliefbase().getBelief("myself").getFact()).setProperty("actionContainer", localActionContainer);
		
		State state = localActionContainer.get(identifier).getState();
		
		while (state.equals(State.INITIATED) || state.equals(State.RUNNING))
		{
			Integer noOfRoot = calculateNoOfRoot();
			
//			SyncInteger noOfWait = (SyncInteger)getBeliefbase().getBelief("noOfWait").getFact();
//			noOfWait.add(1);
//			getBeliefbase().getBelief("noOfWait").setFact(noOfWait);
			addNoOfWait(1);
//			System.out.println("ChecInit-" + this);
			if (isUpdated == true)
				procedureAfterActionUpdate();
			
			SyncInteger noOfWait = (SyncInteger)getBeliefbase().getBelief("noOfWait").getFact();
			this.isWaiting = true;
			noOfWait = (SyncInteger)getBeliefbase().getBelief("noOfWait").getFact();
			noOfRoot = calculateNoOfRoot();
//			System.out.println("Wait here-" +this);
			if (noOfRoot.equals(noOfWait.read()))
			{
//				System.out.println("Idle");
				agentIdle();
			}
			isUpdated = false;
			super.waitForFactChanged("actionChange");
			isUpdated = true;
//			noOfWait = (SyncInteger)getBeliefbase().getBelief("noOfWait").getFact();
//			noOfWait.add(-1);
//			getBeliefbase().getBelief("noOfWait").setFact(noOfWait);
			addNoOfWait(-1);
//			System.out.println("Check State");
			this.isWaiting = false;
			localActionContainer = (ActionContainer)((ISpaceObject)
					getBeliefbase().getBelief("myself").getFact()).getProperty("actionContainer");
			state = localActionContainer.get(identifier).getState();
		}
		if (state.equals(State.FAILED))
		{
			fail();
		}
		else if (state.equals(State.PASSED))
		{
			localActionContainer = (ActionContainer)((ISpaceObject)
					getBeliefbase().getBelief("myself").getFact()).getProperty("actionContainer");
			localActionContainer.remove(identifier);
			((ISpaceObject)getBeliefbase().getBelief("myself").getFact()).setProperty("actionContainer", localActionContainer);
			actionCommenced = null;
		}
	}
	
	@Override
	public void dispatchInternalEvent(IInternalEvent ie)
	{
		SyncInteger noOfMessageEvent = (SyncInteger)getBeliefbase().getBelief("noOfMessageEvent").getFact();
//		noOfMessageEvent++;
		noOfMessageEvent.add(1);
		getBeliefbase().getBelief("noOfMessageEvent").setFact(noOfMessageEvent);
		super.dispatchInternalEvent(ie);
	}
	
	@Override
	public void dispatchSubgoalAndWait(IGoal goal)
	{
		subgoalUpdate(goal);
		this.isDispatchSubgoalAndWait = true;
	//*printRootWaitStatus();
		super.dispatchSubgoalAndWait(goal);
		this.isDispatchSubgoalAndWait = false;
			//startAtomic();
			SyncArrayList subGoals = (SyncArrayList)getBeliefbase().getBelief("subGoals").getFact();
			subGoals.remove(goal);
			getBeliefbase().getBelief("subGoals").setFact(subGoals);
			//endAtomic();
	}
	
	@Override
	public void dispatchSubgoal(IGoal goal)
	{
		if (!isDispatchSubgoalAndWait)
		{
			dispatchSubgoalNoWait.add(goal);
			SyncInteger noOfTopLevelGoal = (SyncInteger) getBeliefbase().getBelief("noOfTopLevelGoal").getFact();
			noOfTopLevelGoal.add(1);
			getBeliefbase().getBelief("noOfTopLevelGoal").setFact(noOfTopLevelGoal);
			SyncArrayList dispatchedTopLevelGoal 
					= (SyncArrayList)getBeliefbase().getBelief("dispatchedTopLevelGoal").getFact();
			
			if (!dispatchedTopLevelGoal.contains(goal))
				dispatchedTopLevelGoal.add(goal);
			
			getBeliefbase().getBelief("dispatchedTopLevelGoal").setFact(dispatchedTopLevelGoal);
		}
		super.dispatchSubgoal(goal);
	}
	
	@Override
	public void dispatchTopLevelGoal(IGoal goal)
	{
		SyncInteger noOfTopLevelGoal = (SyncInteger) getBeliefbase().getBelief("noOfTopLevelGoal").getFact();
		noOfTopLevelGoal.add(1);
		getBeliefbase().getBelief("noOfTopLevelGoal").setFact(noOfTopLevelGoal);
		SyncArrayList dispatchedTopLevelGoal 
				= (SyncArrayList)getBeliefbase().getBelief("dispatchedTopLevelGoal").getFact();
		
		if (!dispatchedTopLevelGoal.contains(goal))
			dispatchedTopLevelGoal.add(goal);
		
		
		getBeliefbase().getBelief("dispatchedTopLevelGoal").setFact(dispatchedTopLevelGoal);
		super.dispatchTopLevelGoal(goal);
	}
	
	private void subgoalUpdate(IGoal goal)
	{
		//startAtomic();
		SyncArrayList subGoals = (SyncArrayList)getBeliefbase().getBelief("subGoals").getFact();
		subGoals.add(goal);
		getBeliefbase().getBelief("subGoals").setFact(subGoals);
		//endAtomic();
	}
	
	@Override
	public final void failed()
	{
		planFailed();
		isFail = true;
		//this.noTalkie(this +" fail "); 
		finish();
		ActionContainer localActionContainer = (ActionContainer)((ISpaceObject)
				getBeliefbase().getBelief("myself").getFact()).getProperty("actionContainer");
		if (actionCommenced != null)
		{
			localActionContainer.remove(actionCommenced);
		}
		((ISpaceObject)getBeliefbase().getBelief("myself").getFact()).setProperty("actionContainer", localActionContainer);
		super.failed();	
	}
	
	/**
	 * A replacement of Jadex's default failed() method()
	 */
	public void planFailed()
	{
		
	}
	
	/**
	 * A debugging tools to see
	 * no of roots and waits
	 * @param str
	 */
	public void noTalkie(String str)
	{
		int noOfRoot = calculateNoOfRoot();
		int noOfWait = ((SyncInteger)getBeliefbase().getBelief("noOfWait").getFact()).read();
		System.out.println(str +":: #roots ="+ noOfRoot + "_#waits ="+noOfWait);
	}
		
	@Override
	public void aborted()
	{
		planAborted();
		if (((IGoal)getReason()).getLifecycleState().equalsIgnoreCase("suspended"))
		{
		//*System.out.println("["+agentID+"]" + getType()+" SUSPENDED");
			//A Suspended plan is treated like a waiting plan, as it will be invoke once the goal
			//comes into valid context condition
			if (isWaiting == false)
			{
//				SyncInteger noOfWait = (SyncInteger)getBeliefbase().getBelief("noOfWait").getFact();
//				noOfWait.add(1);
//				getBeliefbase().getBelief("noOfWait").setFact(noOfWait);
				addNoOfWait(1);
			}
			
			//Setting all action demanded by this plan to be suspended
			ActionContainer localActionContainer = (ActionContainer)((ISpaceObject)
					getBeliefbase().getBelief("myself").getFact()).getProperty("actionContainer");
			if(actionCommenced != null)
			{
				localActionContainer.get(actionCommenced).setState(State.SUSPENDED);
			}
			((ISpaceObject)getBeliefbase().getBelief("myself").getFact()).setProperty("actionContainer", localActionContainer);
			
			//Writing the reason of this suspended plan into suspendedGoals List
			SyncArrayList suspendedGoals = (SyncArrayList)this.getBeliefbase().getBelief("suspendedGoals").getFact();
			
			
			if (!suspendedGoals.contains(new SuspendedGoals((IGoal)this.getReason(),false,false)))
			{
				suspendedGoals.add(new SuspendedGoals((IGoal)this.getReason(),this.isTopLevelGoal,this.isMessageEvent));
			}
			
			getBeliefbase().getBelief("suspendedGoals").setFact(suspendedGoals);
			
		}
		else
		{
			isFail = true;
			finish();
		}
		
		ActionContainer localActionContainer = (ActionContainer)((ISpaceObject)
				getBeliefbase().getBelief("myself").getFact()).getProperty("actionContainer");
		
		if (((IGoal)getReason()).getLifecycleState().equalsIgnoreCase("DROPPED"))
		{
			if (actionCommenced != null)
			{
				localActionContainer.get(actionCommenced).setState(State.DROPPED);
			}
		}
		((ISpaceObject)getBeliefbase().getBelief("myself").getFact()).setProperty("actionContainer", localActionContainer);

		super.aborted();
	}
	
	/**
	 * A replacement of Jadex's default aborted() method()
	 */
	public void planAborted ()
	{
		
	}

	@Override
	public void passed()
	{
		planPassed();
		finish();
		super.passed();
		
	}
	
	/**
	 * A replacement of Jadex's default passed() method()
	 */
	public void planPassed ()
	{
		
	}
	/**
	 * A housekeeping method used to synchronize
	 * the counting of roots and waits whenever
	 * a plan is finished (aborted, failed, passed, etc.)
	 */
	private void finish ()
	{
		try
		{
		//System.out.println(agentID + "in finish");
		releaseUnfulfilledSubgoalsNoWait();
		if (!isFinished)
		{
			Integer noOfRoot = calculateNoOfRoot();
			SyncInteger noOfWait = (SyncInteger)getBeliefbase().getBelief("noOfWait").getFact();
			
//			if (isWaiting)
//			{
////				noOfWait = (SyncInteger)getBeliefbase().getBelief("noOfWait").getFact();
////				noOfWait.add(-1);
////				getBeliefbase().getBelief("noOfWait").setFact(noOfWait);
//				addNoOfWait(-1);
//			}
			addNoOfWait(-1*localNoOfWait);
			
			if (isMessageEvent)
			{
				SyncInteger noOfMessageEvent = (SyncInteger)getBeliefbase().getBelief("noOfMessageEvent").getFact();
//				noOfMessageEvent--;
				noOfMessageEvent.add(-1);
				getBeliefbase().getBelief("noOfMessageEvent").setFact(noOfMessageEvent);
			}
			else if (isTopLevelGoal)
			{
				SyncInteger noOfTopLevelGoal = (SyncInteger)getBeliefbase().getBelief("noOfTopLevelGoal").getFact();
				noOfTopLevelGoal.add(-1);
				getBeliefbase().getBelief("noOfTopLevelGoal").setFact(noOfTopLevelGoal);
			}
			else
			{
				/*
				 * Remove goals in the subGoals list which is not exist anymore
				 */
				IGoal[] goalList = getGoalbase().getGoals();
				//startAtomic();
				SyncArrayList subGoals = (SyncArrayList)getBeliefbase().getBelief("subGoals").getFact();
				//Sometimes using the following syntax to explore the collection deteriorate the collection
				//content itself.
				//for (Object goal: subGoals)
					
				for (int j = 0; j < subGoals.size(); j++)
				{
					Object goal = subGoals.get(j);
					boolean isContained = false;
					for (int i = 0; i < goalList.length; i++)
					{
						if (goal.equals(goalList[i]))
						{
							isContained = true;
						}
					}
					if (!isContained)
					{
						subGoals.remove(goal);
					}	
				}	
				
				getBeliefbase().getBelief("subGoals").setFact(subGoals);
				//endAtomic();
			}
			noOfRoot = calculateNoOfRoot();
			noOfWait = (SyncInteger)getBeliefbase().getBelief("noOfWait").getFact();
	
		//*printRootWaitStatus("END OF FINISH() in" + this.getType());
			if (noOfRoot.equals(noOfWait.read()))
			{
				agentIdle();
			}
		}
		isFinished = true;
		
		//To trap an exclude="never" plan so that it is limited to be ran once per time step.
		//Problem : if there are no new percept or action change, this agent will not wake up
		//			and there will be no exclude never action in the next step.
		if(getReason().getClass().equals(GoalFlyweight.class))
		{
			IGoal reason = (IGoal)getReason();
			if (reason.getExcludeMode().equals("never"))
			{
				//System.out.println(agentID + "in finish and never");
				Boolean isIdle = (Boolean)getBeliefbase().getBelief("isIdle").getFact();
				do
				{
					super.waitForFactChanged("isIdle");
					isIdle = (Boolean)getBeliefbase().getBelief("isIdle").getFact();
				} while (isIdle);
			}
		}
		} catch (Exception e)
		{
			LOGGER.severe("in finish : " +e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * To refresh the number of roots as
	 * The subgoals dispatched without wait will also
	 * exterminated as this plan exterminated.
	 */
	private void releaseUnfulfilledSubgoalsNoWait()
	{
		SyncInteger noOfTopLevelGoal = (SyncInteger) getBeliefbase().getBelief("noOfTopLevelGoal").getFact();
		
		SyncArrayList dispatchedTopLevelGoal 
				= (SyncArrayList)getBeliefbase().getBelief("dispatchedTopLevelGoal").getFact();
		Object[] dispatchSubGoalNoWaitA = dispatchSubgoalNoWait.toArray();
		//for(Object goal : this.dispatchSubgoalNoWait)
		for (int i = 0; i<dispatchSubGoalNoWaitA.length; i++)
		{
			Object goal = dispatchSubGoalNoWaitA[i];
			if (dispatchedTopLevelGoal.contains(goal))
			{
				dispatchedTopLevelGoal.remove(goal);
	//			noOfTopLevelGoal = noOfTopLevelGoal - 1;
				noOfTopLevelGoal.add(-1);
			}
			
			
		}
		getBeliefbase().getBelief("noOfTopLevelGoal").setFact(noOfTopLevelGoal);
		getBeliefbase().getBelief("dispatchedTopLevelGoal").setFact(dispatchedTopLevelGoal);	
	}

	
	/**
	 * To call a Jadex's Environment Action.
	 * @param actionID
	 * The action identifier of Jadex's action
	 * @param parameters
	 * The parameters to be passed to the Jadex's action
	 */
	@SuppressWarnings("rawtypes")
	private Object callEnvironmentAction (String actionID, Map parameters)
	{
		Object retVal = null;
		IEnvironmentSpace env = (IEnvironmentSpace)getBeliefbase().getBelief("environment").getFact();

		//retVal = env.performSpaceAction(actionID, parameters);
		SyncResultListener srl	= new SyncResultListener();
		//System.out.println(agentID + "PERFORMING SOME ACTION - "+actionID);
		env.performSpaceAction(actionID, parameters, srl);
		
		try 
		{
			//System.out.println(agentID + "HERE INSIDE TRY @@@@@@@@@@ - "+actionID);
			retVal = srl.waitForResult();
		} 
		catch (Exception e) 
		{
			//System.out.println(agentID + "HERE INSIDE CATCH @@@@@@@@@@ - "+actionID);
			LOGGER.severe(e.getMessage());
		}
		//System.out.println(agentID + "END PERFORMING SOME ACTION - "+actionID);
		return retVal;
	}
	
	/**
	 * To call a Jadex's Environment Action.
	 * and wait until result returned
	 * @param actionID
	 * The action identifier of Jadex's action
	 * @param parameters
	 * The parameters to be passed to the Jadex's action
	 */
	@SuppressWarnings("rawtypes")
	private Object callEnvironmentActionAndWait (String actionID, Map parameters)
	{
		Object returnVal = null;
		IEnvironmentSpace env = (IEnvironmentSpace)getBeliefbase().getBelief("environment").getFact();
		SyncResultListener srl	= new SyncResultListener();
		env.performSpaceAction(actionID, parameters, srl);
		try
		{
			returnVal = srl.waitForResult();
		}
		catch(RuntimeException e)
		{
			LOGGER.severe(e.getMessage());
		}
		return returnVal;
	}
	
	/**
	 * To calculate the current no of roots
	 * @return
	 * No of roots
	 */
	private int calculateNoOfRoot()
	{
		SyncInteger noOfTopLevelGoal = (SyncInteger)getBeliefbase().getBelief("noOfTopLevelGoal").getFact();
		SyncInteger noOfMessageEvent = (SyncInteger)getBeliefbase().getBelief("noOfMessageEvent").getFact();
		int noOfRoot = noOfTopLevelGoal.read() + noOfMessageEvent.read();
		return noOfRoot;
	}
	
	/**
	 * executing the decision of agent has 
	 * probably want to turn into idle state.
	 */
	private void agentIdle()
	{
		IGoal goal = createGoal("readyToIdle");
		dispatchTopLevelGoal(goal);
	}
	
	@Override
	public void waitForCondition(String condition)
	{
		waitOverHead(WAIT_FOR_CONDITION, condition);
	}
	
	@Override
	public void waitForEver()
	{
		waitOverHead(WAIT_FOR_EVER, null);
	}
	
	@Override 
	public void waitForExternalCondition(IExternalCondition condition)
	{
		waitOverHead(WAIT_FOR_EXT_CONDITION, condition);
	}
	
	@Override
	public Object waitForFactAdded (String beliefset)
	{
		return (Object)waitOverHead(WAIT_FOR_FACT_ADDED, beliefset);
	}
	
	@Override
	public Object waitForFactAddedOrRemoved (String beliefset)
	{
		return (Object)waitOverHead(WAIT_FOR_FACT_ADDED_REMOVED, beliefset);
	}
	
	@Override
	public Object waitForFactChanged (String belief)
	{
		if (belief.equals("agentState"))
			return super.waitForFactChanged(belief);
		else
			return (Object)waitOverHead(WAIT_FOR_FACT_CHANGED, belief);
	}
	
	@Override
	public Object waitForFactRemoved (String beliefset)
	{
		return (Object)waitOverHead(WAIT_FOR_FACT_REMOVED, beliefset);
	}
	
	@Override 
	public void waitForGoal (IGoal goal)
	{
		waitOverHead(WAIT_FOR_GOAL, goal);
	}
	
	@Override
	public IGoal waitForGoal (String type)
	{
		return (IGoal)waitOverHead(WAIT_FOR_GOAL, type);
	}
	
	@Override
	public IInternalEvent waitForInternalEvent (String type)
	{
		return (IInternalEvent)waitOverHead(WAIT_FOR_INTERNAL_EVENT, type);
	}
	
	@Override
	public IMessageEvent waitForMessageEvent (String type)
	{
		return (IMessageEvent)waitOverHead(WAIT_FOR_MESSAGE_EVENT, type);
	}
	
	/**
	 * Helper method to equip Jadex's
	 * waitFor method with a count of waiting points
	 * of the Jadex-Repast System
	 * @param type
	 * @param argument
	 * @return
	 */
	private Object waitOverHead (String type, Object argument)
	{
//		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~HEY YOU" +  getType());
		Object object = null;
		//Wrapped waiting methods with wait's counter process
		isWaiting = true;
		
//		increaseNoOfWait();
		addNoOfWait(1);
		
		SyncInteger noOfWait = (SyncInteger)getBeliefbase().getBelief("noOfWait").getFact();
		Integer noOfRoot = this.calculateNoOfRoot();
		if (noOfRoot.equals(noOfWait.read()))
		{
			agentIdle();
		}
		
		object = waitMethodsChooser(type,argument);
//		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~HEY ME" +  getType());
		isWaiting = false;
		
//		decreaseNoOfWait();
		addNoOfWait(-1);
		
		if (type.equals(WAIT_FOR_MESSAGE_EVENT))
		{
			IMessageEvent me = (IMessageEvent)object;
			IComponentIdentifier sender = (IComponentIdentifier)me.getParameter(SFipa.SENDER).getValue();
			IEnvironmentSpace env = (IEnvironmentSpace)getBeliefbase().getBelief("environment").getFact();
			Map componentAgentIDMap = (Map)env.getProperty("componentAgentIDMap");
			
			if (componentAgentIDMap.containsKey(sender) == false)
			{
				Map params = new HashMap();
				params.put("receiver", this.getComponentIdentifier().toString());
				params.put("sender", ((IComponentIdentifier)me.getParameter(SFipa.SENDER).getValue()).toString());
				this.callEnvironmentActionAndWait("inter_central_action", params);
				
				Boolean isIdle = (Boolean)getBeliefbase().getBelief("isIdle").getFact();
				if (isIdle)
				{
					//In case if it is idle and the messsage is from outerAgent then wake himself up
					StartPlan.BDIServer server = (StartPlan.BDIServer)env.getProperty("server");
					server.setControlStatus(true);
					
					Map parameters = new HashMap();
					IComponentIdentifier[] receivers = new IComponentIdentifier[1];
					receivers[0] = this.getComponentIdentifier();
					parameters.put("receivers", receivers);
					callEnvironmentAction("unidle_agent_action",parameters);
				}
			}
			else
			{
				Boolean isIdle = (Boolean)getBeliefbase().getBelief("isIdle").getFact();
				if (isIdle)
				{
					//In case if it is idle and the messsage is from outerAgent then wake himself up
					StartPlan.BDIServer server = (StartPlan.BDIServer)env.getProperty("server");
					server.setControlStatus(true);
					
					Map parameters = new HashMap();
					IComponentIdentifier[] receivers = new IComponentIdentifier[1];
					receivers[0] = this.getComponentIdentifier();
					parameters.put("receivers", receivers);
					callEnvironmentAction("unidle_agent_action",parameters);
					Object	fact	= getBeliefbase().getBelief("agentState").getFact();
					if (fact.equals(StepManagerPlan.IDLE))
						getBeliefbase().getBelief("agentState").setFact(StepManagerPlan.RUN_NOACTION_NOPERCEPT);
				}
				isIdle = (Boolean)getBeliefbase().getBelief("isIdle").getFact();
				addFloatingMsgNum(-1);
			}
		}
		
		//Trap the process, if agent is idle.
		Boolean isIdle = (Boolean)getBeliefbase().getBelief("isIdle").getFact();
		while (isIdle)
		{
			super.waitForFactChanged("isIdle");
			isIdle = (Boolean)getBeliefbase().getBelief("isIdle").getFact();
		} 

		return object;
	}
	
	/**
	 * Helper Method to choose a waitFor method
	 * @param type
	 * The type of waitFor method
	 * @param argument
	 * the argument of the waitFor method
	 * @return
	 */
	private Object waitMethodsChooser (String type, Object argument)
	{
		Object object = null;
		if (type.equals(WAIT_FOR_CONDITION))
		{
			super.waitForCondition((String)argument);
		}
		else if (type.equals(WAIT_FOR_EVER))
		{
			super.waitForEver();
		}
		else if (type.equals(WAIT_FOR_EXT_CONDITION))
		{
			super.waitForExternalCondition((IExternalCondition)argument);
		}
		else if (type.equals(WAIT_FOR_FACT_ADDED))
		{
			object = super.waitForFactAdded((String)argument);
		}
		else if (type.equals(WAIT_FOR_FACT_ADDED_REMOVED))
		{
			object = super.waitForFactAddedOrRemoved((String)argument);
		}
		else if (type.equals(WAIT_FOR_FACT_CHANGED))
		{
			object = super.waitForFactChanged((String)argument);
		}
		else if (type.equals(WAIT_FOR_FACT_REMOVED))
		{
			object = super.waitForFactRemoved((String)argument);
		}
		else if (type.equals(WAIT_FOR_GOAL))
		{
			if (argument.getClass().equals(String.class))
				object = super.waitForGoal((String)argument);
			else
				super.waitForGoal((IGoal)argument);
		}
		else if (type.equals(WAIT_FOR_INTERNAL_EVENT))
		{
			object = super.waitForInternalEvent((String)argument);
		}
		else if (type.equals(WAIT_FOR_MESSAGE_EVENT))
		{
			object = super.waitForMessageEvent((String)argument);
		}
		return object;
	}
	
	public void printRootWaitStatus ()
	{
		printRootWaitStatus("");
	}
	
	public void printRootWaitStatus (String comment)
	{
		String agentID =(String)((ISpaceObject)getBeliefbase().getBelief("myself").getFact()).getProperty("agentID");
		Integer noOfRoot = calculateNoOfRoot();
		SyncInteger noOfWait = (SyncInteger)getBeliefbase().getBelief("noOfWait").getFact();
		System.out.println("["+agentID+"]"+comment+"..noOfRoot : "+noOfRoot +" & noOfWait : "+noOfWait.read());
	}
}
