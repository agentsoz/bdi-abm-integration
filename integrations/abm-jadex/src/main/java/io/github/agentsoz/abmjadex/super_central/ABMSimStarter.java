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


import io.github.agentsoz.abmjadex.central_organizer.CentralOrganizerXMLCreator;
import io.github.agentsoz.abmjadex.miscellaneous.ABMBDILoggerSetter;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import jadex.base.Starter;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IExternalAccess;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.cms.CreationInfo;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.commons.future.IFuture;
import jadex.commons.future.ThreadSuspendable;
import jadex.extension.envsupport.environment.IEnvironmentSpace;
import jadex.javaparser.SJavaParser;

public class ABMSimStarter 
{
	private final static Logger LOGGER = Logger.getLogger(ABMSimStarter.class.getName());
	
	public final static String SC_ONLY = "sc_only";
	public final static String SC_CO = "sc_co";
	public final static String CO_ONLY = "co_only";
	
	public final static String GUI = "gui";
	public final static String FORCED = "f";
	public final static String FORCED_LONG = "force";
	public final static String PROP = "prop";
	public final static String PROP_LONG = "properties";
	public final static String MODE ="mode";
	public final static String HELP ="h";
	public final static String HELP_LONG ="help";
	public final static String LOG_CONSOLE ="console";
	public final static String LOG_LVL ="loglvl";
	
	public static void main (String[] args)
	{
		parsingArguments(args, commandLinePreparation());
//		startThroughCMS(args);
//		startThroughStarterMain(args);
	}
	
	@SuppressWarnings({"static-access" })
	private static Options commandLinePreparation ()
	{	
		Options options = new Options();
		
		// create the Options		
		options.addOption(HELP, "help", false, "print the help list on usage (this message.)");
		options.addOption(FORCED, "force", false, "overwrite any CentralOrganizer created before.");
		options.addOption(LOG_CONSOLE, false, "Deciding whether to show logging to console or not");
		
		options.addOption(OptionBuilder.withDescription("Launch JCC or not (by default : not)")
				   					   .hasArg()
				   					   .withArgName("run_jcc")
				   					   .create(GUI));
		options.addOption(OptionBuilder.withLongOpt("properties")
									   .withDescription("Locate the app's properties file")
									   .hasArg()
									   .withArgName("properties_file")
									   .create(PROP));
		options.addOption(OptionBuilder.withDescription( "Choose the starting mode."
		                                         			+"sc_only, will run only the SuperCentral," 
		                                         			+"sc_co, will run both sc and co specified in properties file"
		                                         			+"co_only, will run only a central organizer")
		                                .hasArg()
		                                .withArgName("start_mode")
		                                .create(MODE) );
		options.addOption(OptionBuilder.withDescription("Setup the logging level."
		                                				+"{SEVERE,WARNING,INFO,CONFIG,FINE,FINER,FINEST}(default: INFO)")
     								   .hasArg()
     								   .withArgName("log_level")
     								   .create(LOG_LVL) );
		return options;
	}
	
	private static String[] packageOptionSpecific (String[] oldOption)
	{
		String[] specificOption = null;
		ArrayList<String> specificOptionList = new ArrayList<String>();
		
		for (int i = 0; i < oldOption.length; i++)
		{
			if (oldOption[i].equals("-"+LOG_CONSOLE) || oldOption[i].equals("-"+HELP) || oldOption[i].equals("--"+HELP_LONG)
					||oldOption[i].equals("-"+FORCED) || oldOption[i].equals("--"+FORCED_LONG))
			{
				specificOptionList.add(oldOption[i]);
			}
			else if  (oldOption[i].equals("-"+GUI) || oldOption[i].equals("-"+LOG_LVL) || oldOption[i].equals("-"+GUI) ||oldOption[i].equals("-"+MODE)
					|| oldOption[i].equals("-"+PROP) || oldOption[i].equals("--"+PROP_LONG))
			{
				specificOptionList.add(oldOption[i]);
				i++;
				specificOptionList.add(oldOption[i]);
			}
		}
		specificOption = new String[specificOptionList.size()];
		specificOptionList.toArray(specificOption);
		return specificOption;
	}
	
