package io.github.agentsoz.abmjadex.central_organizer;

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



import io.github.agentsoz.abmjadex.agent.StepManagerPlan;
import io.github.agentsoz.bdiabm.data.AgentStateList;
import jadex.bdi.runtime.IBDIExternalAccess;
import jadex.bdi.runtime.IBDIInternalAccess;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentStep;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.cms.IComponentDescription;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.commons.SimplePropertyObject;
import jadex.commons.future.IFuture;
import jadex.commons.future.ISuspendable;
import jadex.commons.transformation.annotations.Classname;
import jadex.extension.envsupport.environment.IEnvironmentSpace;
import jadex.extension.envsupport.environment.ISpaceAction;
import jadex.extension.envsupport.environment.ISpaceObject;

import java.util.ArrayList;
import java.util.Map;

/**
 *  Action for starting interapp application
 */
public class UnidleAgentAction extends SimplePropertyObject implements ISpaceAction, ISuspendable
{

	/**
	 * Performs the action.
	 * @param parameters parameters for the action
	 * @param space the environment space
	 * @return action return value
	 */
	//private String agentID = null;
	@SuppressWarnings("rawtypes")
	public  Object perform(Map parameters, IEnvironmentSpace space)
	{	
		ArrayList<ISpaceObject> objectsToBeUnidled = new ArrayList<ISpaceObject>();
		
		//Find object in environment for each receiver
		Object[] receivers = (Object[])parameters.get("receivers");
		ArrayList agentTypeList = (ArrayList)space.getProperty("agentTypeList");
		for (Object oReceiver : receivers)
		{
			IComponentIdentifier receiver = (IComponentIdentifier)oReceiver;
			boolean isFound = false;
			int i = 0;
			while (!isFound && i < agentTypeList.size())
			{
				String agentType = (String)agentTypeList.get(i);
				ISpaceObject[] objects = space.getSpaceObjectsByType(agentType);
				int j = 0;
				while (!isFound && j < objects.length)
				{
					if(objects[j].getProperty("componentID").equals(receiver))
					{
						isFound = true;
						//agentID = (String)objects[j].getProperty("agentID");
						objectsToBeUnidled.add(objects[j]);	
					}
					j++;
				}
				i++;
			}
		}
		
		AgentStateList agentStateList = (AgentStateList)space.getProperty("agentStateList");
		
		//Unidle each receiver
		for (ISpaceObject object : objectsToBeUnidled)
		{
			String agentID = (String)object.getProperty("agentID");
			if (agentStateList.isIdle(agentID))
			{
				agentStateList.setState(agentID, false);
				//unidleWait(space, (IComponentDescription) object.getProperty(ISpaceObject.PROPERTY_OWNER));
				//unidleWait(space, (IComponentDescription) object.getProperty(ISpaceObject.PROPERTY_OWNER));
			}	
		}
		return null;
	}
	
	/**
	 * Unidle an agent
	 * @param space
	 * The EnvironmentSpace where the agent is
	 * @param agent
	 * The component description of the agent
	 */
//	@SuppressWarnings({ "unchecked", "rawtypes"})
//	private void unidle (final IEnvironmentSpace space, final IComponentDescription agent)
//	{
//		SServiceProvider.getService(space.getExternalAccess().getServiceProvider(), IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM)
//		.addResultListener(new IResultListener()
//		{
//			public void exceptionOccurred(Exception exception)
//			{
//				exception.printStackTrace();
//			}
//			public void resultAvailable(Object result)
//			{
//				IFuture fut = ((IComponentManagementService)result).getExternalAccess(agent.getName());
//				fut.addResultListener(new IResultListener()
//				{
//					public void exceptionOccurred(Exception exception)
//					{
//					}
//					public void resultAvailable(Object result)
//					{
//						final IBDIExternalAccess exta = (IBDIExternalAccess)result;
//						exta.scheduleStep(new IComponentStep<Void>()
//						{
//							@Classname("set")
//							public IFuture<Void> execute(IInternalAccess ia)
//							{
//								IBDIInternalAccess	scope	= (IBDIInternalAccess)ia;
//								Object	fact	= scope.getBeliefbase().getBelief("agentState").getFact();
//								if (fact.equals(StepManagerPlan.IDLE))
//									scope.getBeliefbase().getBelief("agentState").setFact(StepManagerPlan.RUN_NOACTION_NOPERCEPT);
//								return IFuture.DONE;
//							}
//						});	
//				}
//			});
//		}
//		});
//	}

	/**
	 * Unidle an agent
	 * @param space
	 * The EnvironmentSpace where the agent is
	 * @param agent
	 * The component description of the agent
	 */
	@SuppressWarnings("unused")
	private void unidleWait(final IEnvironmentSpace space, final IComponentDescription agent)
	{
		boolean noException = false;
		while (noException == false)
		{
		try 
		{
		IFuture<IComponentManagementService> futService =
				SServiceProvider.getService(space.getExternalAccess().getServiceProvider(), IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM);
		IComponentManagementService resultService = futService.get();

		IFuture<IExternalAccess> fut = resultService.getExternalAccess(agent.getName());
		
		IExternalAccess extaa = fut.get();
		
		IBDIExternalAccess exta = (IBDIExternalAccess)extaa;

		IFuture<Void> futTa = exta.scheduleStep(new IComponentStep<Void>()
		{
			@Classname("set")
			public IFuture<Void> execute(IInternalAccess ia)
			{
				IBDIInternalAccess	scope	= (IBDIInternalAccess)ia;
				Object	fact	= scope.getBeliefbase().getBelief("agentState").getFact();
				if (fact.equals(StepManagerPlan.IDLE))
					scope.getBeliefbase().getBelief("agentState").setFact(StepManagerPlan.RUN_NOACTION_NOPERCEPT);
				return IFuture.DONE;
			}
		});
		
		futTa.get(this);
		noException = true;
		}
		catch (Exception e)
		{
			System.out.println("IN UNINDLE AGENT");
			e.printStackTrace();
		}
	}
	//*System.out.println("["+agentID+"]****************UNIDLE THIS GUYS");
	}
	
	@Override
	public Object getMonitor() 
	{
		return null;
	}

	@Override
	public void resume(IFuture<?> arg0) {
		synchronized (this)
		{
			this.notify();
		}
	}

	@Override
	public void suspend(IFuture<?> arg0, long arg1) {
		synchronized (this)
		{
			try 
			{
				this.wait();
			} catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
		}
	}
	
}



