package io.github.agentsoz.bushfiretute;

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

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import io.github.agentsoz.bushfiretute.datamodels.Location;
import io.github.agentsoz.bushfiretute.datamodels.School;


/**
 * 
 * Utility class to hold configuration information
 *
 */
public class Config {

	private static final Logger logger = LoggerFactory.getLogger("");

	// all the config info is held statically so it can be accessed globally
	private static String configFile = null;

	private static int scenario=0;
	
	private static String reportFile = "bushfire-evacuation-report.txt";
	private static String bdiSimFile = null;
	private static String matSimFile = null;
	private static String geographyFile = null;
	private static int numBDIAgents = 1;
	private static String fireFile = null;
	private static String controllerFile = null;
	private static boolean useGUI = false;
	private static boolean bypassController = true;
	@SuppressWarnings("unused")
	private static boolean controllerUserInput = false;
	private static boolean dieOnDisconnect = false;
	private static int port = -1;
	
	//Scenario2 : kids & relatives Params
	private static double proportionWithKids = 0.0;
	private static double maxDistanceToSchool = 0.0 ;
	
	private static double proportionWithRelatives = 0.0;
	private static int maxDistanceToRelatives = 0 ;
	@SuppressWarnings("unused")
	private static int maxPickups = 0;
	
	private static int max_pickuptime = 0;
	
	private static int depTrigTime = 0;
	
	//Scenario3: diffusion Params
	private static int seed = 0 ;
	private static int diffTurn = 0 ;
	private static int avgLinks = 0 ;
	private static String networkType = " " ;
	private static double act_threshold = 0.0 ;
	private static double volatility=0.0;
	private static double maxRandomPanicRange = 0.0;
	private static double minRandomPanicRange = 0.0;

	private static double maxDistanceToNeighbours = 0.0;
	
	private static int max_neighbours = 0 ;
	private static int min_neighbours = 0 ;
	
	private static int max_friends= 0 ;
	private static int min_friends = 0 ;
	
	private static int max_familyMembers = 0 ;
	private static int min_familyMembers = 0 ;

	private static double high_panic = 0.0 ; 
	private static double med_panic = 0.0 ;
	private static double low_panic = 0.0 ;
	
	
	private static LinkedHashMap<Integer, School> schools = new LinkedHashMap<Integer, School>();
	private static ArrayList<String> agentsWithoutSchools =  new ArrayList<String>();
	
	private static Image image = new Image();
	private static String fireFireCoordinateSystem = "longlat";
	private static String coordinate_system = "longlat";
	private static int schoolCount = 0; //to set a inique int ID to each school
	
	public static class Image {
		public String file;
		public double west, east, north, south;

		public Image() {
		}
		
		@Override
		public String toString() {
			return "west[" + west + "] east[" + east + "] north[" + north + "] south[" + south + "]";
		}
	}
	
	public static Image getImage() {
		return image;
	}

	public int getAgentsWithoutSchool() { 
		return agentsWithoutSchools.size();
	}
	
	public static LinkedHashMap<Integer,School> getSchools() { 
		return schools;
	}
	
	public static int getMaxPickUps()
	{
		int maxPickups = (int) (proportionWithRelatives * 38343.0) + 10;
		return maxPickups;
	}
	
	public static int getPickUpTime()
	{
		//int pickuptime = rand.nextInt(max_pickuptime - min_pickuptime) + min_pickuptime ;
		return max_pickuptime ;
	}
	
	// basic get/set for simple data
	public static String getReportFile() {
		return reportFile;
	}

	public static String getMatSimFile() {
		return matSimFile;
	}

	public static String getFireFile() {
		return fireFile;
	}

	public static int getScenario() {
		return scenario;
	}
	
	public static int getPort() {
		return port;
	}

	public static int getNumBDIAgents() {
		return numBDIAgents;
	}
	
	public static int getSeed() {
		return seed;
	}
	
	public static int getDiffturn() {
		return diffTurn;
	}

	public static int getAvgLinks() {
		return avgLinks;
	}
	
	public static String getNetworkType() {
		return networkType;
	}
	
