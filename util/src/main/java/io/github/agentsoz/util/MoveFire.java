package io.github.agentsoz.util;

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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.StringTokenizer;

/**
 * A simple tool to take an existing fire file and move it to a new location by
 * applying an offset used for testing the bushfire simulation
 */
public class MoveFire {

	private double latOffset = 0.0;
	private double longOffset = 0.0;
	private double timeRatio = 1.0;
	private String inFileName = "infire.txt";
	private String outFileName = "outfire.txt";
	private File inFile;
	private File outFile;

	public static void main(String[] args) {
		new MoveFire(args);
	}

	public MoveFire(String[] args) {
		if (!parseCommandLine(args))
			return;
		say("opening infile " + inFileName);
		BufferedReader reader = openInput();
		if (reader == null)
			return;
		say("opening outfile " + outFileName);
		BufferedWriter writer = openOutput();
		if (writer == null)
			return;

		say("processing data");
		String time = readTime(reader);
		while (time != null) {
			time = formatTime(time);
			writeOutput(writer, time);
			String input = readInput(reader);
			String output = formatOutput(input);
			writeOutput(writer, output);
			time = readTime(reader);
		}
		try {
			reader.close();
			writer.close();
		} catch (Exception e) {
		}
		say("done");
	}

	private BufferedReader openInput() {
		BufferedReader reader = null;
		try {
			inFile = new File(inFileName);

			say(inFile.getCanonicalPath());

			reader = new BufferedReader(new FileReader(inFile));
		} catch (Exception e) {
		}
		return reader;
	}

	private BufferedWriter openOutput() {
		BufferedWriter writer = null;
		try {
			outFile = new File(outFileName);

			say(outFile.getCanonicalPath());

			writer = new BufferedWriter(new FileWriter(outFile));
		} catch (Exception e) {
		}
		return writer;
	}

	private String readTime(BufferedReader reader) {
		String input = null;
		try {
			input = reader.readLine();
			// say(input);
		} catch (Exception e) {
		}
		return input;
	}

	private String readInput(BufferedReader reader) {
		String input = null;
		try {
			input = reader.readLine();
			// say(input);
		} catch (Exception e) {
		}
		return input;
	}

	private String formatTime(String time) {
		double t = Double.parseDouble(time);
		t = t * timeRatio;
		time = Double.toString(t);
		return time;
	}

	private String formatOutput(String input) {
		// say(input);
		String output = "";
		StringTokenizer st1 = new StringTokenizer(input);
		while (st1.hasMoreTokens()) {
			String coord = st1.nextToken();
			// say (coord);
			StringTokenizer st2 = new StringTokenizer(coord, ",");
			while (st2.hasMoreTokens()) {
				String xString = st2.nextToken();
				String yString = st2.nextToken();
				double lat = Double.parseDouble(xString);
				double longitude = Double.parseDouble(yString);
				lat = lat + latOffset;
				longitude = longitude + longOffset;
				output = output + Double.toString(lat) + ","
						+ Double.toString(longitude) + " ";
			}
		}
		// say (output);
		return output;
	}

	private void writeOutput(BufferedWriter writer, String output) {
		try {
			writer.write(output + "\n");
		} catch (Exception e) {
		}
	}

	private void say(String s) {
		System.out.println(s);
	}

	public boolean parseCommandLine(String[] args) {

		boolean result = true;

		String s = null;

		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "-in":
				if (i + 1 < args.length) {
					i++;
					inFileName = args[i];
				}
				break;
			case "-out":
				if (i + 2 < args.length) {
					i++;
					outFileName = args[i];
				}
				break;
			case "-lat":
				if (i + 1 < args.length) {
					i++;
					s = args[i];
					try {
						latOffset = Double.parseDouble(s);
					} catch (Exception e) {
					}
				}
				break;
			case "-long":
				if (i + 1 < args.length) {
					i++;
					s = args[i];
					try {
						longOffset = Double.parseDouble(s);
					} catch (Exception e) {
					}
				}
				break;
			case "-time":
				if (i + 1 < args.length) {
					i++;
					s = args[i];
					try {
						timeRatio = Double.parseDouble(s);
					} catch (Exception e) {
					}
				}
				break;
			}
		}

		if (!result) {
			System.out.println("Some required options were not given \n"
					+ usage);
		}

		return result;
	}

	private String usage = "usage:\njava -cp <path> MoveFire -in <inFile> -out <outFile> -lat <latOffset> -long <longOffset>  -time <ratio>\n"
			+ "where <path> points to the location of the util.jar file\n"
			+ "<inFile> is the fire file to read,\n"
			+ "<outFile> is the fire file to create,\n"
			+ "<latOffset> is the offset to apply to the latitude (y coord) of the existing fire,\n"
			+ "<longOffset> is the offset to apply to the longitude (y coord) of the existing fire\n"
			+ "<ratio> is a multiplier for the time to change the duration of the fire\n";

}
