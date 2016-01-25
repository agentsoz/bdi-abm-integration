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
import io.github.agentsoz.abmjadex.super_central.ABMSimStarter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Class to create a Central Organizer based on a java properties file
 * input at the start of this class main function
 */
public class CentralOrganizerXMLCreator 
{
	private final static Logger LOGGER = Logger.getLogger(ABMSimStarter.class.getName());
	public CentralOrganizerXMLCreator ()
	{
		ABMBDILoggerSetter.setup(LOGGER);
	}
	
	public static void main (String[] args)
	{
		//Get The path to properties file
		@SuppressWarnings("resource")
		Scanner keyboard = new Scanner(System.in);
		System.out.println("Enter the path where the Properties File is :");
		System.out.println("(Enter empty space if the creation of new CentralOrganizer is not desired)");
		String propertiesLoc = keyboard.nextLine();
		if(propertiesLoc.trim().equals("") == false)
		{
			Properties prop = new Properties();
			
			try 
			{
				prop.load(new FileInputStream(propertiesLoc));
				CentralOrganizerXMLCreator creator = new CentralOrganizerXMLCreator();
				creator.write(prop);
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
				LOGGER.severe(e.getMessage());
			}
		}
	}
	
	public void create (Properties properties, boolean isOverwriting)
	{
		String buildLocation = properties.getProperty("build_location").trim();
		File coXML = new File(buildLocation);
		
		if (coXML.exists() == false || isOverwriting)
		{
			write (properties);
		}
		else
		{
			LOGGER.info("File already exist and overwriting is not a desired action."
					+" nothing new created.");
		}
		
	}
	
	public void write (Properties properties)
	{

		//Getting on the file name to be created
		String buildLocation = properties.getProperty("build_location").trim();
		String[] buildLocationSplitted = buildLocation.split("/");
		String xmlName = buildLocationSplitted[buildLocationSplitted.length - 1];
		
		//Get the package where the file will be included in
		String packageProp = properties.getProperty("package");
		
		//Get the starter agent location
		String starterLocation = "";
		//Get the whole repastClass-JadexXml tuple
		Set<String> agentInfoKeys = properties.stringPropertyNames();
		
		agentInfoKeys.remove("build_location");
		agentInfoKeys.remove("package");
		agentInfoKeys.remove("Jadex-ABMS_path");
		agentInfoKeys.remove("co_port");
		agentInfoKeys.remove("sc_port");
		agentInfoKeys.remove("sc_host");
		agentInfoKeys.remove("sc_name");
		agentInfoKeys.remove("logging_location");
		agentInfoKeys.remove("maxCapacityPerCO");
		agentInfoKeys.remove("minCapacityPerCO");
		
		HashMap<String, String> agentsInfo = new HashMap<String,String>();
		for (Object key : agentInfoKeys)
		{
			String keyString = (String)key;
			agentsInfo.put(keyString, properties.getProperty(keyString));
		}
		
		//Begin the creation of xml
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		
		try 
		{
			docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			
			//root elements
			Element rootElement = createRoot (doc, xmlName, packageProp);
			
			//Imports elements
			this.createImport(doc, rootElement);
			
			//
			this.createExtensionsTypes(doc, rootElement, starterLocation, agentsInfo);
			
			//ComponentTypes elements
			this.createComponentTypes(doc, rootElement, starterLocation, agentsInfo);
			
			//Configurations elements
			this.createConfigurations(doc, rootElement, properties);
			
			//Build the XML
			this.buildXML(doc, buildLocation);
		} 
		catch (ParserConfigurationException e) 
		{
			e.printStackTrace();
			LOGGER.severe(e.getMessage());
		}
	}
	
