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


import io.github.agentsoz.abmjack.shared.GlobalTime;
import io.github.agentsoz.vaccination.Log;
import io.github.agentsoz.vaccination.Global.MessageType;
import io.github.agentsoz.vaccination.controller.XMLHandler;

import java.util.*;
import java.io.*;
import java.net.*;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import io.github.agentsoz.bdiabm.ABMServerInterface;
import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.bdiabm.data.AgentStateList;

/**
 * 
 * @author Alex Lutman
 *
 
 */
public class ABMConnector extends Thread implements ABMServerInterface {
	
	private VaccinationBDIServerInterface bdiSystem;
	private AgentDataContainer nextContainer;

	private Socket abmClient;
	private Scanner input;
	private PrintWriter output;

	public ABMConnector() {
	}

	/**
	 * Start listening for connection on the given host:port
	 * @param host
	 * @param port
	 * @throws IOException
	 */
	public void start(String host, int port) throws IOException{
		System.out.println("Connection to ABM system");
		abmClient = new Socket(host, port);
		input = new Scanner(abmClient.getInputStream());
		output = new PrintWriter(abmClient.getOutputStream(), true);
		this.start();
	}
	
	public void takeControl (AgentDataContainer agentDataContainer) {
		System.out.println("---ABM taking control---");
		send(agentDataContainer);
	}

	public Object queryPercept (String agentID, String perceptID) {
		return null;
	}
	
	
	//listen for incoming connections and interpret the message
	@Override
	public void run() {
		// create the XML stream handler
		XMLHandler xmlH = null;
		try {
			xmlH = new XMLHandler();
		} catch (ParserConfigurationException e) {
			Log.error("ERROR creating XML handler: " + e.getMessage());
			return;
		}
		
		while (true) {
			String data = "";

			// wait for a message, or break if something went wrong
		
			try {
				data = input.nextLine();
			} catch (Exception e) {
				break;
			}
			
			// pass it to the XML parser
			try {
				xmlH.setData(data);
			} catch (SAXException | IOException e) {
				Log.error("ERROR parsing incoming stream:" + e.getMessage());
				Log.error("Could not parse: " + data);
			}

			// parse the simulation time
			long time = xmlH.getMessageTime();
			if (time >= 0) {
				GlobalTime.prevTime.setTime(GlobalTime.time.getTime());
				GlobalTime.newTime.setTime(time);
			}

			// handle the message 
			MessageType messageType = xmlH.getMessageType();
			Log.info("Received message: "+messageType.toString());
			AgentDataContainer adc1 = xmlH.retrieveADC();
			Log.info("AgentDataContainer is: " + adc1);
			AgentStateList asl = xmlH.retrieveASL();
			if (messageType == MessageType.START) {
				bdiSystem.init(adc1, asl, this, null);
			} else if (messageType == MessageType.TAKE_CONTROL) {
				bdiSystem.killAgents(xmlH.retrieveAgentIDsKill());
				bdiSystem.createAgents(xmlH.retrieveAgentIDsCreate());
			} else if (messageType == MessageType.TERMINATE_PROGRAM) {
				bdiSystem.finish();
				send(nextContainer);
				break;
			}
			bdiSystem.takeControl(adc1);
		}
	}

	public VaccinationBDIServerInterface getBdiSystem() {
		return bdiSystem;
	}
	
	public void setBdiSystem(VaccinationBDIServerInterface bdiSystem) {
		this.bdiSystem = bdiSystem;
	}
	
	public AgentDataContainer getNextContainer() {
		return nextContainer;
	}
	
	public void setNextContainer(AgentDataContainer nextContainer) {
		this.nextContainer = nextContainer;
	}

	private void send(AgentDataContainer agentDataContainer) {
		try {
			//convert
			String data = XMLHandler.toXML(agentDataContainer, GlobalTime.time.getTime());
			//send
			output.println(data);
		}
		catch (Exception e) {
			System.err.println("XML create error: "+e.toString());
			System.exit(1);
		}
	}
	
}