	public static double getActivationThreshold() {
		return act_threshold;
	}
	
	public static boolean getBypassController() {
		return bypassController;
	}

	public static boolean getDieOnDisconnect() {
		return dieOnDisconnect;
	}

	public static double getProportionWithKids() {
		return proportionWithKids;
	}

	public static double getmaxDistanceToSchool() {
		return maxDistanceToSchool;
	}
	public static double getHighPanicThreshold() {
		return high_panic;
	}
	
	public static double getMedPanicThreshold() {
		return med_panic;
	}
	
	public static double getLowPanicThreshold() {
		return low_panic;
	}
	
	public static double getVolatility() {
		return volatility;
	}

//	public static int getExtDiffTurn() {
//		return ext_diffTurn;
//	}
	
	public static double getMinRandomPanicRange() {
		return minRandomPanicRange;
	}
	public static double getMaxRandomPanicRange() {
		return maxRandomPanicRange;
	}

	
	public static double getProportionWithRelatives() {
		return proportionWithRelatives;
	}

	public static int getMaxDistanceToRelatives() {
		return maxDistanceToRelatives;
	}
	
	public static double getMaxDistanceToNeighbours() {
		return maxDistanceToNeighbours;
	}
	
	public static int getMaxNeighboursLimit() {
		return max_neighbours;
	}
	public static int getMinNeighboursLimit() {
		return min_neighbours;
	}
	
	public static int getMaxFriendsLimit() {
		return max_friends;
	}
	public static int getMinFriendsLimit() {
		return min_friends;
	}
	
	public static int getMaxFamiliesLimit() {
		return max_familyMembers;
	}
	public static int getMinFamiliesLimit() {
		return min_familyMembers;
	}
	
	public static String getCoordinate_system() {
		return coordinate_system;
	}

	public static String getFireFireCoordinateSystem() {
		return fireFireCoordinateSystem;
	}


	public static boolean getUseGUI() {
		return useGUI;
	}

	public static void setUseGUI(boolean b) {
		useGUI = b;
	}

	public static String getConfigFile() {
		return configFile;
	}

	public static void setConfigFile(String string) {
		configFile = string;
	}

	public static int getDepartureTriggerTime() {
		return depTrigTime;
	}
	/**
	 * read the config file (xml) and save data
	 * 
	 * @return false if there was a problem
	 */
	public static boolean readConfig() {
		if (configFile == null) {
			logger.error("No configuration file given");
			return false;
		}
		logger.info("Loading configuration from '" + configFile + "'");
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new FileInputStream(configFile));