	private void createExtensionsTypes (Document doc, Element root, String starterLocation
			,HashMap<String, String> agentsInfo)
	{
		Element extensiontypes = doc.createElement("extensiontypes");
		root.appendChild(extensiontypes);
		
		//Env:SpaceType
		Element env_space = doc.createElement("env:envspacetype");
		extensiontypes.appendChild(env_space);
		
		Attr envSpaceName = doc.createAttribute("name");
		envSpaceName.setValue("2dspace");
		env_space.setAttributeNode(envSpaceName);
		
		Attr envSpaceType = doc.createAttribute("class");
		envSpaceType.setValue("ContinuousSpace2D");
		env_space.setAttributeNode(envSpaceType);
		
		Attr envSpaceWidth = doc.createAttribute("width");
		envSpaceWidth.setValue("1.0");
		env_space.setAttributeNode(envSpaceWidth);
		
		Attr envSpaceHeight = doc.createAttribute("height");
		envSpaceHeight.setValue("1.0");
		env_space.setAttributeNode(envSpaceHeight);
		
		//env_property
		Element env_property = doc.createElement("env:property");
		env_space.appendChild(env_property);
		
		Attr name = doc.createAttribute("name");
		name.setValue("border");
		env_property.setAttributeNode(name);
		
		env_property.appendChild(doc.createTextNode("Space2D.BORDER_STRICT"));
		
		//objectTypes
		this.createObjectTypes(doc, env_space, starterLocation, agentsInfo);
		
		//avatarmappings
		this.createAvatarMappings(doc, env_space, starterLocation, agentsInfo);
		
		//actiontypes
		this.createActionTypes(doc, env_space, starterLocation, agentsInfo);
		
		//space executor
		Element env_spaceexecutor = doc.createElement("env:spaceexecutor");
		env_space.appendChild(env_spaceexecutor);
		
		Attr executorClass = doc.createAttribute("class");
		executorClass.setValue("DeltaTimeExecutor");
		env_spaceexecutor.setAttributeNode(executorClass);
		
		{
			Element env_prop = doc.createElement("env:property");
			env_spaceexecutor.appendChild(env_prop);
			
			Attr prop_name = doc.createAttribute("name");
			prop_name.setValue("space");
			env_prop.setAttributeNode(prop_name);
			
			env_prop.appendChild(doc.createTextNode("$space"));
		}
		
		{
			Element env_prop = doc.createElement("env:property");
			env_spaceexecutor.appendChild(env_prop);
			
			Attr prop_name = doc.createAttribute("name");
			prop_name.setValue("tick");
			env_prop.setAttributeNode(prop_name);
			
			env_prop.appendChild(doc.createTextNode("true"));
		}
	}
	
	private void createActionTypes (Document doc, Element root, String starterLocation
			,HashMap<String, String> agentsInfo)
	{
		Element env_actiontypes = doc.createElement("env:actiontypes");
		root.appendChild(env_actiontypes);
		
		{
			Element env_actiontype = doc.createElement("env:actiontype");
			env_actiontypes.appendChild(env_actiontype);
			
			Attr name = doc.createAttribute("name");
			name.setValue("idle_action");
			env_actiontype.setAttributeNode(name);
			
			Attr action_class = doc.createAttribute("class");
			action_class.setValue("IdleAction");
			env_actiontype.setAttributeNode(action_class);
		}
		
		{
			Element env_actiontype = doc.createElement("env:actiontype");
			env_actiontypes.appendChild(env_actiontype);
			
			Attr name = doc.createAttribute("name");
			name.setValue("unidle_agent_action");
			env_actiontype.setAttributeNode(name);
			
			Attr action_class = doc.createAttribute("class");
			action_class.setValue("UnidleAgentAction");
			env_actiontype.setAttributeNode(action_class);
		}
		
		{
			Element env_actiontype = doc.createElement("env:actiontype");
			env_actiontypes.appendChild(env_actiontype);
			
			Attr name = doc.createAttribute("name");
			name.setValue("query_percept_action");
			env_actiontype.setAttributeNode(name);
			
			Attr action_class = doc.createAttribute("class");
			action_class.setValue("QueryPerceptAction");
			env_actiontype.setAttributeNode(action_class);
		}
		
		{
			Element env_actiontype = doc.createElement("env:actiontype");
			env_actiontypes.appendChild(env_actiontype);
			
			Attr name = doc.createAttribute("name");
			name.setValue("inter_central_action");
			env_actiontype.setAttributeNode(name);
			
			Attr action_class = doc.createAttribute("class");
			action_class.setValue("InterCentralAction");
			env_actiontype.setAttributeNode(action_class);
		}
		
		{
			Element env_actiontype = doc.createElement("env:actiontype");
			env_actiontypes.appendChild(env_actiontype);
			
			Attr name = doc.createAttribute("name");
			name.setValue("kill_action");
			env_actiontype.setAttributeNode(name);
			
			Attr action_class = doc.createAttribute("class");
			action_class.setValue("KillAgentsAction");
			env_actiontype.setAttributeNode(action_class);
		}
		
		{
			Element env_actiontype = doc.createElement("env:actiontype");
			env_actiontypes.appendChild(env_actiontype);
			
			Attr name = doc.createAttribute("name");
			name.setValue("update_float_action");
			env_actiontype.setAttributeNode(name);
			
			Attr action_class = doc.createAttribute("class");
			action_class.setValue("FloatingMsgUpdateAction");
			env_actiontype.setAttributeNode(action_class);
		}
	}
	
