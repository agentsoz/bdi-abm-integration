package io.github.agentsoz.abmjadex.super_central;

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


import io.github.agentsoz.abmjadex.central_organizer.CentralServerInterface;
import io.github.agentsoz.abmjadex.central_organizer.ConfirmatorInterface;
import io.github.agentsoz.abmjadex.data_structure.AddressAgentListTuple;
import io.github.agentsoz.abmjadex.data_structure.AddressTable;
import io.github.agentsoz.abmjadex.data_structure.ReceiverSenderTuple;
import io.github.agentsoz.abmjadex.miscellaneous.ABMBDILoggerSetter;
import io.github.agentsoz.abmjadex.super_central.SuperExternalCommunicator.Methods;
import io.github.agentsoz.bdiabm.ABMRemoteServerInterface;
import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.bdiabm.data.AgentStateList;
import jadex.bdi.runtime.IBDIInternalAccess;
import jadex.bdi.runtime.Plan;
import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.commons.future.ISuspendable;
import jadex.commons.transformation.annotations.Classname;
import jadex.extension.envsupport.environment.IEnvironmentSpace;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class SuperStartPlan extends Plan
{
	/**
	 * The Start Procedure of a SuperCentralOrganizer(SCO).
	 * This class has an inner class of the SCO's server.
	 * It would first register the server to RMIRegistry.
	 */
	private static final long serialVersionUID = 3636598041467150108L;
	
	private final static Logger LOGGER = Logger.getLogger(SuperStartPlan.class.getName());
	
	private BDIServer server;
	private String address;
	private int port;
	
	public SuperStartPlan () throws SecurityException, IOException
	{
		super();
		ABMBDILoggerSetter.setup(LOGGER);
		//Check whether the SCO configuration is set to test message mechanism
		//between two agents in different CO. 
		//SCO : SuperCentralOrganizer
		//CO: CentralOrganizer
		IEnvironmentSpace space = (IEnvironmentSpace)getBeliefbase().getBelief("environment").getFact();
		
		//Preparing port to register the server.
		Integer port = (Integer)space.getProperty("port");
		this.port = port;
		
		//Get the user specified name for this SC
		String name = (String)space.getProperty("name");
		
		try
		{
			int minCapacity = (Integer)space.getProperty("minCapacityPerCO");
			int maxCapacity = (Integer)space.getProperty("maxCapacityPerCO");
			AgentGroupper.setMinCapacity(minCapacity);
			AgentGroupper.setMaxCapacity(maxCapacity);
		}
		catch (Exception e)
		{
			LOGGER.severe(e.getMessage());
		}
		
		//Create address of the SuperCentralOrganizer of Jadex
//		String uniqueID = getComponentIdentifier().toString();
//		uniqueID = uniqueID.split("\\.")[1];
		address = "rmi://localhost:"+port+"/SuperCentralJadex/"+name;
		
	}
	
	@Override
	public void body() 
	{
		//Start RMI Registry Only If there is not 
		//(If there already is a Registry on The port 
		//an exception will be thrown and ignored here)
		try 
		{
			java.rmi.registry.LocateRegistry.createRegistry(port);
		} 
		catch (RemoteException e1) 
		{
			//e1.printStackTrace();
			LOGGER.severe(e1.getMessage());
		}
		
		//Rebind SuperCentral's server 
		try 
		{
			server = new BDIServer();
			IEnvironmentSpace space = (IEnvironmentSpace)getBeliefbase().getBelief("environment").getFact();
			space.setProperty("server", server);
			
			Naming.rebind(address, server);
			LOGGER.info("~ SuperCentralOrganizer created and registered :"+address+" ~");
		} 
		catch (RemoteException e) 
		{
			e.printStackTrace();
			LOGGER.severe(e.getMessage());
		}
		catch (MalformedURLException e) 
		{
			e.printStackTrace();
			LOGGER.severe(e.getMessage());
		}
	}

	
	public class BDIServer extends UnicastRemoteObject implements SuperCentralServerInterface, ISuspendable
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 2674143882294125509L;
		
		private boolean isInControl = false;
		private boolean isStarted = false;
		public final static int SUPER_CENTRAL = 1;
		public final static int REPAST = 2;
		public final static int MINE = 3;
		private CentralServerInterface thisServer = this;
		private ABMRemoteServerInterface abmInstance;
		
		public BDIServer () throws RemoteException 
		{
			super();
		}
		
		public boolean isInControl()
		{
			return isInControl;
		}
		
		public void setControlStatusToFalse ()
		{
			isInControl = false;
		}
		
		public ABMRemoteServerInterface getABMInstance ()
		{
			return abmInstance;
		}
		
		public CentralServerInterface getSuperInstance ()
		{
			return this;
		}
		
		@Override
		public boolean start (final AgentDataContainer ADContainer, final AgentStateList agentList, final ABMRemoteServerInterface abmServer) throws RemoteException
		{
			LOGGER.info("SC Started");
			IFuture<Void> future =
			getExternalAccess().scheduleStep(new IComponentStep<Void>()
			{
				@SuppressWarnings({ "rawtypes", "unchecked" })
				@Classname("StartOfSCO")
				public IFuture<Void> execute(IInternalAccess ia)
				{
					if (isStarted == false)
					{
						isStarted = true;
						isInControl = true;
						abmInstance = abmServer;
						IBDIInternalAccess	scope	= (IBDIInternalAccess)ia;
						IEnvironmentSpace space = (IEnvironmentSpace)scope.getBeliefbase().getBelief("environment").getFact();
						
						Map parameters = new HashMap();
						parameters.put("adContainer", ADContainer);
						parameters.put("agentList", agentList);	
						parameters.put("abmServer", abmServer);
						
//						SuperStartAction action = new SuperStartAction();
//						isStarted = (Boolean) action.perform(parameters, space);
						space.performSpaceAction("super_start_action", parameters,
								new IResultListener() {

									@Override
									public void exceptionOccurred(Exception arg0) {
										isInControl = false;
										abmInstance = null;
									}

									@Override
									public void resultAvailable(Object arg0) {
										// TODO Auto-generated method stub
										
									}

								}
								);
					}
					return IFuture.DONE;
				}
			});	
			//Block This Thread Until all agents are created
			future.get(this);
			return isStarted;
		}
		
		
		public void takeControl (final AgentDataContainer agentDataContainer) throws RemoteException
		{
			//actionPerceptContainer.println();
			LOGGER.info("----SUPER BDI Server Take Control----");
//			terminateProgram();
			getExternalAccess().scheduleStep(new IComponentStep<Void>()
			{
				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Classname("takeControl")
				public IFuture<Void> execute(IInternalAccess ia)
				{
					isInControl = true;
					IBDIInternalAccess	scope	= (IBDIInternalAccess)ia;
					IEnvironmentSpace space = (IEnvironmentSpace)scope.getBeliefbase().getBelief("environment").getFact();
					
					Map parameters = new HashMap();
					parameters.put("agentDataContainer", agentDataContainer);
					
//					SuperTakeControlAction action = new SuperTakeControlAction();
//					action.perform(parameters, space);
					space.performSpaceAction("super_take_control_action", parameters, null);
					
					return IFuture.DONE;
				}
			});
		}
		
		

		@Override
		public void createAgents (final String[] agentIDs) throws RemoteException 
		{
			getExternalAccess().scheduleStep(new IComponentStep<Void>()
			{
				@SuppressWarnings("unchecked")
				@Classname("createAgent")
				public IFuture<Void> execute(IInternalAccess ia)
				{
					IBDIInternalAccess	scope	= (IBDIInternalAccess)ia;
					IEnvironmentSpace space = (IEnvironmentSpace)scope.getBeliefbase().getBelief("environment").getFact();
					
					@SuppressWarnings("rawtypes")
					Map parameters = new HashMap();
					parameters.put("agentIDs", agentIDs);
					
//					SuperCreateAgentAction action = new SuperCreateAgentAction();
//					action.perform(parameters, space);
					
					space.performSpaceAction("super_create_agent_action", parameters, null);
					return IFuture.DONE;
				}
			});
		}


		@Override
		public void administerRegistration(final boolean registrationAction, final CentralServerInterface remoteCO) 
		{
			getExternalAccess().scheduleStep(new IComponentStep<Void>()
			{
				@Classname("Registration")
				public IFuture<Void> execute(IInternalAccess ia)
				{
					IBDIInternalAccess	scope	= (IBDIInternalAccess)ia;
					IEnvironmentSpace space = (IEnvironmentSpace)scope.getBeliefbase().getBelief("environment").getFact();
					
					AddressTable addressTable = (AddressTable)space.getProperty("addressTable");
					
					if (registrationAction == REGISTER)
					{
						boolean isSuccessful = false;
						if (isStarted == false)
						{
							isSuccessful = addressTable.add(new AddressAgentListTuple(remoteCO));
						}
						LOGGER.info("Try to register : " + remoteCO +" : isSuccess-"+isSuccessful);
						try 
						{
							((ConfirmatorInterface)remoteCO).confirm(thisServer, Events.REGISTER, isSuccessful,null);
						} catch (RemoteException e) {
							e.printStackTrace();
							LOGGER.severe(e.getMessage());
						}
					}
					//TODO : Implements the unregister procedure	
					
					return IFuture.DONE;
				}
			});
		}

		@Override
		public void confirm(final CentralServerInterface remoteCO, final Events eventType,
				boolean isSuccessful, final Object[] parameters) throws RemoteException 
		{
			getExternalAccess().scheduleStep(new IComponentStep<Void>()
			{
				@SuppressWarnings({ "rawtypes", "unchecked" })
				@Classname("Confirm")
				public IFuture<Void> execute(IInternalAccess ia)
				{
					IBDIInternalAccess	scope	= (IBDIInternalAccess)ia;
					IEnvironmentSpace space = (IEnvironmentSpace)scope.getBeliefbase().getBelief("environment").getFact();
					
					AddressTable addressTable = (AddressTable)space.getProperty("addressTable");
					
					if (eventType.equals(Events.RETRIEVE))
					{
						int noOfAppToConfirm = (Integer)space.getProperty("noOfAppToConfirm");
						AddressAgentListTuple x = addressTable.get(remoteCO);
						x.setAgentDataContainer((AgentDataContainer)parameters[0]);
						noOfAppToConfirm--;
						space.setProperty("addressTable", addressTable);
						space.setProperty("noOfAppToConfirm", noOfAppToConfirm);
						
						LOGGER.info("DATA RECEIVED FROM CO, no of confirmation left :"+noOfAppToConfirm);
						if (noOfAppToConfirm == 0)
						{
							Map parameters = new HashMap();
							parameters.put("addressTable", addressTable);
							
//							SwitchStepAction action = new SwitchStepAction();
//							action.perform(parameters, space);
							space.performSpaceAction("switch_step_action", parameters, null);
						}
					}
					return IFuture.DONE;
				}
			});		
		}

		@Override
		public void informIdleState(final CentralServerInterface app) throws RemoteException 
		{
			final ConfirmatorInterface thisConfirmator = this;
			getExternalAccess().scheduleStep(new IComponentStep<Void>()
			{
				@SuppressWarnings("rawtypes")
				@Classname("informIdleState")
				public IFuture<Void> execute(IInternalAccess ia)
				{
					IBDIInternalAccess	scope	= (IBDIInternalAccess)ia;
					IEnvironmentSpace space = (IEnvironmentSpace)scope.getBeliefbase().getBelief("environment").getFact();
					
					AddressTable addressTable = (AddressTable)space.getProperty("addressTable");
					
					AddressAgentListTuple appData = addressTable.get(app);
					if (appData != null)
					{
						appData.setIdleState(true);
					}
					
					boolean allIdle = true;
					for (int i = 0; i < addressTable.size(); i++)
					{
						AddressAgentListTuple x = addressTable.get(i);
						if(x.isIdle() == false)
						{
							allIdle = false;
						}
					}
					
					if (allIdle == true)
					{				
						ArrayList agentMessageList = (ArrayList)space.getProperty("agentMessageList");

						if(agentMessageList.size() == 0)
						{
							LOGGER.info("--Retriving data from CO--");
							isInControl = false;
							//Ask each Central Organizer to send its data 
							for (int i = 0; i < addressTable.size(); i++)
							{
								CentralServerInterface remoteApp = addressTable.get(i).getRemoteCO();
								Object[] params = new Object[1];
								params[0] = thisConfirmator;
								SuperExternalCommunicator extComm 
										= new SuperExternalCommunicator(null, remoteApp, Methods.RETRIEVE, params);
								extComm.start();
							}	
						}
					}
					
					return IFuture.DONE;
				}
			});
		}

		@Override
		public void interCentralSent(final Object params, final CentralServerInterface callerInstance) throws RemoteException 
		{	
			getExternalAccess().scheduleStep(new IComponentStep<Void>()
			{
				@SuppressWarnings({ "rawtypes", "unchecked" })
				@Classname("InterCentral")
				public IFuture<Void> execute(IInternalAccess ia)
				{
					IBDIInternalAccess	scope	= (IBDIInternalAccess)ia;
					IEnvironmentSpace space = (IEnvironmentSpace)scope.getBeliefbase().getBelief("environment").getFact();
					if (params.getClass().equals(ReceiverSenderTuple.class))
					{
						//This branch means the info comes from receiver of messages
						AddressTable addressTable = (AddressTable)space.getProperty("addressTable");
						AddressAgentListTuple addressTuple = addressTable.get(callerInstance);
						if (addressTuple.isIdle())
						{
							addressTuple.setIdleState(false);
						}
						ArrayList agentMessageList = (ArrayList)space.getProperty("agentMessageList");
						agentMessageList.remove(params);
						space.setProperty("agentMessageList",agentMessageList);
					}
					else
					{
						//This branch means the info comes from sender of messages
						//The info will be saved in the SuperCentral agentMessageList
						Map parameters = (Map)params;
						Object[] receivers = (Object[])parameters.get("receivers");
						String message = (String)parameters.get("sender");
						
						ArrayList agentMessageList = (ArrayList)space.getProperty("agentMessageList");
						for (int i = 0; i < receivers.length; i++)
						{
							agentMessageList.add(new ReceiverSenderTuple((String)receivers[i], message));
						}
						space.setProperty("agentMessageList",agentMessageList);
					}
					
					return IFuture.DONE;
				}
			});	
		}

		@Override
		public Object getMonitor() 
		{
			return null;
		}

		@Override
		public Object[] retrieveData(ConfirmatorInterface confirmator)
				throws RemoteException 
		{
			return null;
		}

		@Override
		public void killAgents(final String[] agentIDs) throws RemoteException 
		{
			IFuture<Void> fut = 
			getExternalAccess().scheduleStep(new IComponentStep<Void>()
			{
				@SuppressWarnings({ "rawtypes", "unchecked" })
				@Classname("InterCentral")
				public IFuture<Void> execute(IInternalAccess ia)
				{
					IBDIInternalAccess	scope	= (IBDIInternalAccess)ia;
					IEnvironmentSpace space = (IEnvironmentSpace)scope.getBeliefbase().getBelief("environment").getFact();
			
					Map parameters = new HashMap();
					parameters.put("agentIDs", agentIDs);
					
//					SuperKillAgentsAction action = new SuperKillAgentsAction();
//					action.perform(parameters, space);
					space.performSpaceAction("super_kill_action", parameters, null);
					return IFuture.DONE;
				}
			});
			fut.get(this);
		}

		@Override
		public void terminateProgram() throws RemoteException {
			LOGGER.info("Start Jadex Application Termination . . . ");
			IFuture<Void> fut = 
				getExternalAccess().scheduleStep(new IComponentStep<Void>()
				{
					@Classname("InterCentral")
					public IFuture<Void> execute(IInternalAccess ia)
					{
						IBDIInternalAccess	scope	= (IBDIInternalAccess)ia;
						IEnvironmentSpace space = (IEnvironmentSpace)scope.getBeliefbase().getBelief("environment").getFact();
					
						space.performSpaceAction("terminate_action", null, null);
							
						return IFuture.DONE;
					}
				});
			fut.get(this);
			
			try 
			{
				//UnRegister CentralOrganizer in rmiregistry
				Naming.unbind(address);
				LOGGER.info("Central Server unbinding :" + address);
			} 
			catch (RemoteException e) 
			{
				e.printStackTrace();
				LOGGER.severe(e.getMessage());
			}
			catch (MalformedURLException e) 
			{
				e.printStackTrace();
				LOGGER.severe(e.getMessage());
			} catch (NotBoundException e) {
				e.printStackTrace();
				LOGGER.severe(e.getMessage());
			}
			
			LOGGER.info("~ Jadex App Terminated ~");
			System.exit(0);
			
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
					LOGGER.severe(e.getMessage());
				}
			}
		}
	}
}
