package io.github.agentsoz.vaccination.controller;

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


import io.github.agentsoz.vaccination.Log;
import io.github.agentsoz.vaccination.Global.MessageType;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.*;
import org.w3c.dom.*;

import io.github.agentsoz.bdiabm.data.ActionContainer;
import io.github.agentsoz.bdiabm.data.ActionPerceptContainer;
import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.bdiabm.data.AgentState;
import io.github.agentsoz.bdiabm.data.ActionContent.State;
import io.github.agentsoz.bdiabm.data.AgentStateList;
import io.github.agentsoz.bdiabm.data.PerceptContainer;

/**
*
*@author Alex Lutman
*
**/



public class XMLHandler {

	private DocumentBuilder dBuilder;
	private Element root;

	public XMLHandler() throws ParserConfigurationException{
			dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	}
	
	public void setData(String data) throws SAXException, IOException {
		Document doc = dBuilder.parse(new InputSource(new StringReader(data)));
		Log.trace("Received XML : " + getStringFromDocument(doc));
		root = (Element)doc.getElementsByTagName("message").item(0);
	}
	
	public MessageType getMessageType() {
		return MessageType.valueOf(root.getAttribute("id"));
	}
	public long getMessageTime() {
		long t = -1;
		try {
			t = Long.parseLong(root.getAttribute("time"));
		}
		catch(Exception e){
			return -1;
		}
		return t;
		
	}

