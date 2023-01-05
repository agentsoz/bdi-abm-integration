package io.github.agentsoz.util;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2023 by its authors. See AUTHORS file.
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.StringTokenizer;

import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;


public class PopulationGenerator {

	// Defaults
	private static String eduFile = null;
	private static String workFile = null;
	private static String homeFile = null;
	private static String outFile = null;
	private static int eduAgentsCount = -1;
	private static int workAgentsCount = -1;
	private static int homeAgentsCount = -1;
	private static String personIdPrefix = "";

	// Keeps the next unique person ID
	private static int personId = 1;

	public static void main(String[] args) {

		// Parse the command line arguments and print the settings
		parse(args);
		printSettings();

		// Get the list of addresses from the shape-file
		ArrayList<Coordinates> addresses = getAddressesFromShapefile(homeFile);

		// Create the scenario with empty population
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());

		// Use this random generator for placement of agents
		Random rand = Global.getRandom() ;

		// Get the list of educational addresses if given
		ArrayList<Coordinates> edu = null;
		if (eduFile != null) {

			// Filter out educational addresses from the addresses list
			edu = new ArrayList<Coordinates>();

			// Now place the specified number of agents at educational locations
			for (int i = 0; i < eduAgentsCount; i++) {
				addPersonWithActivity("edu", edu.get(rand.nextInt(edu.size())),
						21600, scenario);
			}
		}

		// Get the list of business addresses if given
		ArrayList<Coordinates> work = null;
		if (workFile != null) {

			// Filter out work addresses from the addresses list
			work = new ArrayList<Coordinates>();

			// Now place the specified number of agents at work locations
			for (int i = 0; i < workAgentsCount; i++) {
				addPersonWithActivity("work",
						work.get(rand.nextInt(work.size())), 21600, scenario);
			}
		}

		// The remaining addresses will be treated as home addresses
		ArrayList<Coordinates> home = addresses;

		// Now place the specified number of agents at work locations
		for (int i = 0; i < homeAgentsCount; i++) {
			addPersonWithActivity("home", home.get(rand.nextInt(home.size())),
					21600, scenario);
		}