	private static void parsingArguments (String[] args, Options options)
	{
		//flags
		boolean isRunningCO = true; 
		boolean isForced = false; //Overwrite any existing CO xml file
		boolean isRunningSC = true;
		boolean isLogToConsole = false;
		Level logLevel = Level.INFO;
		
		Properties prop = null;
		CommandLine line = null;
		BasicParser parser = new BasicParser();
		ArrayList<String> argsList = new ArrayList<String>();
		
		//Initialize argsList with args
		for (int i = 0; i < args.length; i++)
		{
			argsList.add(args[i]);
		}
		
		//Update 04/17/2013
		String[] specificArgs = packageOptionSpecific (args);
		
		try 
		{
		    // parse the command line arguments
		    //line = parser.parse(options, args );
			//Update 04/17/2013
		    line = parser.parse(options, specificArgs);
		    
		    //Commandline required -prop argument to be filled with valid properties file location
		    if (line.hasOption(HELP))
		    {
		    	//Remove app specific arguments from total arguments
				int helpIndex = argsList.indexOf("-"+HELP);
				if (helpIndex == -1)
					helpIndex = argsList.indexOf("-"+HELP_LONG);
				argsList.remove(helpIndex);
				
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("Jadex-ABMS", options );
		    }
		    
		    if(line.hasOption(PROP)) 
		    {
		    	//Remove app specific arguments from total arguments
				int propIndex = argsList.indexOf("-"+PROP);
				if (propIndex == -1)
					propIndex = argsList.indexOf("-"+PROP_LONG);
				argsList.remove(propIndex + 1);
				argsList.remove(propIndex);
		    	
		    	String propertiesLoc = line.getOptionValue(PROP).replace("\\", "/").replace("\\\\", "/");
		    	prop = new Properties();
				
				try 
				{
					prop.load(new FileInputStream(propertiesLoc));
					//Parsing options value into local flags------------------------
					if (line.hasOption(MODE))
					{
						String mode = line.getOptionValue(MODE);
						if (mode.equalsIgnoreCase(CO_ONLY))
						{
							isRunningSC = false;
						}
						else if (mode.equalsIgnoreCase(SC_CO))
						{
							//Default value is to run an SC and a CO
						}
						else if (mode.equalsIgnoreCase(SC_ONLY))
						{
							isRunningCO = false;
						}
						else
						{
							throw new ParseException("Wrong argument for -mode.");
						}
						
						//Remove app specific arguments from total arguments
						int modeIndex = argsList.indexOf("-"+MODE);
						argsList.remove(modeIndex + 1);
						argsList.remove(modeIndex);
					}
					
					if (line.hasOption(FORCED))
					{
						isForced = true;
						//Remove app specific arguments from total arguments
						int modeIndex = argsList.indexOf("-"+FORCED);
						if (modeIndex == -1)
							modeIndex = argsList.indexOf("-"+FORCED_LONG);
						argsList.remove(modeIndex);
					}
					
					if (line.hasOption(GUI))
					{
						String guiMode = line.getOptionValue(GUI);
						if (!guiMode.equalsIgnoreCase("true") && !guiMode.equalsIgnoreCase("false"))
							throw new ParseException("Wrong argument for -gui.");
					}
					else
					{
						argsList.add("-"+GUI);
						int guiIndex = argsList.indexOf("-"+GUI);
						argsList.add(guiIndex+1, "false");
					}
					
					if (line.hasOption(LOG_CONSOLE))
					{
						isLogToConsole = true;
						//Remove app specific arguments from total arguments
						int logCIndex = argsList.indexOf("-"+LOG_CONSOLE);
						argsList.remove(logCIndex);
					}
					
					if (line.hasOption(LOG_LVL))
					{
						String level = line.getOptionValue(LOG_LVL);
						if (level.equalsIgnoreCase("INFO"))
						{
							logLevel = Level.INFO;
						}
						else if (level.equalsIgnoreCase("ALL"))
						{
							logLevel = Level.ALL;
						}
						else if (level.equalsIgnoreCase("CONFIG"))
						{
							logLevel = Level.CONFIG;
						}
						else if (level.equalsIgnoreCase("FINE"))
						{
							logLevel = Level.FINE;
						}
						else if (level.equalsIgnoreCase("FINER"))
						{
							logLevel = Level.FINER;
						}
						else if (level.equalsIgnoreCase("FINEST"))
						{
							logLevel = Level.FINEST;
						}
						else if (level.equalsIgnoreCase("OFF"))
						{
							logLevel = Level.OFF;
						}
						else if (level.equalsIgnoreCase("SEVERE"))
						{
							logLevel = Level.SEVERE;
						}
						else if (level.equalsIgnoreCase("WARNING"))
						{
							logLevel = Level.WARNING;
						}
						else
						{
							throw new ParseException("argument for loglvl unknown");
						}
						//Remove app specific arguments from total arguments
						int logLvlIndex = argsList.indexOf("-"+LOG_LVL);
						argsList.remove(logLvlIndex+1);
						argsList.remove(logLvlIndex);
					}
					
					//Setup logger
					try 
					{
						ABMBDILoggerSetter.initialized(prop, isLogToConsole, logLevel);
						ABMBDILoggerSetter.setup(LOGGER);
					}
					catch (IOException e)
					{
						e.printStackTrace();
						LOGGER.severe(e.getMessage());
						throw new RuntimeException("Problems with creating logfile");
					}
					
					//Translate argsList into array------------------------------
					String[] newargs = new String[argsList.size()];
					for (int i = 0; i < argsList.size(); i++)
					{
						newargs[i] = argsList.get(i);
					}
					
					//Running the system----------------------------------------
					if (isRunningSC == true)
					{
						runSC(prop);
					}
					
					if (isRunningCO == true)
					{
						runCO(prop, newargs, isForced);
					}
					
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
					LOGGER.severe(e.getMessage());
				}
		    }
		    else
		    {
		    	throw new ParseException("-prop <properties_location> is a required option");
		    }
		    
		}
		catch(ParseException exp ) 
		{
		    LOGGER.severe( "Unexpected exception:" + exp.getMessage() );
		    
		    //If its not working print out help info
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("Jadex-ABMS", options );
		}
	}
	