			NodeList nl = doc.getDocumentElement().getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				Node node = nl.item(i);
				if (node instanceof Element) {
					String nodeName = node.getNodeName();
					logger.trace("found node " + nodeName);
					if (nodeName.equals("reportfile")) {
						reportFile = node.getAttributes().getNamedItem("name")
								.getNodeValue();
					}
					if (nodeName.equals("bdisimfile")) {
						bdiSimFile = node.getAttributes().getNamedItem("name")
								.getNodeValue();
					}
					if (nodeName.equals("matsimfile")) {
						matSimFile = node.getAttributes().getNamedItem("name")
								.getNodeValue();
					}
					if (nodeName.equals("firefile")) {
						fireFile = node.getAttributes().getNamedItem("name")
								.getNodeValue();
						fireFireCoordinateSystem = node.getAttributes()
								.getNamedItem("coordinates").getNodeValue();
					}
					if (nodeName.equals("controllerfile")) {
						controllerFile = node.getAttributes()
								.getNamedItem("name").getNodeValue();
					}
					if (nodeName.equals("geographyfile")) {
						geographyFile = node.getAttributes()
								.getNamedItem("name").getNodeValue();
						coordinate_system = node.getAttributes()
								.getNamedItem("coordinates").getNodeValue();
					}
					if (nodeName.equals("port") && useGUI) {
						String p = node.getAttributes().getNamedItem("number")
								.getNodeValue();
						port = Integer.parseInt(p);
						try {
							String d = node.getAttributes()
									.getNamedItem("die_on_disconnect")
									.getNodeValue();
							dieOnDisconnect = Boolean.parseBoolean(d);
						} catch (Exception e) {
						}
					}
					if (nodeName.equals("bdiagents")) {
						String n = node.getAttributes().getNamedItem("number")
								.getNodeValue();
						try {
							numBDIAgents = Integer.parseInt(n);
						} catch (Exception e) {
							System.err
									.println("WARNING: Could not read number of BDI agents from configuration file (will use default '"
											+ numBDIAgents
											+ "'): "
											+ e.getMessage());
						}
					}
					if (nodeName.equals("demographics")) {
						try {
							String k = node.getAttributes().getNamedItem("kids").getNodeValue();
							proportionWithKids = Double.parseDouble(k);
							String r = node.getAttributes().getNamedItem("relatives").getNodeValue();
							proportionWithRelatives = Double.parseDouble(r);
							String d = node.getAttributes().getNamedItem("max_distance_to_school").getNodeValue();
							maxDistanceToSchool = Double.parseDouble(d);
							
							String dist = node.getAttributes().getNamedItem("max_distance_to_relatives").getNodeValue();
							maxDistanceToRelatives = Integer.parseInt(dist); 	
							
							String max_pickup = node.getAttributes().getNamedItem("max_pickuptime_for_kids_and_rels").getNodeValue();
							max_pickuptime =Integer.parseInt(max_pickup);					
							
							String val = node.getAttributes().getNamedItem("departure_Trigger_Time").getNodeValue();
							depTrigTime = Integer.parseInt(val);
							
							String nei_dist = node.getAttributes().getNamedItem("max_distance_to_neighbours").getNodeValue();
							maxDistanceToNeighbours  = Double.parseDouble(nei_dist);
							
							String maxN = node.getAttributes().getNamedItem("max_neighbours").getNodeValue();
							max_neighbours = Integer.parseInt(maxN);
							
							String minN = node.getAttributes().getNamedItem("min_neighbours").getNodeValue();
							min_neighbours = Integer.parseInt(minN);
							
							String maxF = node.getAttributes().getNamedItem("max_friends").getNodeValue();
							max_friends = Integer.parseInt(maxF);
							
							String minF = node.getAttributes().getNamedItem("min_friends").getNodeValue();
							min_friends = Integer.parseInt(minF);

							String maxFam = node.getAttributes().getNamedItem("max_familyMembers").getNodeValue();
							max_familyMembers = Integer.parseInt(maxFam);
							
							String minFam = node.getAttributes().getNamedItem("min_familyMembers").getNodeValue();
							min_familyMembers = Integer.parseInt(minFam);
							
							
						}
						catch (Exception e) {
							System.err
									.println("WARNING: could not read from the node demographics "	+ e.getMessage());
						}

					}
					if (nodeName.equals("snmodel")) {
						try {
						String s = node.getAttributes().getNamedItem("diff_seed").getNodeValue();
						seed = Integer.parseInt(s);
						
						String t = node.getAttributes().getNamedItem("diff_turn").getNodeValue();
						diffTurn = Integer.parseInt(t);
						
						String links = node.getAttributes().getNamedItem("avg_links").getNodeValue();
						avgLinks = Integer.parseInt(links);
		
						String type = node.getAttributes().getNamedItem("networkType").getNodeValue();
						networkType  = type;
						
						String thr = node.getAttributes().getNamedItem("panic_act_threshold").getNodeValue();
						act_threshold  = Double.parseDouble(thr);
							
						String high = node.getAttributes().getNamedItem("high_panic_threshold").getNodeValue();
						high_panic  = Double.parseDouble(high);
						
						String med = node.getAttributes().getNamedItem("medium_panic_threshold").getNodeValue();
						med_panic  = Double.parseDouble(med);
						
						String low = node.getAttributes().getNamedItem("low_panic_threshold").getNodeValue();
						low_panic  = Double.parseDouble(low); 
								
//						String ext = node.getAttributes().getNamedItem("ext_diffTurn").getNodeValue();
//						ext_diffTurn = Integer.parseInt(ext);
						
						String maxP = node.getAttributes().getNamedItem("maxRandomPanicRange").getNodeValue();
						maxRandomPanicRange  = Double.parseDouble(maxP);
						
						String minP = node.getAttributes().getNamedItem("minRandomPanicRange").getNodeValue();
						minRandomPanicRange  = Double.parseDouble(minP);
						
						String vol = node.getAttributes().getNamedItem("volatility").getNodeValue();
						volatility = Double.parseDouble(vol);
						
						}
						catch (Exception e) {
							System.err
									.println("WARNING: could not read from the node snmodel "	+ e.getMessage());
						}
						
					}
					if (nodeName.equals("scenario")) {
						String no = node.getAttributes().getNamedItem("scenarioType").getNodeValue();
						scenario = Integer.parseInt(no);

												
					}
					if (nodeName.equals("image")) {
						try {
							String w = node.getAttributes()
									.getNamedItem("west").getNodeValue();
							String e = node.getAttributes()
									.getNamedItem("east").getNodeValue();
							String n = node.getAttributes()
									.getNamedItem("north").getNodeValue();
							String s = node.getAttributes()
									.getNamedItem("south").getNodeValue();
							image.file = node.getAttributes()
									.getNamedItem("file").getNodeValue();
							image.west = Double.parseDouble(w);
							image.east = Double.parseDouble(e);
							image.north = Double.parseDouble(n);
							image.south = Double.parseDouble(s);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		} catch (Exception e) {
			System.err.println("ERROR while reading config: " + e.getMessage());
		}
		if (controllerFile != null) {
			try {
				DocumentBuilderFactory dbf = DocumentBuilderFactory
						.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document doc = db.parse(new FileInputStream(controllerFile));

				NodeList nl = doc.getDocumentElement().getChildNodes();
				for (int i = 0; i < nl.getLength(); i++) {
					Node node = nl.item(i);

					if (node instanceof Element) {

						String nodeName = node.getNodeName();
						logger.trace("found node " + nodeName);

						if (nodeName.equals("bypassing")) {

							String s = node.getAttributes()
									.getNamedItem("bypass").getNodeValue();
							bypassController = Boolean.parseBoolean(s);
							s = node.getAttributes()
									.getNamedItem("userControl").getNodeValue();
							controllerUserInput = Boolean.parseBoolean(s);
						}
					}
				}
			} catch (Exception e) {
				logger.error("unable to read controller file '"
						+ controllerFile + "' :" + e.getMessage());
				return false;
			}
		} else {
			bypassController = true;
		}

		logger.debug("matSimFilefile = " + matSimFile);
		logger.debug("bdiSimFile = " + bdiSimFile);
		logger.debug("geographyFile = " + geographyFile);
//		logger.debug("fireFile = " + fireFile);

		// TODO should check existence of all files and stop gracefully
		// TODO should also allow some files to not exist and setup
		// functionality accordingly
		// eg if no controller file, do not initiate controller

		if (geographyFile != null) {
			if (readGeography()) {
				printSchools();

			} else {
				return false;
			}
		}

		if (matSimFile == null) {
			return false;
		}
		return true;
	}

	/**
	 * read the config file (xml) and save data
	 * 
	 * @return false if there was a problem
	 */
	private static boolean readGeography() {
		boolean result = true;
		logger.info("loading geography file " + geographyFile);
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new FileInputStream(geographyFile));

			NodeList nl = doc.getDocumentElement().getChildNodes();
			List<Node> locationNodes = new ArrayList<Node>();
			for (int i = 0; i < nl.getLength(); i++) {
				Node node = nl.item(i);
				if (node instanceof Element) {
					String nodeName = node.getNodeName();
					logger.trace("found node " + nodeName);
					if (nodeName.equals("location")) {
						locationNodes.add(node); // adding all location nodes
					}
				}
			}
			for (Node n : locationNodes) 
				if (!readSchool(n)) {
					result = false;
				}

		} catch (Exception e) {
			logger.error("Unable to read geography file " + geographyFile);
			return false;
		}
		if (schools.size() == 0) {
			logger.error("No schools configured in " + geographyFile);
			return false;
		}
//		if (reliefCentres.size() == 0) {
//			logger.error("No relief centres configured in " + geographyFile);
//			return false;
//		}

