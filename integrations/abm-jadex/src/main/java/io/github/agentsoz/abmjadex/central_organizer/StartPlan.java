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
import io.github.agentsoz.abmjadex.central_organizer.ExternalCommunicator.Methods;
import io.github.agentsoz.abmjadex.miscellaneous.ABMBDILoggerSetter;
import io.github.agentsoz.abmjadex.super_central.SuperCentralServerInterface;
import io.github.agentsoz.bdiabm.ABMRemoteServerInterface;
import io.github.agentsoz.bdiabm.BDIRemoteServerInterface;
import io.github.agentsoz.bdiabm.data.ActionContainer;
import io.github.agentsoz.bdiabm.data.ActionPerceptContainer;
import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.bdiabm.data.AgentState;
import io.github.agentsoz.bdiabm.data.AgentStateList;
import io.github.agentsoz.bdiabm.data.PerceptContainer;
import jadex.bdi.runtime.IBDIExternalAccess;
import jadex.bdi.runtime.IBDIInternalAccess;
import jadex.bdi.runtime.Plan;
import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.cms.IComponentDescription;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.commons.future.ISuspendable;
import jadex.commons.transformation.annotations.Classname;
import jadex.extension.envsupport.environment.IEnvironmentSpace;
import jadex.extension.envsupport.environment.ISpaceObject;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class StartPlan extends Plan 
{
	/** 
	 *This is the plan to start a CentralOrganizer(CO).
	 *This holds an instance of CO's server.
	 *It will create and also register the server to RMIRegistry.
	 */
	private static final long serialVersionUID = -7755762983385704681L;
	
	private final static Logger LOGGER = Logger.getLogger(StartPlan.class.getName());
	
	BDIServer server;
	IEnvironmentSpace envSpace;
	private String superCentralAddress;
	private String address;
	private int co_port = 1099;
	private int sc_port = 1099;
	private String sc_host = "localhost";
	private String sc_name = "supercentral";
	
	public StartPlan ()
	{
		super();
		ABMBDILoggerSetter.setup(LOGGER);
		//Get the super central organizer address
		//all of this data are written in properties file, and parsed into xml by
		//CentralOrganizerXMLCreator
		IEnvironmentSpace space = (IEnvironmentSpace)getBeliefbase().getBelief("environment").getFact();
		co_port = (Integer) space.getProperty("co_port");
		sc_port = (Integer) space.getProperty("sc_port");
		sc_host = (String) space.getProperty("sc_host");
		sc_name = (String) space.getProperty("sc_name");
		superCentralAddress = "rmi://"+sc_host+":"+sc_port+"/SuperCentralJadex/"+sc_name;
		
		//Creation of this CO address
		String uniqueID = getComponentIdentifier().toString();
		uniqueID = uniqueID.split("\\.")[1];
		address = "rmi://localhost:"+co_port+"/CentralJadex/"+uniqueID;
	}
	
	@Override
	public void body() 
	{
		try 
		{
			server = new BDIServer();
			IEnvironmentSpace space = (IEnvironmentSpace)getBeliefbase().getBelief("environment").getFact();
			space.setProperty("server", server);
			
			//Register CentralOrganizer in rmiregistry
			Naming.rebind(address, server);
			LOGGER.info("Central Server created and registered :" + address);
			
			//Register CentralOrganizer to SuperCentral
			server.registerToSuperCentral();
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

	
	public class BDIServer extends UnicastRemoteObject implements CentralServerInterface, Runnable, ISuspendable
	{
		/**
		 * This is the server instance of a CO.
		 * It is the gate for external entities to communicate with a CO.
		 */
		private static final long serialVersionUID = -4213412392347031706L;
		
		private boolean isInControl = false;
		private boolean isRegistered = false;
		private String thisAddress;
		public final static int SUPER_CENTRAL = 1;
		public final static int REPAST = 2;
		public final static int MINE = 3;
		private ABMRemoteServerInterface abmServer;
		private BDIRemoteServerInterface thisServer = this;
		private SuperCentralServerInterface scServer = null;
		
		public BDIServer () throws RemoteException 
		{
			super();
			thisAddress = address;
		}
		
		@Override
		public boolean equals (Object o)
		{
			boolean isEquals = false;
			if (o.getClass().equals(this.getClass()))
			{
				BDIServer x = (BDIServer)o;
				if (x.thisAddress.equals(this.thisAddress))
					isEquals = true;
			}
			return isEquals;
		}
		
		public boolean isInControl()
		{
			return isInControl;
		}
		
		public void setControlStatus (final boolean isInControlState)
		{
			getExternalAccess().scheduleStep(new IComponentStep<Void>()
			{
				@Classname("StartCentral")
				public IFuture<Void> execute(IInternalAccess ia)
				{
					isInControl = isInControlState;
					return IFuture.DONE;
				}
			});
		}
		
//		public String getAddress (int type)
//		{
//			String value = null;
//			if (type == SUPER_CENTRAL)
//			{
//				value = superCentralAddress;
//			}
//			else if(type == REPAST)
//			{
//				value = repastAddress;
//			}
//			else if(type == MINE)
//			{
//				value = address;
//			}
//			return value;
//		}
		
		public boolean start (final AgentDataContainer ADContainer, final AgentStateList agentList, final ABMRemoteServerInterface abmServerInstance) throws RemoteException
		{
			LOGGER.info("CO started");
			getExternalAccess().scheduleStep(new IComponentStep<Void>()
			{
				@Classname("StartCentral")
				public IFuture<Void> execute(IInternalAccess ia)
				{
					isInControl = true;
					IBDIInternalAccess	scope	= (IBDIInternalAccess)ia;
					IEnvironmentSpace space = (IEnvironmentSpace)scope.getBeliefbase().getBelief("environment").getFact();
//					//Set up address info
					setAbmServer(abmServerInstance);
//					repastAddress = abmAddress;
					
					//copy the BDIAPContainer with the data from ABM APContainer
					space.setProperty("agentDataContainer", ADContainer);
					
					//Set the space property up
					space.setProperty("agentStateList", agentList);
					createBDICorrespondingAgent(space, "agentStateList");
					//If there are no agents being born, then we need to check by ourself this fact.
					checkAllIdle(space);
					return IFuture.DONE;
				}
			});
			return true;
		}
		
		/**
		 * Procedures to create the BDI agent
		 * @param space
		 */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		private void createBDICorrespondingAgent(IEnvironmentSpace space, String source)
		{
			AgentDataContainer ADContainer = (AgentDataContainer)space.getProperty("agentDataContainer");
			AgentStateList agentList = (AgentStateList)space.getProperty(source);
			ArrayList agentTypeList = (ArrayList)space.getProperty("agentTypeList");
			//For each agent listed from ABM create a corresponding agent in BDI, with initial percept and action
			for (AgentState agentState: agentList)
			{
				//Extracting the agent type
				String agentID = agentState.getID();
				String agentType = ((agentID.split("@"))[0]).split("\\.")[1];
				
				//Collecting agentType data
				if (!agentTypeList.contains(agentType))
				{
					agentTypeList.add(agentType);
				}
				
				//Creating and initializing the corresponding BDI agent
				ISpaceObject agent = space.createSpaceObject(agentType, null, null);
				agent.setProperty("agentID", agentID);	
				agent.setProperty("actionContainer", ADContainer.getOrCreate(agentID).getActionContainer());
				agent.setProperty("perceptContainer", ADContainer.getOrCreate(agentID).getPerceptContainer());
			}
		}
		
		public void takeControl (final AgentDataContainer agentDataContainer) throws RemoteException
		{
			//actionPerceptContainer.println();
			LOGGER.info("----BDI Server Take Control----");
			getExternalAccess().scheduleStep(new IComponentStep<Void>()
			{
				@SuppressWarnings("rawtypes")
				@Classname("translate")
				public IFuture<Void> execute(IInternalAccess ia)
				{
					isInControl = true;
					IBDIInternalAccess	scope	= (IBDIInternalAccess)ia;
					IEnvironmentSpace space = (IEnvironmentSpace)scope.getBeliefbase().getBelief("environment").getFact();
					//Update BDI APContainer
					space.setProperty("agentDataContainer", agentDataContainer);
					
					/*
					 * Create New Agent
					 * First : Update the central agentStateList, as if the agent being born 
					 * is directly going to idle, then the availability of it is being registered
					 * to the central data before that happens is crucial
					 */
					AgentStateList agentToBeBornList = (AgentStateList)space.getProperty("agentToBeBornList");
					AgentStateList centralAgentStateList = (AgentStateList)space.getProperty("agentStateList");
					
					for (AgentState agentState : agentToBeBornList)
					{
						centralAgentStateList.add(agentState);
					}
					space.setProperty("agentStateList", centralAgentStateList);
					
					createBDICorrespondingAgent(space,"agentToBeBornList");
					
					//Per Object
					ArrayList agentTypeList = (ArrayList)space.getProperty("agentTypeList");
					for (Object agentType : agentTypeList)
					{
						ISpaceObject[] objects = space.getSpaceObjectsByType((String)agentType);
						for(ISpaceObject object : objects)
						{
							String agentID = (String)object.getProperty("agentID");
							
							//Only those who is not being born in this step that gets to be examined
							if (!agentToBeBornList.contains(new AgentState(agentID)))
							{
								ActionPerceptContainer actionPerceptContainer = agentDataContainer.get(agentID);
								//Compare new and old value before updating value
								ActionContainer newActionContainer = actionPerceptContainer.getActionContainer();
								ActionContainer oldActionContainer = (ActionContainer)object.getProperty("actionContainer");
								boolean isActionEqual = isActionContainerEqual(newActionContainer, oldActionContainer);
								
								PerceptContainer newPerceptContainer = actionPerceptContainer.getPerceptContainer();
								boolean isNewPerceptEmpty = newPerceptContainer.isEmpty();
								//Update to new value
								object.setProperty("actionContainer", newActionContainer);
								object.setProperty("perceptContainer", newPerceptContainer);
								
	
								//Decide to wake or let agent still idle, using the previous comparison
								
								int agentRunningState;
								if (isActionEqual && isNewPerceptEmpty)
								{
									//don't wake the agent
								}
								else
								{
									if(isActionEqual && !isNewPerceptEmpty)
									{
										agentRunningState = StepManagerPlan.RUN_NOACTION_PERCEPT;
										
									}
									else if (!isActionEqual && !isNewPerceptEmpty)
									{
										agentRunningState = StepManagerPlan.RUN_ACTION_PERCEPT;
									}
									else 
									{
										agentRunningState = StepManagerPlan.RUN_ACTION_NOPERCEPT;
									}
									//Update central state into awake
									AgentStateList agentStateList = (AgentStateList)space.getProperty("agentStateList");
									//An Exception could be thrown because the agentID is not
									//exist anymore in the list but still exist as an object in Space.
									//The reason is the destroySpaceObject(Obj) function not working.
									try {
									agentStateList.setState(agentID, false);
									wakeAgent(space, (IComponentDescription) object.getProperty(ISpaceObject.PROPERTY_OWNER), agentRunningState);
									} catch (Exception e) {};
								}
							}
						}
					}
					//Emptied up the request of new agent creation from Repast's latest turn of control.		
					agentToBeBornList.clear();
					space.setProperty("agentToBeBornList", agentToBeBornList);
					
					checkAllIdle(space);
					return IFuture.DONE;
				}
			});
		}
		
		/**
		 * Whether actionContainer a and b are equals
		 * @param a
		 * @param b
		 * @return
		 */
		private boolean isActionContainerEqual(ActionContainer a, ActionContainer b)
		{
			boolean isEqual = true;
			if(a.size() == b.size())
			{
				Set<String> aIDSet = a.actionIDSet();
				Set<String> bIDSet = b.actionIDSet();
				for(String aID : aIDSet)
				{
					boolean isFound = false;
					for(String bID : bIDSet)
					{
						boolean isIDEqual = aID.equals(bID);
						if(isIDEqual)
						{
							boolean isStateEqual = (a.get(aID).getState() == (b.get(bID).getState()));
							if (isStateEqual)
							{
								isFound = true;
								break;
							}
						}
					}
					if (!isFound)
					{
						isEqual = false;
						break;
					}
				}
			}
			return isEqual;
		}
		
		/**
		 * The procedures to unidled an Agent
		 * @param space
		 * @param agent
		 * @param agentState
		 * The state of when the agent awaken
		 */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		private void wakeAgent (final IEnvironmentSpace space, final IComponentDescription agent, final int agentState)
		{
			SServiceProvider.getService(space.getExternalAccess().getServiceProvider(), IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM)
			.addResultListener(new IResultListener()
			{
				public void exceptionOccurred(Exception exception)
				{
					exception.printStackTrace();
				}
				public void resultAvailable(Object result)
				{
					IFuture fut = ((IComponentManagementService)result).getExternalAccess(agent.getName());
					fut.addResultListener(new IResultListener()
					{
						public void exceptionOccurred(Exception exception)
						{
						}
						public void resultAvailable(Object result)
						{
							final IBDIExternalAccess exta = (IBDIExternalAccess)result;
							exta.scheduleStep(new IComponentStep<Void>()
							{
								@Classname("set")
								public IFuture<Void> execute(IInternalAccess ia)
								{
									IBDIInternalAccess	scope	= (IBDIInternalAccess)ia;
									Object	fact	= scope.getBeliefbase().getBelief("agentState").getFact();
									if (fact.equals(StepManagerPlan.IDLE))
									{
										scope.getBeliefbase().getBelief("agentState").setFact(agentState);
									}
									return IFuture.DONE;
								}
							});	
					}
				});
			}
			});
		}

		@Override
		public void createAgents (final String[] agentIDs) throws RemoteException 
		{
			getExternalAccess().scheduleStep(new IComponentStep<Void>()
			{
				@Classname("createAgents")
				public IFuture<Void> execute(IInternalAccess ia)
				{
					IBDIInternalAccess	scope	= (IBDIInternalAccess)ia;
					IEnvironmentSpace space = (IEnvironmentSpace)scope.getBeliefbase().getBelief("environment").getFact();
					
					AgentStateList agentToBeBornList = (AgentStateList)space.getProperty("agentToBeBornList");
					for (int i = 0; i < agentIDs.length; i++)
					{
						AgentState newAgent = new AgentState(agentIDs[i]);
						agentToBeBornList.add(newAgent);
					}
					space.setProperty("agentToBeBornList", agentToBeBornList);
					
					return IFuture.DONE;
				}
			});
		}
		
		/**
		 * Procedures to check whether all of
		 * the agent in the EnvironmentSpace are idle
		 * @param space
		 */
		private void checkAllIdle (IEnvironmentSpace space)
		{
			/*
			 * Server need to monitor its own condition whether in Control or not.
			 * This is to prevent if there is a race condition and resulted in multiple calls
			 * to Repast's server.
			 * (This flag method works as long as there are no interruption point, we
			 * utilize Jadex's schedule. This is also works while the racing we are prevent from
			 * shall all be in the same platform / no networks delay effect..)
			 */

			if (isInControl)
			{
				AgentStateList agentStateList = (AgentStateList)space.getProperty("agentStateList");
				boolean allIdle = true;
				for (AgentState agentState : agentStateList)
				{
					if (!agentState.isIdle())
					{
						allIdle = false;
					}
				}
				
				if (allIdle)
				{
					isInControl = false;
					envSpace = space;
					CentralServerInterface myServer = (CentralServerInterface) thisServer;
					SuperCentralServerInterface superCentral = scServer;
					ExternalCommunicator extComm = new ExternalCommunicator(superCentral, myServer, Methods.INFORM, null);
					extComm.start();
				}
			}
		}
		
		@Override
		public void run() 
		{
			if (!isRegistered)
			{
				this.searchingForSuperCentral();
			}
		}


		@Override
		public void confirm(CentralServerInterface callerServer, Events eventType,
				boolean isSuccessful, final Object[] parameters) throws RemoteException 
		{
			if (eventType.equals(Events.REGISTER))
			{
				if (callerServer.equals(scServer))
				{
					isRegistered = isSuccessful;
					if (isRegistered == false)
					{
						Thread x = new Thread (this);
						x.start();
					}
				}
			}
			
			//TODO: UNREGISTER event confirmation procedure
		}
		
		/**
		 * Method for searching on super central
		 */
		public void searchingForSuperCentral ()
		{
			if (isRegistered == false)
			{
				superCentralAddress = "rmi://"+sc_host+":"+sc_port+"/SuperCentralJadex/"+sc_name;
				
//				Scanner keyboard = new Scanner(System.in);
//				try 
//				{
//					String[] list;
//					do
//					{
//						String[] rawList = Naming.list("rmi://"+SC_HOST+":"+SC_PORT+"/SuperCentralJadex/");
//						String[] tempList = new String[rawList.length];
//						int x = 0;
//						for (int j = 0; j < rawList.length; j++)
//						{
//							if (rawList[j].contains("SuperCentralJadex"))
//							{
//								tempList[x] = (rawList[j]);
//								x++;
//							}
//						}
//						list = new String[x];
//						for (;x > 0;x--)
//						{
//							list[x-1] = tempList[x-1];
//						}
//						
//						if (list.length > 0)
//						{
//							System.out.println("Choose SuperCentral to register in :");
//							for(int i = 0; i < list.length; i++)
//							{
//								System.out.println(i +". "+list[i]);
//							}
//							System.out.print("Choose the available Super Central to be registered to: ");
//							if (keyboard.hasNextInt())
//							{
//								int choice = keyboard.nextInt();
//								if (choice >= 0 && choice < list.length)
//								{
//									superCentralAddress = list[choice];
//									this.registerToSuperCentral();
//								}
//								else
//								{
//									System.out.println("Choice is out of bound. Press Enter to Search again...");
//									keyboard.hasNext();
//								}
//							}
//							else
//							{
//								System.out.println("Invalid input. Press Enter to Search again...");
//								keyboard.hasNext();
//							}
//						}
//						else
//						{
//							System.out.println("SuperCentral not Exists. Press Enter to Search again...");
//							keyboard.hasNext();
//						}
//					} while (list.length == 0);
//					
//				} catch (MalformedURLException e) {
//					e.printStackTrace();
//				} catch (RemoteException e) {
//					e.printStackTrace();
//				} 	
			}
		}
		
		private void registerToSuperCentral ()
		{
			if (isRegistered == false)
			{
				try 
				{
					scServer = (SuperCentralServerInterface)Naming.lookup(superCentralAddress);
					scServer.administerRegistration(SuperCentralServerInterface.REGISTER, this);
					LOGGER.info("Register to " + superCentralAddress);
				} catch (MalformedURLException e) {
					e.printStackTrace();
					LOGGER.severe(e.getMessage());
				} catch (RemoteException e) {
					e.printStackTrace();
					LOGGER.severe(e.getMessage());
				} catch (NotBoundException e) {
					e.printStackTrace();
					LOGGER.severe(e.getMessage());
				}	
			}
		}

		@Override
		public Object[] retrieveData(final ConfirmatorInterface confirmator) throws RemoteException 
		{	
			getExternalAccess().scheduleStep(new IComponentStep<Void>()
			{
				@Classname("retrieveData")
				public IFuture<Void> execute(IInternalAccess ia)
				{
					IBDIInternalAccess	scope	= (IBDIInternalAccess)ia;
					IEnvironmentSpace space = (IEnvironmentSpace)scope.getBeliefbase().getBelief("environment").getFact();
					
					Object[] params = new Object[1];
					params[0] = space.getProperty("agentDataContainer");
					
					try 
					{
						LOGGER.info("--SENDING DATA TO SC--");
						confirmator.confirm((CentralServerInterface)thisServer, Events.RETRIEVE, true, params);
					} catch (RemoteException e) 
					{
						e.printStackTrace();
						LOGGER.severe(e.getMessage());
					}
					
					return IFuture.DONE;
				}
			});
			return null;
		}

		@Override
		public void killAgents(final String[] agentIDs) throws RemoteException 
		{
			IFuture<Void> fut =
			getExternalAccess().scheduleStep(new IComponentStep<Void>()
			{
				@SuppressWarnings({ "rawtypes", "unchecked" })
				@Classname("killAgents")
				public IFuture<Void> execute(IInternalAccess ia)
				{
					IBDIInternalAccess	scope	= (IBDIInternalAccess)ia;
					IEnvironmentSpace space = (IEnvironmentSpace)scope.getBeliefbase().getBelief("environment").getFact();
					Map mParameters = new HashMap();
					mParameters.put("agentIDs", agentIDs);
					KillAgentsAction action = new KillAgentsAction ();
					action.perform(mParameters, space);
					return IFuture.DONE;
				}
			});
			fut.get(this);
			
		}

		@Override
		public Object getMonitor() {
			return null;
		}

		public ABMRemoteServerInterface getAbmServer() {
			return abmServer;
		}

		public void setAbmServer(ABMRemoteServerInterface abmServer) {
			this.abmServer = abmServer;
		}
		
		public SuperCentralServerInterface getSCServer() {
			return scServer;
		}

		public void setSCServer(SuperCentralServerInterface scServer) {
			this.scServer = scServer;
		}

		@Override
		public void terminateProgram() throws RemoteException {
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
		}

		@Override
		public void resume(IFuture<?> arg0) {
			this.notify();
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