	//Common functionality for creating XML files
	//ref: http://www.mkyong.com/java/how-to-create-xml-file-in-java-dom/
	private static Document startXMLCreation(MessageType messageID, long time){
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			
			//<message id="ID">
			Element message = doc.createElement("message");
			message.setAttribute("id", messageID.toString());
			message.setAttribute("time", time+"");
			doc.appendChild(message);
			return doc;
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	
	}
	private static String finishXMLCreation(Document doc){
		try {
			//write content into a XML file
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			DOMSource source = new DOMSource(doc);
			
			StreamResult result = new StreamResult(new StringWriter());
			transformer.transform(source, result);
			
			return result.getWriter().toString();
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}
	public static String terminateToXML(long time) {
		try {
			Document doc = startXMLCreation(MessageType.TERMINATE_PROGRAM, time);
			return finishXMLCreation(doc);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}
	//Methods to create XML files from data types
	//Used to create start xml messages
	public static String toXML(AgentDataContainer adc, AgentStateList asl, long time){
		try {
			Document doc  = startXMLCreation(MessageType.START, time);
			addAgentDataContainer(adc, doc);
			addAgentStateList(asl, doc);
			return finishXMLCreation(doc);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}
	//Used to create takecontrol xml messages
	public static String toXML(AgentDataContainer adc, long time){
	    try {
			Document doc = startXMLCreation(MessageType.TAKE_CONTROL, time);
			addAgentDataContainer(adc, doc);
			return finishXMLCreation(doc);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	
	}
	//Used to create kill and create agent xml messages
	public static String toXML(String[] agentIDs, MessageType messageType, long time) {
		try {
			Document doc = startXMLCreation(messageType, time);
			addAgentIDList(agentIDs, doc);
			return finishXMLCreation(doc);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}
	
	//Methods to add data packs to XML files
	//adds agentIDs
	private static void addAgentIDList(String[] agentIDs, Document doc){
		try {
			Element data = doc.createElement("data");
			data.setAttribute("id",DataType.agentIDs.toString());
			doc.getDocumentElement().appendChild(data);
			for(int i = 0; i < agentIDs.length; i++) {
				Element agent = doc.createElement("agent");
				agent.setAttribute("id", agentIDs[i]);
				data.appendChild(agent);	
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	
	}
	//adds agentstatelist
	private static void addAgentStateList(AgentStateList adc, Document doc) {	
		try {
			Element data = doc.createElement("data");
			data.setAttribute("id",DataType.agentstatelist.toString());
			doc.getDocumentElement().appendChild(data);
			
			Iterator<AgentState> i = adc.iterator();
			while(i.hasNext()) {
				AgentState cur = i.next();
				Element agent = doc.createElement("agent");
				agent.setAttribute("id", cur.getID());
				data.appendChild(agent);
				Element isidle = doc.createElement("isidle");
				agent.appendChild(isidle);
				isidle.appendChild(doc.createTextNode(""+cur.isIdle()));
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}		
	}
	//adds agentdatacontainer
	private static void addAgentDataContainer(AgentDataContainer adc, Document doc){	
		try {
			Element data = doc.createElement("data");
			data.setAttribute("id",DataType.agentdatacontainer.toString());
			doc.getDocumentElement().appendChild(data);
			
			
			Iterator<Map.Entry<String, ActionPerceptContainer>> i = adc.entrySet().iterator();
			//add each agent
			while(i.hasNext()) {
				Map.Entry<String, ActionPerceptContainer> mapEntry = i.next();
				//<agent id="ID">
				Element agent = doc.createElement("agent");
				agent.setAttribute("id", mapEntry.getKey());
				data.appendChild(agent);
				
				//<actioncontainer>
				Element actioncontainer = doc.createElement("actioncontainer");
				agent.appendChild(actioncontainer);
				
				ActionContainer ac = mapEntry.getValue().getActionContainer();
				Iterator<String> i2 = ac.actionIDSet().iterator();
				//add each action
				while(i2.hasNext()) {
					String actionID = i2.next();
					//<actioncontent id="ID">
					Element actioncontent = doc.createElement("actioncontent");
					actioncontent.setAttribute("id", actionID);
					actioncontainer.appendChild(actioncontent);
					
					//<state>STATE</state>
					Element state = doc.createElement("state");
					actioncontent.appendChild(state);
					state.appendChild(doc.createTextNode(ac.get(actionID).getState().toString()));
					
					//for each param
					Object[] params = ac.get(actionID).getParameters();
					if(params != null) {
						for(int j = 0; j < params.length; j++) {
							//<parameter id="array num">DATA</parameter>
							Element parameter = doc.createElement("parameter");
							actioncontent.appendChild(parameter);
							parameter.appendChild(doc.createTextNode(""+params[j]));
						}
					}
				}
				PerceptContainer pc = mapEntry.getValue().getPerceptContainer();
				//<perceptcontainer>
				Element perceptcontainer = doc.createElement("perceptcontainer");
				agent.appendChild(perceptcontainer);
				Set<String> s3 = pc.perceptIDSet();
				String[] a3 = s3.toArray(new String[0]);
				//Iterator<String> i3 = s3.iterator();
				//add each percept
				
				//while(i3.hasNext()){
				for(int k = 0; k < a3.length; k++) {
					//String perceptID = i3.next();
					String perceptID = a3[k];
					//<percept id="ID">
					Element percept = doc.createElement("percept");
					percept.setAttribute("id", perceptID);
					perceptcontainer.appendChild(percept);
					
					Element parameters = doc.createElement("parameters");
					percept.appendChild(parameters);
					try {
						parameters.appendChild(doc.createTextNode(pc.read(perceptID).toString()));
					}
					catch (NullPointerException npe) {
						//Ignore null parameters
					}

		
				}
				
			}
		}
		catch (NullPointerException npe) {
			//Nothing to add
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

	}
	
	//Retrieval functions to get data types from XML files
	//Retrieve an AgentStateList
	public AgentStateList retrieveASL(){
		try {
			Element data = validData(DataType.agentstatelist.toString());
			if(data == null) {
				return null;
			}
			AgentStateList asl = new AgentStateList();
			NodeList agents = data.getElementsByTagName("agent");
			for(int i = 0; i < agents.getLength(); i++) {
				String state = ((Element)((Element)agents.item(i)).getElementsByTagName("isidle").item(0)).getTextContent();
				String id = ((Element)agents.item(i)).getAttribute("id");
				AgentState as = new AgentState(id);
				as.setIdleState(Boolean.parseBoolean(state));
				asl.add(as);
			}
			return asl;
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}
	// Retrieve agentIDs
	// public String[] retrieveAgentIDs() {
		// try {
			// Element data = validData(DataType.agentIDs.toString());
			// if(data == null) {
				// return null;
			// }
			// NodeList agents = data.getElementsByTagName("agent");
			// String[] agentIDs = new String[agents.getLength()];
			// for(int i = 0; i < agents.getLength(); i++) {
				// agentIDs[i] = ((Element)agents.item(i)).getAttribute("id");
			// }
			// return agentIDs;
		// }
		// catch (Exception e) {
			// e.printStackTrace();
			// System.exit(1);
		// }
		// return null;
	// }
	//retrieve ids and paramters to create agents
	public String[] retrieveAgentIDsCreate() {
		try {
			Element data = validData(DataType.agentIDs.toString());
			if(data == null) {
				return null;
			}
			//retrive the bunch of id's associated with create
			NodeList purpose = data.getElementsByTagName("purpose");
			Element purposeSingle = null;
			for(int i = 0; i < purpose.getLength(); i++) {
				if(((Element)purpose.item(i)).getAttribute("id").equals(DataType.purposeCreate.toString())) {
					purposeSingle = ((Element)purpose.item(i));
				}
			}
			//if there is no bunch associated with create, quit
			if(purposeSingle == null) {
				return null;
			}
			//dump ids into an array
			NodeList agents = purposeSingle.getElementsByTagName("agent");
			String[] agentIDs = new String[agents.getLength()];
			//for each agent
			for(int i = 0; i < agents.getLength(); i++) {
				String agentID = ((Element)agents.item(i)).getAttribute("id");
				double vaebase = DataType.DEFAULT_BASELINE;
				double vpdbase = DataType.DEFAULT_BASELINE;
				boolean lastvacc = DataType.DEFAULT_LASTVACC;
				NodeList parameters = ((Element)agents.item(i)).getElementsByTagName("parameter");
				for(int j = 0; j < parameters.getLength(); j++) {
					if(((Element)parameters.item(i)).getAttribute("id").equals(DataType.parameterVPDBaseline.toString())) {
						vpdbase = Double.parseDouble(((Element)parameters.item(i)).getTextContent());
					}
					else if(((Element)parameters.item(i)).getAttribute("id").equals(DataType.parameterVAEBaseline.toString())) {
						vaebase = Double.parseDouble(((Element)parameters.item(i)).getTextContent());
					}
					else if(((Element)parameters.item(i)).getAttribute("id").equals(DataType.parameterLastVaccinated.toString())) {
						lastvacc = Boolean.parseBoolean(((Element)parameters.item(i)).getTextContent());
					}
				}
				//agentIDs[i]  = new AgentDataTuple(agentID, vpdbase, vaebase, lastvacc);
				agentIDs[i] = agentID;
			}
			return agentIDs;
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	
	}
	//retrieve ids to kill
	public String[] retrieveAgentIDsKill() {
		try {
			Element data = validData(DataType.agentIDs.toString());
			if(data == null) {
				return null;
			}
			//retrive the bunch of id's associated with kill
			NodeList purpose = data.getElementsByTagName("purpose");
			Element purposeSingle = null;
			for(int i = 0; i < purpose.getLength(); i++) {
				if(((Element)purpose.item(i)).getAttribute("id").equals(DataType.purposeKill.toString())) {
					purposeSingle = ((Element)purpose.item(i));
				}
			}
			//if there is no bunch associated with kill, quit
			if(purposeSingle == null) {
				return null;
			}
			//dump ids into an array
			NodeList agents = purposeSingle.getElementsByTagName("agent");
			String[] agentIDs = new String[agents.getLength()];
			for(int i = 0; i < agents.getLength(); i++) {
				agentIDs[i] = ((Element)agents.item(i)).getAttribute("id");
			}
			return agentIDs;
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}
	//Retrieve an AgentDataContainer
	public AgentDataContainer retrieveADC() {
		try {
			Element data = validData(DataType.agentdatacontainer.toString());
			if(data == null) {
				return null;
			}
			AgentDataContainer adc = new AgentDataContainer();
			NodeList agentList = data.getElementsByTagName("agent");
			//for each agent
			for (int i = 0; i < agentList.getLength(); i++) {
				Element agent = (Element)agentList.item(i);
				String agentID = agent.getAttribute("id");
				Element actioncontainer = (Element)agent.getElementsByTagName("actioncontainer").item(0);
				NodeList actioncontentList = actioncontainer.getElementsByTagName("actioncontent");
				ActionContainer ac = new ActionContainer();
				//for each actioncontent
				for(int j = 0; j < actioncontentList.getLength(); j++) {
					Element actioncontent = (Element)actioncontentList.item(j);
					String actionID = actioncontent.getAttribute("id");
					
					State state = State.valueOf(((Element)actioncontent.getElementsByTagName("state").item(0)).getTextContent());

					NodeList parameterList = actioncontent.getElementsByTagName("parameter");
					Object[] parameterArray = new Object[parameterList.getLength()];
					//for each parameter
					for(int k =0; k < parameterList.getLength(); k++) {
						Element parameter = (Element)parameterList.item(k);
						parameterArray[k] = (Object)parameter.getTextContent();
					}
					ac.register(actionID, parameterArray);
					ac.get(actionID).setState(state);
				}

				Element perceptcontainer = (Element)agent.getElementsByTagName("perceptcontainer").item(0);
				NodeList perceptList = perceptcontainer.getElementsByTagName("percept");
				PerceptContainer pc = new PerceptContainer();
				//for each percept
				for(int ij = 0; ij < perceptList.getLength(); ij++) {
					String perceptID = ((Element)perceptList.item(ij)).getAttribute("id");
					Object parameters = ((Object)((Element)perceptList.item(ij)).getElementsByTagName("parameters").item(0).getTextContent());
					pc.put(perceptID, parameters);
				}
				ActionPerceptContainer apc = adc.getOrCreate(agentID);
				apc.setActionContainer(ac);
				apc.setPerceptContainer(pc);
			}
	 
			return adc;
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}
	//Determines if a data pack exists in the XML file and returns the element for it if it does
	private Element validData(String dataName) {
		if(root == null) {
			return null;
		}
		NodeList nl = root.getElementsByTagName("data");
		for(int ik = 0; ik < nl.getLength(); ik++) {
			if(((Element)nl.item(ik)).getAttribute("id").equals(dataName)){
				return ((Element)nl.item(ik));
			}
		}
		return null;
	}
	
	
	//method to convert Document to String
	private String getStringFromDocument(Document doc)
	{
	    try
	    {
	       DOMSource domSource = new DOMSource(doc);
	       StringWriter writer = new StringWriter();
	       StreamResult result = new StreamResult(writer);
	       TransformerFactory tf = TransformerFactory.newInstance();
	       Transformer transformer = tf.newTransformer();
	       //transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	       transformer.transform(domSource, result);
	       return writer.toString();
	    }
	    catch(TransformerException ex)
	    {
	       ex.printStackTrace();
	       return null;
	    }
	} 


}