	private void createAvatarMappings (Document doc, Element root, String starterLocation
			,HashMap<String, String> agentsInfo)
	{
		Element env_avatarmappings = doc.createElement("env:avatarmappings");
		root.appendChild(env_avatarmappings);
		
		{
			Element env_avatarmapping = doc.createElement("env:avatarmapping");
			env_avatarmappings.appendChild(env_avatarmapping);
			
			Attr objecttype = doc.createAttribute("objecttype");
			objecttype.setValue("Starter");
			env_avatarmapping.setAttributeNode(objecttype);
			
			Attr componenttype = doc.createAttribute("componenttype");
			componenttype.setValue("Starter");
			env_avatarmapping.setAttributeNode(componenttype);
		}
		
		Set<String> keys = agentsInfo.keySet();
		for(String key:keys)
		{		
			Element env_avatarmapping = doc.createElement("env:avatarmapping");
			env_avatarmappings.appendChild(env_avatarmapping);
			
			Attr objecttype = doc.createAttribute("objecttype");
			objecttype.setValue(key);
			env_avatarmapping.setAttributeNode(objecttype);
			
			Attr componenttype = doc.createAttribute("componenttype");
			componenttype.setValue(key);
			env_avatarmapping.setAttributeNode(componenttype);
			
			Attr createcomponent = doc.createAttribute("createcomponent");
			createcomponent.setValue("true");
			env_avatarmapping.setAttributeNode(createcomponent);
		}
		
	}
	