	private static void runSC (Properties prop)
	{
		String [] platfutArgs = {"-gui","false"};
		
		// Start the platform with the arguments.
		IFuture<IExternalAccess> platfut = Starter.createPlatform(platfutArgs);
		
		// Wait until the platform has started and retrieve the platform access.
		ThreadSuspendable	sus	= new ThreadSuspendable();
		IExternalAccess	platform	= platfut.get(sus);
		
		LOGGER.info("Started platform: "+platform.getComponentIdentifier());
		
		// Get the CMS service from the platform
		IComponentManagementService	cms	= SServiceProvider.getService(platform.getServiceProvider(),
			IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM).get(sus);
		
		// Start the SuperCentralOrganizer
		String component = "rmit/agent/jadex/abm_interface/super_central/SuperCentralOrganizer.application.xml";
		IComponentIdentifier cid	= createComponent(component, cms, sus);
		LOGGER.info("Started Super Central component: "+cid);
		
		// Initialize Port
		IExternalAccess superCentral = cms.getExternalAccess(cid).get(sus);
		IEnvironmentSpace space = (IEnvironmentSpace)superCentral.getExtension("my2dspace").get(sus);
		space.setProperty("port", Integer.parseInt(prop.getProperty("sc_port")));
		space.setProperty("name", prop.getProperty("sc_name"));
		space.setProperty("minCapacityPerCO", Integer.parseInt(prop.getProperty("minCapacityPerCO", "25")));
		space.setProperty("maxCapacityPerCO", Integer.parseInt(prop.getProperty("maxCapacityPerCO", "100")));
		space.createSpaceObject("SuperStarter", null, null);
	}
	
