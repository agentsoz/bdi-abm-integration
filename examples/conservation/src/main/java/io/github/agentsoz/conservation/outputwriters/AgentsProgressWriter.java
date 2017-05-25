package io.github.agentsoz.conservation.outputwriters;

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

import io.github.agentsoz.conservation.Log;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;

/**
 * This class generates an output file named "agents.progress.csv". The open
 * method in the class should be called at the beginning of each repeat. Using
 * the "addAgentsInfo" method, BDI program can store the information in this
 * class. Then at the end of the repeat the "flush" method should be called to
 * write all the information stored in the class to the output file.
 * 
 * @author Sewwandi Perera
 */
public class AgentsProgressWriter {
	/**
	 * {@link FileWriter} instance
	 */
	private BufferedWriter agentsInfoWriter;

	/**
	 * All information about agents progress is stored in this TreeMap. Key
	 * field contains the agent ID. Value filed contains the List of categories
	 * (HCHP, HCLP, LCLP, LCHP) agent was in during all auction cycles.
	 */
	private TreeMap<Integer, List<String>> agentsInfo;

	/**
	 * Number of auction cycles.
	 */
	private int numberOfCycles = 0;

	/**
	 * Singleton instance
	 */
	private static AgentsProgressWriter instance = new AgentsProgressWriter();

	/**
	 * @return singleton instance
	 */
	public static AgentsProgressWriter getInstance() {
		return instance;
	}

	/**
	 * private constructor
	 */
	private AgentsProgressWriter() {
		agentsInfo = new TreeMap<Integer, List<String>>();
	}

	/**
	 * This method should be called at the beginning of each repeat. This method
	 * opens a new {@link FileWriter} for each repeat.
	 * 
	 * @param repeat
	 *            current repeat number
	 * @param numberOfCycle
	 *            number of cycles in the repeat
	 */
	public void open(int repeat, int numberOfCycles) {
		setNumberOfCycles(numberOfCycles);
		try {
			GZIPOutputStream zip = new GZIPOutputStream(new FileOutputStream(ConstantFileNames.getAgentsProgressFileName(repeat)));
			agentsInfoWriter = new BufferedWriter(new OutputStreamWriter(zip, "UTF-8"));			
		} catch (IOException e) {
			Log.error(e.getMessage());
		}
	}

	/**
	 * This method is called by "DecideBids" and "DecideParticipation" plans to
	 * store whether agent decided to participate in the auction, and if so what
	 * is his category.
	 * 
	 * @param agentName
	 *            name of the agent
	 * @param info
	 *            example info fields; A - participated in auction, in LCLP
	 *            category; A-np - not participated, in LCLP category; B -
	 *            participated in auction, in HCLP category; B-np - not
	 *            participated, in HCLP category;C - participated in auction, in
	 *            LCHP category; C-np - not participated, in LCHP category;D -
	 *            participated in auction, in HCHP category; D-np - not
	 *            participated, in HCHP category;
	 */
	public void addAgentsInfo(String agentName, String info) {
		List<String> storedInfo = agentsInfo.get(Integer.parseInt(agentName));

		if (storedInfo == null) {
			storedInfo = new ArrayList<String>();
		}

		storedInfo.add(info);

		agentsInfo.put(Integer.parseInt(agentName), storedInfo);
	}

	/**
	 * This method is called at the end of each repeat. So, all information
	 * about agents progress stored in the class is written to a csv file.
	 */
	public void flush() {
		try {
			this.appendHeader();

			Object[] agentNames = agentsInfo.keySet().toArray();

			for (Object name : agentNames) {
				agentsInfoWriter.append(String.valueOf((int) name));
				agentsInfoWriter.append(",");

				List<String> info = agentsInfo.get(name);
				for (String param : info) {
					agentsInfoWriter.append(param);
					agentsInfoWriter.append(",");
				}

				agentsInfoWriter.append("\n");
			}

			this.appendFooter();

			agentsInfoWriter.flush();
			close();
		} catch (IOException e) {
			Log.error(e.getMessage());
		}
	}

	/**
	 * Append the headr fields of the output csv
	 */
	private void appendHeader() {
		try {
			agentsInfoWriter.append("agentID\\cycle#,");
			for (int i = 1; i <= numberOfCycles; i++) {
				agentsInfoWriter.append(String.valueOf(i));
				agentsInfoWriter.append(",");
			}

			agentsInfoWriter.append("\n");
		} catch (IOException e) {
			Log.error(e.getMessage());
		}
	}

	/**
	 * Append a footer for output csv.
	 */
	private void appendFooter() {
		try {
			agentsInfoWriter.append("\n\n\n");
			agentsInfoWriter.append("DecideBidsWhenLowCLowP,A\n");
			agentsInfoWriter.append("DecideBidsWhenHighCLowP,B\n");
			agentsInfoWriter.append("DecideBidsWhenLowCHighP,C\n");
			agentsInfoWriter.append("DecideBidsWhenHighCHighP,D\n");
			agentsInfoWriter.append("Not Participated,np\n");
		} catch (IOException e) {
			Log.error(e.getMessage());
		}

	}

	/**
	 * close the file writer
	 * 
	 * @throws IOException
	 */
	private void close() throws IOException {
		agentsInfoWriter.close();
	}

	/**
	 * @return the number of packages used in the simulation.
	 */
	public int getNumberOfPackges() {
		return numberOfCycles;
	}

	/**
	 * Set number of cycles in the repeat
	 * 
	 * @param numberOfCycles
	 */
	public void setNumberOfCycles(int numberOfCycles) {
		this.numberOfCycles = numberOfCycles;
	}
}