		// Finally, write this population to file
		MatsimWriter popWriter = new org.matsim.api.core.v01.population.PopulationWriter(
				scenario.getPopulation(), scenario.getNetwork());
		popWriter.write(outFile);
	}

	public static void addPersonWithActivity(String actType,
			Coordinates coordsOfAct, double actEndTime, Scenario scenario) {
		PopulationFactory populationFactory = scenario.getPopulation()
				.getFactory();
		Coord matSimCoord = new Coord(coordsOfAct.getLongitude(),
				coordsOfAct.getLatitude());

		// Create a new plan
		Plan plan = populationFactory.createPlan();

		// Create a new activity with the end time and add it to the plan
		Activity act = populationFactory.createActivityFromCoord(actType,
				matSimCoord);
		act.setEndTime(actEndTime);
		plan.addActivity(act);

		// Add a new leg to the plan. Needed, otherwise MATSim won't add this
		// leg-less plan to the simulation
		plan.addLeg(populationFactory.createLeg("car"));

		// Create a second activity (all plans must end in an activity)
		act = populationFactory.createActivityFromCoord(actType.toString(),
				matSimCoord);
		plan.addActivity(act);

		Person person = populationFactory.createPerson(Id
				.createPersonId(personIdPrefix + personId++));
		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);
	}

	public static ArrayList<Coordinates> getAddressesFromShapefile(
			String shapeFilePath) {
		ArrayList<Coordinates> listOfLocations = null;
		try {
			listOfLocations = new ArrayList<Coordinates>();
			// get the input shapefile
			DataStore inStore = new ShapefileDataStore(new File(shapeFilePath)
					.toURI().toURL());
			String name = inStore.getTypeNames()[0];
			// extract feature source from the shapefile
			FeatureSource<SimpleFeatureType, SimpleFeature> inSource = inStore
					.getFeatureSource(name);
			FeatureCollection<SimpleFeatureType, SimpleFeature> fc = inSource
					.getFeatures();
			FeatureIterator<SimpleFeature> i = fc.features();
			while (i.hasNext()) {
				SimpleFeature inFeature = i.next();
				// Extract the address
				Object inAttribute = inFeature.getAttribute(3);
				String address = (inAttribute instanceof String) ? (String) inAttribute
						: "address_unavailable";
				// Extract the coordinates
				inAttribute = inFeature.getAttribute(0);
				Coordinates coordinates = (inAttribute instanceof Geometry) ? new Coordinates(
						((Geometry) inAttribute).getCoordinate().x,
						((Geometry) inAttribute).getCoordinate().y) : null;
				coordinates.setAddress(address);
				listOfLocations.add(coordinates);
			}
		} catch (NumberFormatException e) {
			System.out.println("Error: Can not parse numbers in shape file");
		} catch (FileNotFoundException e) {
			System.out.println("Error: File not found!");
			System.out.println("File: " + shapeFilePath);
			System.out.println("Are you missing the file extension (.shp)?");
			System.exit(1);
		} catch (IOException e) {
			System.out.println("Error: Unsuccessful in reading shape file.");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return listOfLocations;
	}

	private static String usage() {
		return "usage: "
				+ PopulationGenerator.class.getName()
				+ "  [options] -a <home_addresses> -n <home_agents> -o <output_xml> -p <person_id_prefix>\n"
				+ "   -a <addresses>             shapefile (.shp) of addresses in the region\n"
				+ "   -n <agents>                number of agents to place in home addresses (default is homeAgentsCount)\n"
				+ "   -e <edu_addresses:agents>  file listing educational addresses, and number of agents to place here (optional)\n"
				+ "   -w <work_addresses:agents> file listing work addresses, and number of agents to place here (optional)\n"
				+ "   -o <output_xml>            file to write agent population to\n"
				+ "   -p <person_id_prefix>      prefix for IDs of persons\n";
	}

	private static void parse(String[] args) {
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "-a":
				if (i + 1 < args.length) {
					i++;
					homeFile = args[i];
				}
				break;
			case "-e":
				if (i + 1 < args.length) {
					i++;
					StringTokenizer tokenizer = new StringTokenizer(args[i],
							":");
					if (tokenizer.countTokens() != 2) {
						abort("Did not find colon-separated string:int pair");
					}
					eduFile = tokenizer.nextToken();
					String token = tokenizer.nextToken();
					try {
						eduAgentsCount = Integer.parseInt(token);
					} catch (Exception e) {
						abort("Option value '" + token + "' is not a number");
					}
				}
				break;
			case "-n":
				if (i + 1 < args.length) {
					i++;
					try {
						homeAgentsCount = Integer.parseInt(args[i]);
					} catch (Exception e) {
						abort("Option value '" + args[i] + "' is not a number");
					}
				}
				break;
			case "-w":
				if (i + 1 < args.length) {
					i++;
					StringTokenizer tokenizer = new StringTokenizer(args[i],
							":");
					if (tokenizer.countTokens() != 2) {
						abort("Did not find colon-separated string:int pair");
					}
					workFile = tokenizer.nextToken();
					String token = tokenizer.nextToken();
					try {
						workAgentsCount = Integer.parseInt(token);
					} catch (Exception e) {
						abort("Option value '" + token + "' is not a number");
					}
				}
				break;
			case "-o":
				if (i + 1 < args.length) {
					i++;
					outFile = args[i];
				}
				break;
			case "-p":
				if (i + 1 < args.length) {
					i++;
					personIdPrefix = args[i] + "_";
				}
				break;
			}
		}
		// Abort if required arguments were not given
		if (homeFile == null || homeAgentsCount == -1 || outFile == null) {
			abort("Some required options were not given");
		}
	}

	private static void printSettings() {
		System.out.println("Using the following settings:");
		System.out.println("Home agents: " + homeAgentsCount);
		System.out.println("Edu agents: " + eduAgentsCount);
		System.out.println("Work agents: " + workAgentsCount);
		System.out.println("");
		System.out.println("Files:");
		System.out.println("Shape file: " + homeFile);
		System.out.println("Edu locations: " + eduFile);
		System.out.println("Work locations: " + workFile);
	}

	private static void abort(String err) {
		System.err.println("\nERROR: " + err + "\n");
		System.out.println(usage());
		System.exit(0);
	}

}