	private static void runCO (Properties prop, String[] args, boolean isOverwriting)
	{	
		Properties properties = new Properties(prop);
		//Create CO files
		CentralOrganizerXMLCreator coCreator = new CentralOrganizerXMLCreator();
		coCreator.create(properties, isOverwriting);

		// Start the platform with the arguments.
		IFuture<IExternalAccess> platfut = Starter.createPlatform(args);
		
		// Wait until the platform has started and retrieve the platform access.
		ThreadSuspendable	sus	= new ThreadSuspendable();
		IExternalAccess	platform	= platfut.get(sus);
		
		LOGGER.info("Started platform: "+platform.getComponentIdentifier());
		
		// Get the CMS service from the platform
		IComponentManagementService	cms	= SServiceProvider.getService(platform.getServiceProvider(),
			IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM).get(sus);
		
		// Start the SuperCentralOrganizer
		String component = prop.getProperty("build_location").replace("\\", "/").replace("\\\\", "/");
		IComponentIdentifier cid	= createComponent(component, cms, sus);
		LOGGER.info("Started Central Organizer component: "+cid);
	}
	
	@SuppressWarnings("unchecked")
	public static IComponentIdentifier createComponent (String component, IComponentManagementService cms, ThreadSuspendable sus)
	{
		String name	= null;
		String config	= null;
		String args = null;
		Map<String, Object> oargs = null;
		String comp = component;
		// check if name:type are both present (to not find last : check that no ( before)
		int	i1	= comp.indexOf(':');
		int i11 = comp.indexOf('(');
		if(i1!=-1 && (i11==-1 || i11>i1))
		{
			name	= comp.substring(0, i1);
			comp	= comp.substring(i1+1);
		}
		
		// check if (config:args) part is present
		int	i2	= comp.indexOf('(');
		if(i2!=-1)
		{
			// must end with )
			// must have : if both are presents otherwise all is configname
			if(!comp.endsWith(")"))
			{
				throw new RuntimeException("Component specification does not match scheme [<name>:]<type>[(<config>)[:<args>]) : "+component);
			}

			int i3 = comp.indexOf(":");
			if(i3!=-1)
			{
				if(comp.length()-i3>1)
					args = comp.substring(i3+1, comp.length()-1);
				if(i3-i2>1)
					config	= comp.substring(i2+1, i3-1);
			}
			else
			{
				config = comp.substring(i2+1, comp.length()-1);
			}
			
			comp = comp.substring(0, i2);	
		}
		
		if(args!=null)
		{
			try
			{
				Object o = SJavaParser.evaluateExpression(args, null);
				if(!(o instanceof Map))
				{
					throw new RuntimeException("Arguments must evaluate to Map<String, Object>"+args);
				}
				oargs = (Map<String, Object>)o;
			}
			catch(Exception e)
			{
				LOGGER.severe(e.getMessage());
				throw new RuntimeException("Arguments evaluation error: "+e);
			}
		}
		
		return cms.createComponent(name, comp, new CreationInfo(config, oargs), null).get(sus);
	}
	
	
	public static void startThroughStarterMain (String[] args)
	{
		//Create The Desired CO
		int indexCO = -1;
		for (int j = 0; j < args.length; j++)
		{
			if(args[j].equals("-createCO"))
			{
				indexCO = j;	
			}
		}
		if(indexCO == -1 || args[indexCO+1].equals("true"))
			CentralOrganizerXMLCreator.main(args);
		
		//Run an instance of SC & Run a JCC, Except User defined special args to do
		//something else
		String[] newArgs = new String[args.length + 2];
		newArgs[0] = "-component";
		newArgs[1] = "rmit/agent/jadex/abm_interface/super_central/SuperCentralOrganizer.application.xml";
		for (int i = 0; i < args.length; i++)
		{
			if (i != indexCO || (indexCO!=-1 && i!=indexCO+1))
				newArgs[i+2] = args[i];
		}
		jadex.base.Starter.main(newArgs);
	}
}