		return result;

	}

	/**
	 * read the list of schools and store them.
	 * 
	 * @param parent
	 *            The node containing the list
	 * @return false if there was a problem
	 */
	private static boolean readSchool(Node node) {
		boolean result = true;
		try {
			String name = node.getAttributes().getNamedItem("name")
					.getNodeValue();
			String type = node.getAttributes().getNamedItem("type")
					.getNodeValue();
			String eastStr = node.getAttributes().getNamedItem("easting")
					.getNodeValue();
			String northStr = node.getAttributes().getNamedItem("northing")
					.getNodeValue();
			double easting = Double.parseDouble(eastStr);
			double northing = Double.parseDouble(northStr);
			School scl = new School(schoolCount,name, type, easting, northing);
			schools.put(schoolCount, scl);
			schoolCount++;
		} catch (Exception e) {
			logger.error("Could not read location from config file: "
					+ e.getMessage());
			result = false;
		}
		return result;
	}

	public static void  readSchoolFromList(LinkedHashMap<Double,Double> schoolLocs) {
		for(Map.Entry<Double, Double> entry : schoolLocs.entrySet()) {
			double east = (double) entry.getKey();
			double north =  (double) entry.getValue();
			
			School scl = new School(schoolCount,"school","s", east, north);			
			schools.put(schoolCount, scl);
			schoolCount++;			
		}
		printSchools();
	}
	private static void printSchools(){
		for (Integer key : schools.keySet()) {
			School scl = schools.get(key);
			logger.trace("school info : {} ", scl.toString());

		}
//		testGetRandomSchool();
	}
	/**
	 * measures the distance in between two locations in km and randomly
	 * selects coords of a school.
	 * @param agentLoc : home location of the agent .These should be UTM (meters)
	 * @param distRange : distance range in km
	 * @return
	 */
	public static School getRandomSchool(String id,double[] agentLoc) {
		 double distRange = maxDistanceToSchool;
		 School selectedSchool=null;
		 ArrayList<School> schoolsWithinDistance = new ArrayList<School> ();
		 for(School school : schools.values()) {
			 //distance is returned in km
			 if(Location.distance(agentLoc,school.getCoordinates()) <= distRange)
			 {
				 schoolsWithinDistance.add(school);
			 }
		 }
		 if(!schoolsWithinDistance.isEmpty()) {
			 logger.debug("number of schools within the range : {}", schoolsWithinDistance.size());
			 logger.debug(" selected school array : {}",schoolsWithinDistance.toString());
			 int num = BushfireMain.getRandom().nextInt(schoolsWithinDistance.size());
			 selectedSchool= schoolsWithinDistance.get(num);
			 logger.debug("selected school ID : " + selectedSchool.getID());
			 
			 
			School scl = schools.get(selectedSchool.getID());
			scl.addKid(id);
			
			logger.trace("added kid {} to school {} ",id, scl.getID());
			 
		 }
		 else {
			 agentsWithoutSchools.add(id);
		 }
		 return selectedSchool;
	}
	
	public static double[] getRandomSchoolCoords(String id,double[] agentLoc) {
		double[] coords =  null; 
		School randomSchool = getRandomSchool(id,agentLoc);
		if(randomSchool != null) {
			coords = randomSchool.getCoordinates();
		}	
		return coords;
	}
	
	public static int getRandomSchoolID(String id, double[] agentLoc) {
		int schoolID=-1; 
		School randomSchool = getRandomSchool(id,agentLoc);
		if(randomSchool != null) {
			schoolID = randomSchool.getID();

		}
				
		return schoolID;
	}
	
	public static void testGetRandomSchool()
	{
		//testing getRandomSchool method
		double[] test = {1500.0,1000.0};
		School school = getRandomSchool("1",test); //
		logger.debug("selected school coords : easting - {} northing -{}", school.getEasting(), school.getNorthing());
	}
	
}