	private void createObjectTypes(Document doc, Element root, String starterLocation
			,HashMap<String, String> agentsInfo)
	{
		Element env_objecttypes = doc.createElement("env:objecttypes");
		root.appendChild(env_objecttypes);
		
		{
			Element objecttype = doc.createElement("env:objecttype");
			env_objecttypes.appendChild(objecttype);
			
			Attr name = doc.createAttribute("name");
			name.setValue("Starter");
			objecttype.setAttributeNode(name);
		}
		
		Set<String> keys = agentsInfo.keySet();
		for(String key:keys)
		{		
			Element objecttype = doc.createElement("env:objecttype");
			env_objecttypes.appendChild(objecttype);
			
			Attr name = doc.createAttribute("name");
			name.setValue(key);
			objecttype.setAttributeNode(name);
			
			{
				Element env_prop = doc.createElement("env:property");
				objecttype.appendChild(env_prop);
				
				Attr prop_name = doc.createAttribute("name");
				prop_name.setValue("agentID");
				env_prop.setAttributeNode(prop_name);
				
				Attr prop_class = doc.createAttribute("class");
				prop_class.setValue("String");
				env_prop.setAttributeNode(prop_class);
			}
			
			{
				Element env_prop = doc.createElement("env:property");
				objecttype.appendChild(env_prop);
				
				Attr prop_name = doc.createAttribute("name");
				prop_name.setValue("componentID");
				env_prop.setAttributeNode(prop_name);
				
				Attr prop_class = doc.createAttribute("class");
				prop_class.setValue("IComponentIdentifier");
				env_prop.setAttributeNode(prop_class);
			}
			
			{
				Element env_prop = doc.createElement("env:property");
				objecttype.appendChild(env_prop);
				
				Attr prop_name = doc.createAttribute("name");
				prop_name.setValue("actionContainer");
				env_prop.setAttributeNode(prop_name);
				
				Attr prop_class = doc.createAttribute("class");
				prop_class.setValue("ActionContainer");
				env_prop.setAttributeNode(prop_class);
			}
			
			{
				Element env_prop = doc.createElement("env:property");
				objecttype.appendChild(env_prop);
				
				Attr prop_name = doc.createAttribute("name");
				prop_name.setValue("perceptContainer");
				env_prop.setAttributeNode(prop_name);
				
				Attr prop_class = doc.createAttribute("class");
				prop_class.setValue("PerceptContainer");
				env_prop.setAttributeNode(prop_class);
			}
		}
		
	}
	private void createComponentTypes (Document doc, Element root, String starterLocation
										,HashMap<String, String> agentsInfo)
	{
		Element componenttypes = doc.createElement("componenttypes");
		root.appendChild(componenttypes);
		{
			Element componenttype = doc.createElement("componenttype");
			componenttypes.appendChild(componenttype);

			Attr filename = doc.createAttribute("filename");
			starterLocation = "/rmit/agent/jadex/abm_interface/central_organizer/Starter.agent.xml";
			filename.setValue(starterLocation);
			componenttype.setAttributeNode(filename);
			
			Attr name = doc.createAttribute("name");
			name.setValue("Starter");
			componenttype.setAttributeNode(name);
		}
		
		Set<String> keys = agentsInfo.keySet();
		for(String key:keys)
		{		
			Element componenttype = doc.createElement("componenttype");
			componenttypes.appendChild(componenttype);

			Attr filename = doc.createAttribute("filename");
			filename.setValue(agentsInfo.get(key));
			componenttype.setAttributeNode(filename);
			
			Attr name = doc.createAttribute("name");
			name.setValue(key);
			componenttype.setAttributeNode(name);
		}
	}
	
	private void createConfigurations (Document doc, Element root, Properties prop)
	{
		Element configurations = doc.createElement("configurations");
		root.appendChild(configurations);
		
		Element configuration = doc.createElement("configuration");
		configurations.appendChild(configuration);
		
		Attr configName = doc.createAttribute("name");
		configName.setValue("default");
		configuration.setAttributeNode(configName);
		
		Element extensions = doc.createElement("extensions");
		configuration.appendChild(extensions);
		
		Element components = doc.createElement("components");
		configuration.appendChild(components);
		
		//Extensions filling
		Element env_space = doc.createElement("env:envspace");
		extensions.appendChild(env_space);
		
		Attr envSpaceName = doc.createAttribute("name");
		envSpaceName.setValue("my2dspace");
		env_space.setAttributeNode(envSpaceName);
		
		Attr envSpaceType = doc.createAttribute("type");
		envSpaceType.setValue("2dspace");
		env_space.setAttributeNode(envSpaceType);
		
		Attr envSpaceWidth = doc.createAttribute("width");
		envSpaceWidth.setValue("1.0");
		env_space.setAttributeNode(envSpaceWidth);
		
		Attr envSpaceHeight = doc.createAttribute("height");
		envSpaceHeight.setValue("1.0");
		env_space.setAttributeNode(envSpaceHeight);
		
		String[][] envProperties = {{"agentDataContainer","AgentDataContainer","new AgentDataContainer()"}
									,{"agentStateList","AgentStateList","new AgentStateList()"}
									,{"agentTypeList","ArrayList","new ArrayList()"}
									,{"agentToBeBornList","AgentStateList","new AgentStateList()"}
									,{"server","Object","null"}
									,{"componentAgentIDMap","HashMap","new HashMap()"}
									,{"floatingMsgNum","SyncInteger","new SyncInteger()"}
									,{"co_port","Integer",prop.getProperty("co_port")}
									,{"sc_port","Integer",prop.getProperty("sc_port")}
									,{"sc_host","String","\""+prop.getProperty("sc_host")+"\""}
									,{"sc_name","String","\""+prop.getProperty("sc_name")+"\""}
									};
		for (int i = 0; i < envProperties.length; i++)
		{
			Element envSpaceProp = doc.createElement("env:property");
			env_space.appendChild(envSpaceProp);
			
			Attr name = doc.createAttribute("name");
			name.setValue(envProperties[i][0]);
			envSpaceProp.setAttributeNode(name);
			
			Attr class_prop = doc.createAttribute("class");
			class_prop.setValue(envProperties[i][1]);
			envSpaceProp.setAttributeNode(class_prop);
			
			envSpaceProp.appendChild(doc.createTextNode(envProperties[i][2]));
		}
		//Components filling
		Element component = doc.createElement("component");
		components.appendChild(component);
		
		Attr compType = doc.createAttribute("type");
		compType.setValue("Starter");
		component.setAttributeNode(compType);
		
	}
	
