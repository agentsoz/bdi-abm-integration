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



import io.github.agentsoz.abmjadex.miscellaneous.ABMBDILoggerSetter;
import io.github.agentsoz.bdiabm.data.AgentState;
import io.github.agentsoz.bdiabm.data.AgentStateList;
import jadex.bdi.runtime.IBDIExternalAccess;
import jadex.bdi.runtime.IBDIInternalAccess;
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
import java.util.logging.Logger;

/**
 *  Action for starting interapp application
 */
public class KillAgentsAction extends SimplePropertyObject implements ISpaceAction, ISuspendable
{
	private final static Logger LOGGER = Logger.getLogger(KillAgentsAction.class.getName());
//	private Object lock = new Object();
	/**
	 * Performs the action.
	 * @param parameters parameters for the action
	 * @param space the environment space
	 * @return action return value
	 */
	@SuppressWarnings("rawtypes")
	public Object perform(Map parameters, IEnvironmentSpace space)
	{	
		ABMBDILoggerSetter.setup(LOGGER);
		ArrayList<ISpaceObject> objectsToBeKilled = new ArrayList<ISpaceObject>();
		
		String[] agentIDs= (String[])parameters.get("agentIDs");
		ArrayList agentTypeList = (ArrayList)space.getProperty("agentTypeList");
		
		//Find object in environment for each agent To be Killed	
		for (String agentID : agentIDs)
		{
			boolean isFound = false;
			int i = 0;
			while (!isFound && i < agentTypeList.size())
			{
				String agentType = (String)agentTypeList.get(i);
				ISpaceObject[] objects = space.getSpaceObjectsByType(agentType);
				int j = 0;
				while (!isFound && j < objects.length)
				{
					if(objects[j].getProperty("agentID").equals(agentID))
					{
						isFound = true;
						objectsToBeKilled.add(objects[j]);
					}
					j++;
				}
				i++;
			}
		}
		try
		{
			for (ISpaceObject object : objectsToBeKilled)
			{
				String agentID = (String)object.getProperty("agentID");
				killAgentWait(space, (IComponentDescription) object.getProperty(ISpaceObject.PROPERTY_OWNER), object, agentID);			
			}
		} catch (Exception e)
		{
			LOGGER.severe(e.getMessage());
		}
		return null;
	}
	
//	/**
//	 * kill agents
//	 * @param space
//	 * The EnvironmentSpace where the agent is
//	 * @param agent
//	 * The component description of the agent
//	 */
//	@SuppressWarnings({ "unchecked", "rawtypes" })
//	private void killAgent (final IEnvironmentSpace space, final IComponentDescription agent, final ISpaceObject objectToDestroy, final String agentIDtoDestroy)
//	{
//		SServiceProvider.getService(space.getExternalAccess().getServiceProvider(), IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM).addResultListener(new IResultListener()
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
//								scope.killAgent();
//								AgentStateList asList 
//									=(AgentStateList)space.getProperty("agentStateList");
//								asList.remove(new AgentState(agentIDtoDestroy));
//								//Strangely this is not working
//								//space.destroySpaceObject(objectToDestroy);
//								return IFuture.DONE;
//							}
//						});	
//					}
//				});
//			}
//		});	
//	}

	/**
	 * kill agents
	 * @param space
	 * The EnvironmentSpace where the agent is
	 * @param agent
	 * The component description of the agent
	 */
	private void killAgentWait (final IEnvironmentSpace space, final IComponentDescription agent, final ISpaceObject objectToDestroy, final String agentIDtoDestroy)
	{
		IFuture<IComponentManagementService> futService =
				SServiceProvider.getService(space.getExternalAccess().getServiceProvider(), IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM);
		IComponentManagementService resultService = futService.get();
		
		IFuture<IExternalAccess> fut = resultService.getExternalAccess(agent.getName());
		IExternalAccess extaa = fut.get();
		
		IBDIExternalAccess exta = (IBDIExternalAccess)extaa;
		IFuture<Void> futTa = exta.scheduleStep(new IComponentStep<Void>()
		{
			@Classname("kill")
			public IFuture<Void> execute(IInternalAccess ia)
			{
				IBDIInternalAccess	scope	= (IBDIInternalAccess)ia;
				scope.killAgent();
				AgentStateList asList 
					=(AgentStateList)space.getProperty("agentStateList");
				asList.remove(new AgentState(agentIDtoDestroy));
				//Strangely this is not working
				//space.destroySpaceObject(objectToDestroy);
				return IFuture.DONE;
			}
		});

		futTa.get(this);
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
			this.notifyAll();
		}
	}

	@Override
	public void suspend(IFuture<?> arg0, long arg1) {
		synchronized (this)
		{
			try 
			{
				this.wait();
			} 
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
		}
	}
}