	private void createImport (Document doc, Element root)
	{
		Element importsElement = doc.createElement("imports");
		root.appendChild(importsElement);
		
		String[] thingsToImport = {"jadex.extension.envsupport.environment.space2d.*"
									,"jadex.extension.envsupport.environment.*"
									,"jadex.extension.envsupport.math.*"
									,"jadex.extension.envsupport.dataview.*"
									,"jadex.extension.envsupport.observer.perspective.*"
									,"jadex.bdi.planlib.*"
									,"jadex.bdi.examples.cleanerworld.cleaner.*"
									,"jadex.bridge.service.types.clock.IClockService"
									,"jadex.bridge.service.search.*"
									,"jadex.bridge.service.*"
									,"jadex.bridge.*"
									,"jadex.commons.future.*"
									,"rmit.agent.jadex.abm_interface.data_structure.*"
									,"java.util.*"
									,"rmit.agent.jadex.abm_interface.*"
									,"rmit.agent.jadex.abm_interface.central_organizer.*"
									,"bdisim.data.*"};
		
		for (int i = 0; i < thingsToImport.length; i++)
		{
			Element importElement = doc.createElement("import");
			importElement.appendChild(doc.createTextNode(thingsToImport[i]));
			importsElement.appendChild(importElement);	
		}
	}
	
	private Element createRoot (Document doc, String xmlName, String packageProp)
	{
		//root elements
		Element rootElement = doc.createElement("applicationtype");
		doc.appendChild(rootElement);
		
		Attr xmlns = doc.createAttribute("xmlns");
		xmlns.setValue("http://jadex.sourceforge.net/jadex");
		rootElement.setAttributeNode(xmlns);
		
		Attr xmlns_env = doc.createAttribute("xmlns:env");
		xmlns_env.setValue("http://jadex.sourceforge.net/jadex-envspace");
		rootElement.setAttributeNode(xmlns_env);
		
		Attr xmlns_xsi = doc.createAttribute("xmlns:xsi");
		xmlns_xsi.setValue("http://www.w3.org/2001/XMLSchema-instance");
		rootElement.setAttributeNode(xmlns_xsi);
		
		Attr xsi_schemaLocation = doc.createAttribute("xsi:schemaLocation");
		xsi_schemaLocation.setValue("http://jadex.sourceforge.net/jadex" 
				+" http://jadex.sourceforge.net/jadex-application-2.3.xsd"
                +" http://jadex.sourceforge.net/jadex-envspace" 
                +" http://jadex.sourceforge.net/jadex-envspace-2.3.xsd");
		rootElement.setAttributeNode(xsi_schemaLocation);
		
		Attr name = doc.createAttribute("name");
		name.setValue(xmlName);
		rootElement.setAttributeNode(name);
		
		Attr package_attr = doc.createAttribute("package");
		package_attr.setValue(packageProp);
		rootElement.setAttributeNode(package_attr);
		
		return rootElement;
	}
	
	private void buildXML (Document doc, String buildLocation)
	{
		boolean isCreated = false;
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		try 
		{
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(buildLocation));
			
			transformer.transform(source, result);
			isCreated = true;
		} 
		catch (TransformerConfigurationException e) 
		{
			e.printStackTrace();
			LOGGER.severe(e.getMessage());
		} 
		catch (TransformerException e) 
		{
			e.printStackTrace();
			LOGGER.severe(e.getMessage());
		}
		
		if (isCreated == true)
		{
			LOGGER.info("XML file created in :"+buildLocation);
		}
	}
}
